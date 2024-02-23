package dev.oscarglo.popixiv.activities.views

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.oscarglo.popixiv.activities.components.SaveToast
import dev.oscarglo.popixiv.activities.components.SaveViewModel
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.api.Illust
import dev.oscarglo.popixiv.api.IllustPage
import dev.oscarglo.popixiv.api.PixivApi
import dev.oscarglo.popixiv.ui.theme.AppTheme
import dev.oscarglo.popixiv.util.getImagesDir
import dev.oscarglo.popixiv.util.globalViewModel
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import java.io.File

val client = OkHttpClient.Builder().build()

fun downloadPage(saveViewModel: SaveViewModel, page: IllustPage) {
    Thread {
        saveViewModel.saving.value += page.filename

        val req = Request
            .Builder()
            .url(page.image_urls.original!!)
            .headers(PixivApi.getHeaders())
            .build()
        val res = client
            .newCall(req)
            .execute()
        val bytes = res.body?.bytes()
        if (bytes != null)
            File(getImagesDir(), page.filename).writeBytes(bytes)

        saveViewModel.saving.value -= page.filename
    }.start()
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun Gallery(fetcherKey: String, navController: NavController) {
    val fetcherViewModel = globalViewModel<FetcherViewModel>()
    val saveViewModel = globalViewModel<SaveViewModel>()

    val fetcher = fetcherViewModel.get(fetcherKey)

    val pagerState = rememberPagerState(fetcher.current, 0f) { fetcher.illusts.size }

    val allDownloaded = fetcher.illusts[pagerState.currentPage].pages.all {
        File(getImagesDir(), it.filename).exists()
    }

    fun handleBack() {
        navController.navigateUp()
        //fetcherViewModel.pop()
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = ::handleBack) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "back"
                            )
                        }

                        IconButton(onClick = {
                            fetcher.illusts[pagerState.currentPage].pages.forEach {
                                downloadPage(saveViewModel, it)
                            }
                        }) {
                            Icon(
                                if (allDownloaded) Icons.Default.DownloadDone
                                else Icons.Default.Download,
                                contentDescription = "download all"
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                HorizontalPager(pagerState) { i ->
                    if (i == pagerState.pageCount - 1)
                        LaunchedEffect("fetchMore") {
                            fetcherViewModel.fetch(fetcherKey)
                        }

                    IllustView(fetcher.illusts[i])
                }

                SaveToast(modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        BackHandler(onBack = ::handleBack)
    }
}

@Composable
fun IllustView(illust: Illust) {
    val saveViewModel = globalViewModel<SaveViewModel>()

    val scrollState = rememberScrollState()
    val savingPages by saveViewModel.saving.collectAsState()

    var bookmarked by rememberSaveable { mutableStateOf(illust.is_bookmarked) }
    var loadingBookmark by rememberSaveable { mutableStateOf(false) }

    Surface {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                illust.pages.map { page ->
                    Box {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                        AsyncImage(
                            ImageRequest.Builder(LocalContext.current)
                                .data(page.image_urls.large)
                                .headers(PixivApi.getHeaders())
                                .build(),
                            contentDescription = illust.caption,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6F))
                                .size(48.dp)
                                .clickable { downloadPage(saveViewModel, page) },
                        ) {
                            if (savingPages.contains(page.filename))
                                CircularProgressIndicator(
                                    strokeWidth = 3.dp,
                                    color = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(
                                            Alignment.Center
                                        )
                                )
                            else
                                Icon(
                                    if (File(getImagesDir(), page.filename).exists())
                                        Icons.Default.DownloadDone
                                    else
                                        Icons.Default.Download,
                                    contentDescription = "download",
                                    modifier = Modifier.align(
                                        Alignment.Center
                                    )
                                )
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    if (!loadingBookmark)
                        Thread {
                            loadingBookmark = true
                            try {
                                runBlocking {
                                    if (bookmarked) PixivApi.instance.deleteBookmark(illust.id)
                                    else PixivApi.instance.addBookmark(illust.id)
                                    bookmarked = !bookmarked
                                }
                            } catch (e: HttpException) {
                                e.printStackTrace()
                            }
                            loadingBookmark = false
                        }.start()
                },
                backgroundColor = MaterialTheme.colors.background,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)
            ) {
                if (loadingBookmark)
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                else
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(24.dp),
                        tint = if (bookmarked) Color(0xffff4060) else MaterialTheme.colors.onBackground
                    )
            }
        }
    }
}