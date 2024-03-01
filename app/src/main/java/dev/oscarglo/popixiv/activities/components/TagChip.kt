package dev.oscarglo.popixiv.activities.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.activities.viewModels.IllustFetcher
import dev.oscarglo.popixiv.activities.viewModels.SearchMeta
import dev.oscarglo.popixiv.api.Tag
import dev.oscarglo.popixiv.util.globalViewModel

@Composable
fun TagChip(tag: Tag, modifier: Modifier = Modifier, navController: NavController? = null) {
    val fetcherViewModel = globalViewModel<FetcherViewModel>()

    var mod = modifier
    if (navController != null)
        mod = mod.clickable {
            fetcherViewModel.push(
                mapOf(
                    "search" to IllustFetcher.search(SearchMeta(tag.name))
                )
            )
            navController.navigate("search/${tag.name}–${tag.translated_name}")
        }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = mod
            .clip(CircleShape)
            .background(
                if (tag.name == "R-18") MaterialTheme.colors.secondary
                else MaterialTheme.colors.primary
            )
            .padding(horizontal = 8.dp),
    ) {
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