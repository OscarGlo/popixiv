package dev.oscarglo.popixiv.activities.views

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.oscarglo.popixiv.activities.AppViewModel
import dev.oscarglo.popixiv.activities.components.dialog.RestrictDialog
import dev.oscarglo.popixiv.activities.components.illust.PreviewGrid
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.api.PixivApi
import dev.oscarglo.popixiv.api.User
import dev.oscarglo.popixiv.util.globalViewModel
import dev.oscarglo.popixiv.util.pixivImage
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserPage(
    navController: NavController,
    id: Long? = null,
    user: User? = null,
    hasBackButton: Boolean = false
) {
    val context = LocalContext.current

    val appViewModel = globalViewModel<AppViewModel>()
    val fetcherViewModel = globalViewModel<FetcherViewModel>()

    val appUser by appViewModel.user.collectAsState()
    var user by remember { mutableStateOf(user) }

    var followed by rememberSaveable { mutableStateOf(user?.is_followed ?: false) }
    var loadingFollow by rememberSaveable { mutableStateOf(false) }

    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect("fetchUser") {
        if (user == null)
            Thread {
                runBlocking {
                    try {
                        user = PixivApi.instance.getUserDetail(id!!).user
                        followed = user!!.is_followed!!
                    } catch (e: HttpException) {
                        e.printStackTrace()
                    }
                }
            }.start()
    }

    var followDialogOpen by remember { mutableStateOf(false) }
    if (followDialogOpen)
        RestrictDialog(
            title = "Edit follow",
            onAdd = { PixivApi.instance.addFollow(user!!.id, it) },
            onDelete = { PixivApi.instance.deleteFollow(user!!.id) },
            onClose = { followDialogOpen = false }
        )

    fun handleBack() {
        fetcherViewModel.pop()
        navController.navigateUp()
    }

    val scrollState = rememberScrollState()

    Scaffold(topBar = {
        if (hasBackButton) {
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

                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Image actions",
                        )

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    context.startActivity(Intent.createChooser(Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "https://www.pixiv.net/en/users/${user?.id}"
                                        )
                                        type = "text/plain"
                                    }, null))
                                },
                                enabled = user != null
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "share link"
                                    )
                                    Text("Share link")
                                }
                            }
                        }
                    }
                }
            }
        }
    }) { padding ->
        Box(
            modifier = Modifier.padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            if (user != null) pixivImage(user!!.profile_image_urls.values.last()) else null,
                            contentDescription = user?.account,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(80.dp)
                                .background(MaterialTheme.colors.background)
                        )

                        Column {
                            Text(
                                user?.name ?: "Loading...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (user != null)
                                Text(user!!.account, fontSize = 14.sp)
                        }
                    }

                    if (user != null && appUser != null && user!!.id != appUser!!.id)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (followed) MaterialTheme.colors.primary
                                    else MaterialTheme.colors.onSurface.copy(0.15f)
                                )
                                .combinedClickable(
                                    onClick = {
                                        if (!loadingFollow)
                                            Thread {
                                                runBlocking {
                                                    loadingFollow = true
                                                    try {
                                                        if (followed) PixivApi.instance.deleteFollow(
                                                            user!!.id
                                                        )
                                                        else PixivApi.instance.addFollow(user!!.id)

                                                        followed = !followed
                                                    } catch (e: HttpException) {
                                                        e.printStackTrace()
                                                    }
                                                    loadingFollow = false
                                                }
                                            }.start()
                                    },
                                    onLongClick = { followDialogOpen = true }
                                )
                                .padding(12.dp, 8.dp)
                        ) {
                            val color =
                                if (followed) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface

                            if (loadingFollow)
                                CircularProgressIndicator(
                                    strokeWidth = 3.dp,
                                    color = color,
                                    modifier = Modifier.size(24.dp)
                                )
                            else
                                Icon(
                                    if (followed) Icons.Default.Person else Icons.Default.PersonAdd,
                                    contentDescription = if (followed) "add" else "remove",
                                    tint = color,
                                )

                            Text(if (followed) "Following" else "Follow", color = color)
                        }
                }

                PreviewGrid(navController, "user", "Illusts")
                PreviewGrid(navController, "bookmark", "Bookmarks")
            }
        }
    }

    if (hasBackButton)
        BackHandler(onBack = ::handleBack)
}