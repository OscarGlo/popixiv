package dev.oscarglo.popixiv.activities.views

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.oscarglo.popixiv.activities.components.IllustGrid
import dev.oscarglo.popixiv.activities.components.Select
import dev.oscarglo.popixiv.activities.viewModels.BookmarkMeta
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.activities.viewModels.FollowMeta
import dev.oscarglo.popixiv.activities.viewModels.IllustFetcher
import dev.oscarglo.popixiv.activities.viewModels.RestrictMeta
import dev.oscarglo.popixiv.util.globalViewModel
import java.util.Date
import java.util.Locale

val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)

fun dateToString(date: Date): String {
    val day = date.date.toString().padStart(2, '0')
    val month = (1 + date.month).toString().padStart(2, '0')
    val year = 1900 + date.year
    return "$day/$month/$year"
}

@Composable
fun IllustGridPage(
    fetcherKey: String,
    navController: NavController,
    modifier: Modifier = Modifier,
    showDates: Boolean = false,
    hasBackButton: Boolean = false
) {
    val fetcherViewModel = globalViewModel<FetcherViewModel>()
    val fetcher = fetcherViewModel.get(fetcherKey)

    var currentDate by rememberSaveable { mutableStateOf("Loading...") }

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
                            Text(currentDate, modifier = Modifier.padding(8.dp))
                    }

                    if (fetcher.meta is RestrictMeta)
                        RestrictFilter(fetcherKey)
                }
            }
        }
    ) { padding ->
        IllustGrid(
            fetcherKey,
            navController,
            showDates = showDates,
            onDateChange = { currentDate = it },
            modifier = modifier.padding(padding)
        )
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