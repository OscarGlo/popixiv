package dev.oscarglo.popixiv.activities.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled._18UpRating
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.oscarglo.popixiv.activities.components.PasswordTextField
import dev.oscarglo.popixiv.activities.components.Select
import dev.oscarglo.popixiv.ui.theme.AppTheme
import dev.oscarglo.popixiv.util.Prefs
import kotlin.math.roundToInt

class SettingTab(val icon: ImageVector, val content: @Composable () -> Unit)

@Composable
fun switchColors() = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colors.primary,
    checkedTrackColor = MaterialTheme.colors.primary.copy(alpha = 0.7f),
)

val settingsTabs = mapOf(
    "Appearance" to SettingTab(Icons.Default.Visibility) {
        var theme by Prefs.APPEARANCE_THEME.state()
        var blurR18 by Prefs.APPEARANCE_BLUR_R18.booleanState()
        var gridStagger by Prefs.APPEARANCE_GRID_STAGGER.booleanState()
        var cardMulti by Prefs.APPEARANCE_CARD_MULTI.booleanState()
        var gridGap by Prefs.APPEARANCE_GRID_GAP.intState()

        println(gridGap)

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(Icons.Default.Brightness6, contentDescription = "theme")
                Text("Theme")
            }

            Select(
                listOf("system", "light", "dark"),
                theme,
                onChange = { theme = it },
                modifier = Modifier.width(128.dp),
                render = { Text(it.capitalize()) }
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { blurR18 = !blurR18 }
                .padding(16.dp, 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(Icons.Default._18UpRating, contentDescription = "blur r18")
                Text("Blur R18 previews")
            }
            Switch(
                checked = blurR18,
                onCheckedChange = { blurR18 = it },
                colors = switchColors()
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { gridStagger = !gridStagger }
                .padding(16.dp, 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(Icons.Default.Dashboard, contentDescription = "stagger")
                Text("Stagger image grid")
            }
            Switch(
                checked = gridStagger,
                onCheckedChange = { gridStagger = it },
                colors = switchColors()
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { cardMulti = !cardMulti }
                .padding(16.dp, 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(Icons.Default.DashboardCustomize, contentDescription = "multi")
                Text("Multi page preview")
            }
            Switch(
                checked = cardMulti,
                onCheckedChange = { cardMulti = it },
                colors = switchColors()
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp, start = 16.dp, end = 24.dp)
        ) {
            Text("Grid gap")
            Slider(
                value = gridGap.toFloat(),
                onValueChange = { gridGap = it.roundToInt() },
                steps = 7,
                valueRange = 0f..8f,
                modifier = Modifier.weight(1f)
            )
            Text(gridGap.toString(), modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
        }
    },
    "Reverse search" to SettingTab(Icons.Default.ImageSearch) {
        var sauceNaoKey by rememberSaveable { mutableStateOf(Prefs.SAUCENAO_TOKEN.get()) }

        Box(modifier = Modifier.padding(16.dp)) {
            PasswordTextField(
                value = sauceNaoKey,
                onValueChange = {
                    Prefs.SAUCENAO_TOKEN.set(it)
                    sauceNaoKey = it
                },
                label = "SauceNAO API key",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
)

@Composable
fun SettingsPage(navController: NavController) {
    Surface {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Settings") })
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                settingsTabs.map { (key, tab) ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("settings/$key")
                            }
                            .padding(16.dp)
                    ) {
                        Icon(tab.icon, contentDescription = key)
                        Text(key)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingTabPage(name: String, navController: NavController) {
    val tab = settingsTabs[name]
    if (tab == null) {
        navController.navigateUp()
        return
    }

    AppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(name) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "back"
                            )
                        }
                    })
            }
        ) { padding ->
            Surface {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    tab.content()
                }
            }
        }
    }
}