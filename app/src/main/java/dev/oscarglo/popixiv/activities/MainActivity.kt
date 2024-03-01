package dev.oscarglo.popixiv.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.oscarglo.popixiv.activities.components.SaveToast
import dev.oscarglo.popixiv.activities.viewModels.BookmarkMeta
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.activities.viewModels.IllustFetcher
import dev.oscarglo.popixiv.activities.viewModels.UserMeta
import dev.oscarglo.popixiv.activities.views.Gallery
import dev.oscarglo.popixiv.activities.views.IllustGridPage
import dev.oscarglo.popixiv.activities.views.SearchPage
import dev.oscarglo.popixiv.activities.views.SettingTabPage
import dev.oscarglo.popixiv.activities.views.SettingsPage
import dev.oscarglo.popixiv.activities.views.UserPage
import dev.oscarglo.popixiv.api.AuthApi
import dev.oscarglo.popixiv.api.User
import dev.oscarglo.popixiv.ui.theme.AppTheme
import dev.oscarglo.popixiv.util.Prefs
import dev.oscarglo.popixiv.util.globalViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException

class AppViewModel : ViewModel() {
    val user = MutableStateFlow<User?>(null)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Prefs.init(this)
        if (!Prefs.PIXIV_REFRESH_TOKEN.exists())
            return startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })

        setContent {
            AppTheme {
                AppRouter()
            }
        }
    }
}

@Composable
fun AppRouter() {
    val navController = rememberNavController()

    val fetcherViewModel = globalViewModel<FetcherViewModel>()
    val appViewModel = globalViewModel<AppViewModel>()

    val user by appViewModel.user.collectAsState()

    LaunchedEffect("loadUser") {
        if (user == null)
            Thread {
                runBlocking {
                    try {
                        appViewModel.user.value = AuthApi.refreshTokens().user
                    } catch (e: HttpException) {
                        e.printStackTrace()
                    }
                }
            }.start()
    }

    LaunchedEffect(user) {
        if (user == null)
            return@LaunchedEffect

        fetcherViewModel.updateLast("bookmark") {
            (this as IllustFetcher<BookmarkMeta>).copy(
                BookmarkMeta("public", user!!.id),
                done = false
            )
        }
        fetcherViewModel.updateLast("user") {
            (this as IllustFetcher<UserMeta>).copy(
                UserMeta(user!!.id),
                done = false
            )
        }
    }

    NavHost(
        navController,
        "home",
        // Foreground transitions
        enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Up,
                tween(400)
            ) + fadeIn(tween(200))
        },
        popExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Down,
                tween(400),
            ) + fadeOut(tween(400, 200))
        },
        // Background transitions
        exitTransition = {
            // Dummy animation to keep background visible
            fadeOut(tween(1, 400))
        },
        popEnterTransition = { EnterTransition.None }
    ) {
        composable("home") {
            HomeLayout(navController)
        }
        composable(
            "settings/{tab}",
            arguments = listOf(navArgument("tab") { type = NavType.StringType })
        ) {
            SettingTabPage(it.arguments?.getString("tab")!!, navController)
        }
        composable(
            "user/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) {
            UserPage(navController, id = it.arguments?.getLong("id")!!, hasBackButton = true)
        }

        composable(
            "grid/{key}",
            arguments = listOf(navArgument("key") { type = NavType.StringType })
        ) {
            IllustGridPage(it.arguments?.getString("key")!!, navController, hasBackButton = true)
        }
        composable(
            "gallery/{key}",
            arguments = listOf(navArgument("key") { type = NavType.StringType })
        ) {
            Gallery(it.arguments?.getString("key")!!, navController)
        }
        composable(
            "search/{query}",
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) {
            SearchPage(navController, it.arguments?.getString("query")!!)
        }
    }
}

class NavigationTab(val icon: ImageVector, val label: String, val content: @Composable () -> Unit)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeLayout(navController: NavController) {
    val activity = LocalContext.current as MainActivity

    val appViewModel = globalViewModel<AppViewModel>()
    val user by appViewModel.user.collectAsState()

    val navigationTabs = listOf(
        NavigationTab(Icons.Default.Home, "Feed") {
            IllustGridPage(
                "follow",
                navController,
                showDates = true
            )
        },
        NavigationTab(Icons.Default.Search, "Search") {
            SearchPage(navController)
        },
        NavigationTab(Icons.Default.Person, "Account") {
            if (user != null)
                UserPage(navController, user = user!!)
        },
        NavigationTab(Icons.Default.Settings, "Settings") {
            SettingsPage(navController)
        },
    )

    val initialPage = 0

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage, 0f) { navigationTabs.size }

    Scaffold(bottomBar = {
        BottomNavigation {
            navigationTabs.mapIndexed { i, tab ->
                BottomNavigationItem(
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) },
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(i, 0f)
                        }
                    },
                    selected = pagerState.currentPage == i,
                )
            }
        }
    }) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            HorizontalPager(pagerState) { i ->
                Surface(modifier = Modifier.fillMaxSize()) {
                    navigationTabs[i].content()
                }
            }

            SaveToast(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    BackHandler {
        if (pagerState.currentPage != initialPage)
            coroutineScope.launch {
                pagerState.animateScrollToPage(initialPage, 0f)
            }
        else
            activity.moveTaskToBack(false);
    }
}
