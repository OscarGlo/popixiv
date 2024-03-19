package dev.oscarglo.popixiv.activities.views

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled._18UpRating
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.oscarglo.popixiv.activities.components.HtmlText
import dev.oscarglo.popixiv.activities.components.Placeholder
import dev.oscarglo.popixiv.activities.components.SaveToast
import dev.oscarglo.popixiv.activities.components.SaveViewModel
import dev.oscarglo.popixiv.activities.components.TagChip
import dev.oscarglo.popixiv.activities.components.dialog.BookmarkDialog
import dev.oscarglo.popixiv.activities.viewModels.BookmarkMeta
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.activities.viewModels.IllustFetcher
import dev.oscarglo.popixiv.activities.viewModels.SearchMeta
import dev.oscarglo.popixiv.activities.viewModels.UserMeta
import dev.oscarglo.popixiv.api.Illust
import dev.oscarglo.popixiv.api.IllustPage
import dev.oscarglo.popixiv.api.PixivApi
import dev.oscarglo.popixiv.ui.theme.AppTheme
import dev.oscarglo.popixiv.util.Prefs
import dev.oscarglo.popixiv.util.displayDateTimeFormat
import dev.oscarglo.popixiv.util.getImagesDir
import dev.oscarglo.popixiv.util.getImagesPath
import dev.oscarglo.popixiv.util.globalViewModel
import dev.oscarglo.popixiv.util.pixivDateFormat
import dev.oscarglo.popixiv.util.pixivImage
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import java.io.File

val client = OkHttpClient.Builder().build()

