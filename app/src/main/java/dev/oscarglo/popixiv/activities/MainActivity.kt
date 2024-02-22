package dev.oscarglo.popixiv.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.oscarglo.popixiv.activities.components.SaveToast
import dev.oscarglo.popixiv.activities.views.Gallery
import dev.oscarglo.popixiv.activities.views.IllustGrid
import dev.oscarglo.popixiv.activities.views.SettingTabPage
import dev.oscarglo.popixiv.activities.views.SettingsPage
import dev.oscarglo.popixiv.ui.theme.AppTheme
import dev.oscarglo.popixiv.util.Prefs
import dev.oscarglo.popixiv.util.getImagesDir
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        getImagesDir().mkdir()

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
        composable("home") { HomeLayout(navController) }
        composable(
            "gallery/{key}",
            arguments = listOf(navArgument("key") { type = NavType.StringType })
        ) {
            val key = it.arguments?.getString("key")!!
            Gallery(key, navController)
        }
        composable(
            "settings/{tab}",
            arguments = listOf(navArgument("tab") { type = NavType.StringType })
        ) {
            val key = it.arguments?.getString("tab")!!
            SettingTabPage(key, navController)
        }
    }
}

class NavigationTab(val icon: ImageVector, val label: String, val content: @Composable () -> Unit)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeLayout(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(0, 0f) { 3 }

    val navigationTabs = listOf(
        NavigationTab(Icons.Default.Home, "Feed") {
            IllustGrid(
                "follow",
                navController,
                showDates = true
            )
        },
        NavigationTab(Icons.Default.Person, "Account") {
            IllustGrid("bookmark", navController)
        },
        NavigationTab(Icons.Default.Settings, "Settings") {
            SettingsPage(navController)
        },
    )

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
}
