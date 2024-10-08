package dev.oscarglo.popixiv.activities.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import dev.oscarglo.popixiv.activities.components.Placeholder
import dev.oscarglo.popixiv.activities.components.TagChip
import dev.oscarglo.popixiv.activities.components.dialog.SearchFilters
import dev.oscarglo.popixiv.activities.components.dialog.SearchFiltersDialog
import dev.oscarglo.popixiv.activities.components.dialog.searchFiltersSaver
import dev.oscarglo.popixiv.activities.components.illust.IllustGrid
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.activities.viewModels.IllustFetcher
import dev.oscarglo.popixiv.activities.viewModels.SearchMeta
import dev.oscarglo.popixiv.api.PixivApi
import dev.oscarglo.popixiv.api.Tag
import dev.oscarglo.popixiv.ui.theme.AppTheme
import dev.oscarglo.popixiv.util.globalViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel : ViewModel() {
    val word = MutableStateFlow("")
    val autocomplete = MutableStateFlow(listOf<Tag>())

    init {
        viewModelScope.launch {
            word.debounce(500).collectLatest {
                autocomplete.value = PixivApi.instance.getSearchAutocomplete(it.trimStart('-')).tags
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun SearchPage(navController: NavController, query: String = "", hasBackButton: Boolean = false) {
    val fetcherViewModel = globalViewModel<FetcherViewModel>()
    val searchViewModel = viewModel<SearchViewModel>()

    val word by searchViewModel.word.collectAsState()
    var filters by rememberSaveable(stateSaver = searchFiltersSaver) { mutableStateOf(SearchFilters()) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val autocomplete by searchViewModel.autocomplete.collectAsState()
    var focused by remember { mutableStateOf(false) }
    val showDropdown = focused && autocomplete.isNotEmpty()

    LaunchedEffect(query) {
        if (query.isBlank() || filters.tags.isNotEmpty())
            return@LaunchedEffect

        filters = SearchFilters(tags = query.split(" ").map { Tag(it) })
    }

    fun handleBack() {
        fetcherViewModel.pop()
        navController.navigateUp()
    }

    var showFilterDialog by remember { mutableStateOf(false) }

    if (showFilterDialog)
        SearchFiltersDialog(filters) {
            if (it != null) {
                fetcherViewModel.updateLast("search") {
                    var fetcher = this as IllustFetcher<SearchMeta>
                    if (it.tags != filters.tags || it.sort != filters.sort || it.duration != filters.duration)
                        fetcher = fetcher.reset()
                    fetcher.copy(meta = SearchMeta(it))
                }

                filters = it
            }
            showFilterDialog = false
        }

    AppTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    if (hasBackButton)
                        IconButton(onClick = ::handleBack) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "back")
                        }

                    ExposedDropdownMenuBox(
                        expanded = showDropdown,
                        onExpandedChange = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = if (hasBackButton) 0.dp else 8.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                )
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.15f))
                                .padding(12.dp, 4.dp)
                        ) {
                            filters.tags.mapIndexed { i, tag ->
                                TagChip(
                                    tag,
                                    modifier = Modifier
                                        .clickable {
                                            filters =
                                                filters.copy(tags = filters.tags.filterIndexed { j, _ -> i != j })
                                        }
                                        .align(Alignment.CenterVertically),
                                )
                            }

                            BasicTextField(
                                word,
                                { searchViewModel.word.value = it },
                                textStyle = TextStyle(color = MaterialTheme.colors.onBackground),
                                cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    imeAction = if (word.isNotBlank()) ImeAction.Done
                                    else if (filters.tags.isNotEmpty()) ImeAction.Search
                                    else ImeAction.None
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        filters = filters.copy(
                                            tags = filters.tags + Tag(
                                                word.trimStart('-').trim(),
                                                negative = word.startsWith("-")
                                            )
                                        )
                                        searchViewModel.word.value = ""
                                    },
                                    onSearch = {
                                        fetcherViewModel.updateLast("search") {
                                            (this as IllustFetcher<SearchMeta>)
                                                .reset()
                                                .copy(
                                                    meta = SearchMeta(
                                                        filters = filters,
                                                        offset = 0
                                                    )
                                                )
                                        }
                                        focusManager.clearFocus()
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterVertically)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focused = it.isFocused },
                            )

                            IconButton(
                                onClick = {
                                    filters = filters.copy(tags = emptyList())
                                    searchViewModel.word.value = ""
                                    searchViewModel.autocomplete.value = emptyList()
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(4.dp, 8.dp)
                                    .size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "clear")
                            }
                        }

                        ExposedDropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { focusManager.clearFocus() },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .exposedDropdownSize()
                                .focusable(false)
                        ) {
                            autocomplete.map {
                                DropdownMenuItem(onClick = {
                                    filters = filters.copy(
                                        tags = filters.tags + it.copy(negative = word.startsWith("-"))
                                    )
                                    searchViewModel.word.value = ""
                                    searchViewModel.autocomplete.value = emptyList()
                                }) {
                                    Text(it.name, fontWeight = FontWeight.SemiBold)

                                    if (it.translated_name != null)
                                        Text(
                                            it.translated_name,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                        )
                                }
                            }
                        }
                    }

                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    }
                }

                IllustGrid(
                    fetcherKey = "search",
                    navController = navController,
                    filters = filters,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Placeholder(icon = Icons.Default.ImageSearch, label = "No results")
                }
            }
        }
    }

    if (hasBackButton)
        BackHandler(onBack = ::handleBack)
}