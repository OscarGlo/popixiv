package dev.oscarglo.popixiv.activities.views

import android.content.Intent
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import dev.oscarglo.popixiv.activities.components.IllustCard
import dev.oscarglo.popixiv.activities.components.Select
import dev.oscarglo.popixiv.activities.viewModels.BookmarkMeta
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.activities.viewModels.FollowMeta
import dev.oscarglo.popixiv.activities.viewModels.IllustFetcher
import dev.oscarglo.popixiv.activities.viewModels.RestrictMeta
import dev.oscarglo.popixiv.api.Illust
import dev.oscarglo.popixiv.util.Prefs
import dev.oscarglo.popixiv.util.globalViewModel
import retrofit2.HttpException
import java.util.Date
import java.util.Locale
import kotlin.math.max

val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)

fun dateToString(date: Date): String {
    val day = date.date.toString().padStart(2, '0')
    val month = (1 + date.month).toString().padStart(2, '0')
    val year = 1900 + date.year
    return "$day/$month/$year"
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun IllustGrid(
    fetcherKey: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    showDates: Boolean = false,
    hasBackButton: Boolean = false
) {
    val context = LocalContext.current

    val fetcherViewModel = globalViewModel<FetcherViewModel>()

    val fetcher = fetcherViewModel.get(fetcherKey)

    val refreshing by rememberSaveable { mutableStateOf(false) }
    var currentDateIndex by rememberSaveable { mutableIntStateOf(0) }

    val illustGroups = fetcher.illusts.mapIndexed { i, illust -> i to illust }
        .groupBy { dateToString(dateFormat.parse(it.second.create_date)) }

    val showIndicator = refreshing || (!fetcher.done && fetcher.illusts.isEmpty())

    val pullRefreshState = rememberPullRefreshState(
        showIndicator,
        onRefresh = { fetcherViewModel.reset(fetcherKey) }
    )

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
                    currentDateIndex =
                        if (it.positionInParent().y < 0) index else max(index - 1, 0)
                }
        )
    }

    @Composable
    fun GridIllust(indexedIllust: Pair<Int, Illust>) {
        IllustCard(
            indexedIllust.second,
            onClick = {
                fetcherViewModel.updateLast(fetcherKey) {
                    fetcher.copy(current = indexedIllust.first)
                }
                navController.navigate("gallery/$fetcherKey")
            },
            largePreview = stagger,
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

    Scaffold(
        topBar = {
            TopAppBar {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasBackButton)
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = "back"
                                )
                            }

                        if (showDates)
                            Text(
                                illustGroups.keys.toList()
                                    .getOrElse(currentDateIndex) { "Loading..." },
                                modifier = Modifier.padding(8.dp)
                            )
                    }

                    if (fetcher.meta is RestrictMeta)
                        RestrictFilter(fetcherKey)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
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

            PullRefreshIndicator(
                showIndicator,
                pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun RestrictFilter(fetcherKey: String) {
    val fetcherViewModel = globalViewModel<FetcherViewModel>()
    val fetcher = fetcherViewModel.get(fetcherKey) as IllustFetcher<RestrictMeta>

    val values =
        if (fetcher.meta.allAllowed) listOf("all", "public", "private")
        else listOf("public", "private")

    Select(
        values,
        fetcher.meta.restrict,
        onChange = {
            fetcherViewModel.updateLast(fetcherKey) {
                when (fetcher.meta) {
                    is FollowMeta -> fetcher.reset().copy(FollowMeta(it))
                    is BookmarkMeta -> fetcher.reset().copy(BookmarkMeta(it, fetcher.meta.userId))
                }
            }
        },
        modifier = Modifier.width(100.dp),
        render = { Text(it.capitalize()) }
    )
}