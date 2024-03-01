package dev.oscarglo.popixiv.activities.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
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
import dev.oscarglo.popixiv.activities.components.IllustGrid
import dev.oscarglo.popixiv.activities.components.TagChip
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
                autocomplete.value = PixivApi.instance.getSearchAutocomplete(it).tags
            }
        }
    }
}

val tagSaver = Saver<List<Tag>, String>(
    { tags -> tags.map { it.name + "–" + it.translated_name }.joinToString("—") },
    { str -> str.split("—").map { part -> part.split("–").let { Tag(it[0], it.getOrNull(1)) } } }
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun SearchPage(navController: NavController, query: String? = null) {
    val fetcherViewModel = globalViewModel<FetcherViewModel>()
    val searchViewModel = viewModel<SearchViewModel>()

    val word by searchViewModel.word.collectAsState()
    var tags by rememberSaveable(stateSaver = tagSaver) { mutableStateOf(emptyList()) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val autocomplete by searchViewModel.autocomplete.collectAsState()
    var focused by remember { mutableStateOf(false) }
    val showDropdown = focused && autocomplete.isNotEmpty()

    LaunchedEffect(query) {
        if (query != null)
            tags = query.split("—").map {
                val values = it.split("–")
                Tag(values[0], values.getOrNull(1))
            }
    }

    AppTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize()) {
                ExposedDropdownMenuBox(
                    expanded = showDropdown,
                    onExpandedChange = {}
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.15f))
                            .padding(12.dp, 4.dp)
                    ) {
                        tags.mapIndexed { i, tag ->
                            TagChip(
                                tag,
                                modifier = Modifier
                                    .clickable { tags = tags.filterIndexed { j, _ -> i != j } }
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
                                else if (tags.isNotEmpty()) ImeAction.Search
                                else ImeAction.None
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    tags += Tag(word.trim())
                                    searchViewModel.word.value = ""
                                },
                                onSearch = {
                                    fetcherViewModel.updateLast("search") {
                                        (this as IllustFetcher<SearchMeta>)
                                            .reset()
                                            .copy(
                                                meta = SearchMeta(
                                                    tags.joinToString(" ") { it.name },
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
                                tags = emptyList()
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
                                tags += it
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

                IllustGrid(
                    fetcherKey = "search",
                    navController = navController,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ImageSearch,
                            contentDescription = "no result",
                            modifier = Modifier.size(64.dp)
                        )
                        Text("No results", fontSize = 20.sp)
                    }
                }
            }
        }
    }
}