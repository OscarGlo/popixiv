package dev.oscarglo.popixiv.activities.components.illust

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.oscarglo.popixiv.activities.components.dialog.BookmarkDialog
import dev.oscarglo.popixiv.api.Illust
import dev.oscarglo.popixiv.api.ImageUrls
import dev.oscarglo.popixiv.api.PixivApi
import dev.oscarglo.popixiv.util.Prefs
import dev.oscarglo.popixiv.util.pixivImage
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IllustCard(
    illust: Illust,
    modifier: Modifier = Modifier,
    largePreview: Boolean = false,
    onClick: (illust: Illust) -> Unit,
    gap: Dp = 0.dp
) {
    var bookmarked by rememberSaveable { mutableStateOf(illust.is_bookmarked) }
    var loadingBookmark by rememberSaveable { mutableStateOf(false) }

    val multi by Prefs.APPEARANCE_CARD_MULTI.booleanState()
    val blurR18 by Prefs.APPEARANCE_BLUR_R18.booleanState()
    val mutedTags by Prefs.MUTED_TAGS.listState()
    val mutedUsers by Prefs.MUTED_USERS.listState()

    @Composable
    fun IllustPreview(
        imageUrls: ImageUrls, modifier: Modifier = Modifier
    ) {
        var loading by rememberSaveable { mutableStateOf(true) }

        var imgMod = modifier.fillMaxWidth()

        if (blurR18 && illust.r18 ||
            illust.tags.any { tag -> mutedTags.any { tag.match(it) } }
            || mutedUsers.contains(illust.user.account)
        )
            imgMod = imgMod.blur(16.dp)

        if (loading)
            imgMod = imgMod.aspectRatio(1f)

        AsyncImage(
            pixivImage(if (largePreview) imageUrls.medium else imageUrls.square_medium),
            contentDescription = "preview",
            contentScale = ContentScale.Crop,
            modifier = imgMod,
            onSuccess = { loading = false },
        )
    }

    var showBookmarkDialog by rememberSaveable { mutableStateOf(false) }

    if (showBookmarkDialog)
        BookmarkDialog(illust.id) { showBookmarkDialog = false }

    val mod = if (largePreview) modifier else modifier.aspectRatio(1f)
    Box(
        modifier = mod.background(MaterialTheme.colors.background),
    ) {
        Box(modifier = Modifier.clickable { onClick(illust) }) {
            if (!multi || illust.page_count == 1) IllustPreview(illust.image_urls)
            else when (illust.page_count) {
                2 -> Column(
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    IllustPreview(
                        illust.pages[0].image_urls,
                        modifier = Modifier.fillMaxHeight(0.5f)
                    )
                    IllustPreview(illust.pages[1].image_urls)
                }

                3 -> Column(
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    IllustPreview(
                        illust.pages[0].image_urls,
                        modifier = Modifier.fillMaxHeight(0.5f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        IllustPreview(
                            illust.pages[1].image_urls,
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                        IllustPreview(illust.pages[2].image_urls)
                    }
                }

                else -> Column(
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier.fillMaxHeight(0.5f)
                    ) {
                        IllustPreview(
                            illust.pages[0].image_urls,
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                        IllustPreview(illust.pages[1].image_urls)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        IllustPreview(
                            illust.pages[2].image_urls,
                            modifier = Modifier.fillMaxWidth(0.5f)
                        )
                        IllustPreview(illust.pages[3].image_urls)
                    }
                }
            }
        }

        // Page count
        val showCountAt = if (multi) 4 else 1
        if (illust.type == "ugoira" || illust.page_count > showCountAt) Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6F))
                .size(25.dp)
        ) {
            if (illust.type == "ugoira") Icon(
                Icons.Default.PlayArrow,
                contentDescription = "gif",
                modifier = Modifier.align(Alignment.TopCenter)
            )
            else Text(
                illust.page_count.toString(),
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 1.dp),
            )
        }

        // Like count
        Row(modifier = Modifier
            .align(Alignment.BottomEnd)
            .clip(RoundedCornerShape(8.dp, 0.dp, 0.dp, 0.dp))
            .background(Color.Black.copy(alpha = 0.6F))
            .combinedClickable(
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
                onLongClick = {
                    if (!loadingBookmark)
                        showBookmarkDialog = true
                }
            )
            .padding(6.dp, 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(illust.total_bookmarks.toString(), color = Color.White)
            if (loadingBookmark)
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(2.dp)
                )
            else
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(20.dp),
                    tint = if (bookmarked) MaterialTheme.colors.secondary else Color.White
                )
        }
    }
}

