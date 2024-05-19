package dev.oscarglo.popixiv.activities.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.oscarglo.popixiv.activities.components.dialog.SearchFilters
import dev.oscarglo.popixiv.activities.components.dialog.TagDialog
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.activities.viewModels.IllustFetcher
import dev.oscarglo.popixiv.activities.viewModels.SearchMeta
import dev.oscarglo.popixiv.api.Tag
import dev.oscarglo.popixiv.util.Prefs
import dev.oscarglo.popixiv.util.globalViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagChip(tag: Tag, modifier: Modifier = Modifier, navController: NavController? = null) {
    val fetcherViewModel = globalViewModel<FetcherViewModel>()

    val mutedTags by Prefs.MUTED_TAGS.listState()
    val highlightTags by Prefs.HIGHLIGHT_TAGS.listState()

    var dialogOpen by remember { mutableStateOf(false) }
    if (dialogOpen)
        TagDialog(tag) { dialogOpen = false }

    var mod = modifier
    if (navController != null)
        mod = mod.combinedClickable(
            onClick = {
                fetcherViewModel.push(
                    mapOf(
                        "search" to IllustFetcher.search(SearchMeta(SearchFilters(listOf(tag))))
                    )
                )
                navController.navigate("search/${tag}")
            },
            onLongClick = {
                dialogOpen = true
            }
        )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = mod
            .clip(CircleShape)
            .background(
                when {
                    highlightTags.contains(tag.name) -> Color(0xff41dcae)
                    mutedTags.contains(tag.name) -> MaterialTheme.colors.onBackground.copy(0.2f)
                    tag.name == "R-18" -> MaterialTheme.colors.secondary
                    else -> MaterialTheme.colors.primary
                }
            )
            .padding(horizontal = 8.dp),
    ) {
        if (tag.negative)
            Text("-")

        Text(
            text = tag.name,
            fontSize = 15.sp,
            color = MaterialTheme.colors.onPrimary,
            fontWeight = FontWeight.SemiBold,
            softWrap = false
        )

        if (tag.translated_name != null)
            Text(
                text = tag.translated_name,
                fontSize = 14.sp,
                color = MaterialTheme.colors.onPrimary.copy(alpha = 0.9f),
                softWrap = false,
            )
    }
}