fun downloadPage(context: Context, saveViewModel: SaveViewModel, page: IllustPage) {
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

        if (bytes != null) {
            var values = ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, getImagesPath())
                put(MediaStore.MediaColumns.DISPLAY_NAME, page.filename)
                put(MediaStore.MediaColumns.IS_PENDING, true)
            }
            val uri =
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            if (uri != null) {
                val output = context.contentResolver.openOutputStream(uri)
                output?.write(bytes)
                output?.close()

                values = ContentValues().apply {
                    put(MediaStore.Images.ImageColumns.IS_PENDING, false)
                }
                context.contentResolver.update(uri, values, null, null)
            }
        }

        saveViewModel.saving.value -= page.filename
    }.start()
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun Gallery(fetcherKey: String, navController: NavController, popBack: Boolean = false) {
    val fetcherViewModel = globalViewModel<FetcherViewModel>()
    val fetcher = fetcherViewModel.get(fetcherKey)

    val illusts =
        if (fetcher.meta is SearchMeta) fetcher.meta.filters.filter(fetcher.illusts)
        else fetcher.illusts

    val pagerState = rememberPagerState(fetcher.current, 0f) { illusts.size }

    fun onBack() {
        if (popBack)
            fetcherViewModel.pop()
        navController.navigateUp()
    }

    AppTheme {
        Box {
            HorizontalPager(pagerState) { i ->
                if (i == pagerState.pageCount - 1)
                    LaunchedEffect("fetchMore") {
                        fetcherViewModel.fetch(fetcherKey)
                    }

                IllustView(navController, illusts[i], ::onBack)
            }

            SaveToast(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    BackHandler(onBack = ::onBack)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun IllustView(navController: NavController, illust: Illust, onBack: () -> Unit = {}) {
    val context = LocalContext.current

    val saveViewModel = globalViewModel<SaveViewModel>()
    val fetcherViewModel = globalViewModel<FetcherViewModel>()

    val scrollState = rememberScrollState()
    val savingPages by saveViewModel.saving.collectAsState()

    val mutedTags by Prefs.MUTED_TAGS.listState()
    val mutedUsers by Prefs.MUTED_USERS.listState()

    var loading by rememberSaveable { mutableStateOf(true) }

    var bookmarked by rememberSaveable { mutableStateOf(illust.is_bookmarked) }
    var loadingBookmark by rememberSaveable { mutableStateOf(false) }

    val allDownloaded = illust.pages.all { File(getImagesDir(), it.filename).exists() }

    var showBookmarkDialog by rememberSaveable { mutableStateOf(false) }

    if (showBookmarkDialog)
        BookmarkDialog(illust.id) { showBookmarkDialog = false }

    Surface {
        Scaffold(
            topBar = {
                TopAppBar {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onBack() }) {
                                Icon(
                                    Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = "back"
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    fetcherViewModel.push(
                                        mapOf(
                                            "bookmark" to IllustFetcher.bookmark(
                                                BookmarkMeta(
                                                    "public",
                                                    illust.user.id
                                                )
                                            ),
                                            "user" to IllustFetcher.user(UserMeta(illust.user.id))
                                        )
                                    )
                                    navController.navigate("user/${illust.user.id}")
                                }
                            ) {
                                AsyncImage(
                                    pixivImage(illust.user.profile_image_urls.values.first()),
                                    contentDescription = illust.user.account,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(36.dp)
                                        .background(MaterialTheme.colors.background)
                                )

                                Column {
                                    Text(
                                        illust.user.name,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = MaterialTheme.colors.onBackground
                                    )
                                    Text(illust.user.account, fontSize = 12.sp, lineHeight = 16.sp)
                                }
                            }
                        }

                        IconButton(onClick = {
                            illust.pages.forEach { downloadPage(context, saveViewModel, it) }
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
            Box(
                modifier = Modifier.padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 76.dp)
                ) {

                    illust.pages.mapIndexed { i, page ->
                        var size: IntSize by remember { mutableStateOf(IntSize(0, 0)) }

                        var mutedOverlay by remember {
                            mutableStateOf(
                                illust.tags.any { tag -> mutedTags.any { tag.match(it) } }
                                        || mutedUsers.contains(illust.user.account)
                            )
                        }
                        var r18Overlay by remember {
                            mutableStateOf(illust.r18 && Prefs.APPEARANCE_BLUR_R18.get() == "true")
                        }

                        Box {
                            var imgMod = Modifier.fillMaxWidth()

                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                imgMod = imgMod.aspectRatio(1f)
                            }

                            if (mutedOverlay || r18Overlay)
                                imgMod = imgMod.blur(32.dp)

                            AsyncImage(
                                pixivImage(page.image_urls.large),
                                contentDescription = illust.caption,
                                contentScale = ContentScale.FillWidth,
                                modifier = imgMod.onSizeChanged { size = it },
                                onSuccess = { loading = false }
                            )

                            var overlayMod = Modifier
                                .background(MaterialTheme.colors.background.copy(0.5f))

                            if (size.height > 0)
                                overlayMod = overlayMod.aspectRatio(
                                    size.width / size.height.toFloat()
                                )

                            if (mutedOverlay || r18Overlay)
                                Box(modifier = overlayMod.clickable {
                                    r18Overlay = false
                                    mutedOverlay = false
                                }) {
                                    Box(modifier = Modifier.align(Alignment.Center)) {
                                        when {
                                            mutedOverlay -> Placeholder(Icons.Default.VisibilityOff)
                                            r18Overlay -> Placeholder(Icons.Default._18UpRating)
                                        }
                                    }
                                }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6F))
                                    .size(48.dp)
                                    .clickable { downloadPage(context, saveViewModel, page) },
                            ) {
                                if (savingPages.contains(page.filename))
                                    CircularProgressIndicator(
                                        strokeWidth = 3.dp,
                                        color = Color.White,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.Center)
                                    )
                                else
                                    Icon(
                                        if (File(getImagesDir(), page.filename).exists())
                                            Icons.Default.DownloadDone
                                        else
                                            Icons.Default.Download,
                                        tint = Color.White,
                                        contentDescription = "download",
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                            }

                            if (illust.page_count > 1)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6F))
                                        .size(32.dp)
                                ) {
                                    Text(
                                        (i + 1).toString(),
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(top = 1.dp),
                                    )
                                }


                        }
                    }

                    val lightColor = MaterialTheme.colors.onBackground.copy(alpha = 0.7f)

                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(illust.title, fontSize = 20.sp)

                        if (illust.caption.isNotBlank())
                            HtmlText(illust.caption)

                        Text(
                            displayDateTimeFormat.format(pixivDateFormat.parse(illust.create_date)),
                            color = lightColor
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "Views",
                                tint = lightColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                illust.total_view.toString(),
                                color = lightColor,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Bookmarks",
                                tint = lightColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(illust.total_bookmarks.toString(), color = lightColor)
                        }

                        FlowRow(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            illust.tags.map { TagChip(it, navController = navController) }
                        }
                    }
                }

                Surface(
                    elevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(56.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.surface)
                        .combinedClickable(
                            onClick = {
                                if (!loadingBookmark)
                                    Thread {
                                        loadingBookmark = true
                                        try {
                                            runBlocking {
                                                if (bookmarked) PixivApi.instance.deleteBookmark(
                                                    illust.id
                                                )
                                                else PixivApi.instance.addBookmark(illust.id)
                                                bookmarked = !bookmarked
                                            }
                                        } catch (e: HttpException) {
                                            e.printStackTrace()
                                        }
                                        loadingBookmark = false
                                    }.start()
                            },
                            onLongClick = {
                                if (!loadingBookmark)
                                    showBookmarkDialog = true
                            }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        if (loadingBookmark)
                            CircularProgressIndicator(
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colors.onBackground,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center),
                            )
                        else
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Favorite",
                                tint = if (bookmarked) MaterialTheme.colors.secondary
                                else MaterialTheme.colors.onBackground,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.Center),
                            )
                    }
                }
            }
        }
    }
}