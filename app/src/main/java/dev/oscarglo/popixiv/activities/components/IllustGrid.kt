package dev.oscarglo.popixiv.activities.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.oscarglo.popixiv.activities.LoginActivity
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.api.Illust
import dev.oscarglo.popixiv.util.Prefs
import dev.oscarglo.popixiv.util.displayShortDateFormat
import dev.oscarglo.popixiv.util.globalViewModel
import dev.oscarglo.popixiv.util.pixivDateFormat
import retrofit2.HttpException
import kotlin.math.max

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun IllustGrid(
    fetcherKey: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    showDates: Boolean = false,
    filters: SearchFilters = SearchFilters(),
    onDateChange: (date: String) -> Unit = {},
    placeholder: @Composable BoxScope.() -> Unit = {
        Placeholder(Icons.Default.QuestionMark, "No illusts")
    }
) {
    val context = LocalContext.current

    val fetcherViewModel = globalViewModel<FetcherViewModel>()
    val fetcher = fetcherViewModel.get(fetcherKey)

    val illustGroups = fetcher.illusts
        .mapIndexed { i, illust -> i to illust }
        .filter { it.second.total_bookmarks >= (filters.minBookmarks ?: 0) }
        .groupBy { displayShortDateFormat.format(pixivDateFormat.parse(it.second.create_date)) }
    var firstLoad by rememberSaveable { mutableStateOf(true) }

    val refreshing by rememberSaveable { mutableStateOf(false) }
    val showIndicator = refreshing || (!fetcher.done && fetcher.illusts.isEmpty())
    val pullRefreshState = rememberPullRefreshState(
        showIndicator,
        onRefresh = { fetcherViewModel.reset(fetcherKey) }
    )

    LaunchedEffect(illustGroups) {
        if (firstLoad && illustGroups.isNotEmpty()) {
            onDateChange(illustGroups.keys.toList().first())
            firstLoad = false
        }
    }

    val stagger by Prefs.APPEARANCE_GRID_STAGGER.booleanState()
    val gap by Prefs.APPEARANCE_GRID_GAP.intState()
    val cardSize by Prefs.APPEARANCE_CARD_SIZE.intState()

    @Composable
    fun DateHeader(date: String, index: Int) {
        Text(
            date,
            modifier = Modifier
                .padding(8.dp)
                .onGloballyPositioned {
                    val dateIndex = if (it.positionInParent().y < 0) index else max(index - 1, 0)
                    onDateChange(illustGroups.keys.toList()[dateIndex])
                }
        )
    }

    @Composable
    fun GridIllust(indexedIllust: Pair<Int, Illust>) {
        IllustCard(
            indexedIllust.second,
            Modifier,
            largePreview = stagger,
            onClick = {
                fetcherViewModel.updateLast(fetcherKey) {
                    fetcher.copy(current = indexedIllust.first)
                }
                navController.navigate("gallery/$fetcherKey")
            },
            gap = (gap / 2).dp
        )
    }

    @Composable
    fun Loader() {
        if (!showIndicator)
            Box(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

        LaunchedEffect("getIllusts") {
            try {
                fetcherViewModel.fetch(fetcherKey)
            } catch (e: HttpException) {
                e.printStackTrace()
                context.startActivity(Intent(context, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                return@LaunchedEffect
            }
        }
    }

    Box(modifier = modifier.pullRefresh(pullRefreshState)) {
        if (stagger)
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(cardSize.dp),
                verticalItemSpacing = gap.dp,
                horizontalArrangement = Arrangement.spacedBy(gap.dp)
            ) {
                illustGroups.entries.mapIndexed { i, (date, group) ->
                    if (showDates && i > 0)
                        this.item(span = StaggeredGridItemSpan.FullLine) { DateHeader(date, i) }

                    this.items(group.size) { GridIllust(indexedIllust = group[it]) }
                }

                if (!fetcher.done)
                    item(span = StaggeredGridItemSpan.FullLine) { Loader() }
            }
        else
            LazyVerticalGrid(
                columns = GridCells.Adaptive(cardSize.dp),
                verticalArrangement = Arrangement.spacedBy(gap.dp),
                horizontalArrangement = Arrangement.spacedBy(gap.dp)
            ) {
                illustGroups.entries.mapIndexed { i, (date, group) ->
                    if (showDates && i > 0)
                        this.item(span = { GridItemSpan(maxLineSpan) }) { DateHeader(date, i) }

                    this.items(group.size) { GridIllust(indexedIllust = group[it]) }
                }

                if (!fetcher.done)
                    item(span = { GridItemSpan(maxLineSpan) }) { Loader() }
            }

        if (fetcher.done && fetcher.illusts.isEmpty())
            Box(modifier = Modifier.align(Alignment.Center)) {
                placeholder()
            }

        PullRefreshIndicator(
            showIndicator,
            pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}