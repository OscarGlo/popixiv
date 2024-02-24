package dev.oscarglo.popixiv.activities.views

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.oscarglo.popixiv.activities.components.PreviewGrid
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.api.PixivApi
import dev.oscarglo.popixiv.api.User
import dev.oscarglo.popixiv.util.globalViewModel
import dev.oscarglo.popixiv.util.pixivImage
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException

@Composable
fun UserPage(
    navController: NavController,
    id: Long? = null,
    user: User? = null,
    hasBackButton: Boolean = false
) {
    val fetcherViewModel = globalViewModel<FetcherViewModel>()

    var user by remember { mutableStateOf(user) }

    LaunchedEffect("fetchUser") {
        if (user == null)
            Thread {
                runBlocking {
                    try {
                        user = PixivApi.instance.getUserDetail(id!!).user
                    } catch (e: HttpException) {
                        e.printStackTrace()
                    }
                }
            }.start()
    }

    fun handleBack() {
        fetcherViewModel.pop()
        navController.navigateUp()
    }

    Scaffold(topBar = {
        if (hasBackButton) {
            TopAppBar(navigationIcon = {
                IconButton(onClick = ::handleBack) {
                    Icon(
                        Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "back"
                    )
                }
            }, title = {})
        }
    }) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    AsyncImage(
                        if (user != null) pixivImage(user!!.profile_image_urls.values.last()) else null,
                        contentDescription = user?.account,
                        modifier = Modifier
                            .clip(CircleShape)
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

                PreviewGrid(navController, "user", "Illusts")
                PreviewGrid(navController, "bookmark", "Bookmarks")
            }
        }
    }

    if (hasBackButton)
        BackHandler(onBack = ::handleBack)
}