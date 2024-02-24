package dev.oscarglo.popixiv.activities.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dev.oscarglo.popixiv.activities.viewModels.FetcherViewModel
import dev.oscarglo.popixiv.util.Prefs
import dev.oscarglo.popixiv.util.globalViewModel
import kotlin.math.min

@Composable
fun PreviewGrid(navController: NavController, fetcherKey: String, label: String) {
    val fetcherViewModel = globalViewModel<FetcherViewModel>()

    val fetcher = fetcherViewModel.get(fetcherKey)

    val gap by Prefs.APPEARANCE_GRID_GAP.intState()
    val cardSize by Prefs.APPEARANCE_CARD_SIZE.intState()

    LaunchedEffect(fetcher) {
        println("$fetcherKey: ${fetcher.illusts.size}")
        if (fetcher.illusts.isEmpty() && !fetcher.done)
            fetcherViewModel.fetch(fetcherKey)
    }

    BoxWithConstraints {
        val lines = if (cardSize < 200) 2 else 1
        val count = min(fetcher.illusts.size, (maxWidth.value.toInt() / cardSize) * lines)

        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(8.dp)
                )

                if (fetcher.illusts.size > count)
                    Row(
                        modifier = Modifier
                            .clickable {
                                navController.navigate("grid/$fetcherKey")
                            }
                            .padding(8.dp)
                    ) {
                        Text("More")
                        Icon(Icons.Default.ChevronRight, contentDescription = "more")
                    }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(cardSize.dp),
                verticalArrangement = Arrangement.spacedBy(gap.dp),
                horizontalArrangement = Arrangement.spacedBy(gap.dp)
            ) {
                this.items(count) { i ->
                    IllustCard(
                        fetcher.illusts[i],
                        onClick = {
                            fetcherViewModel.updateLast(fetcherKey) {
                                fetcher.copy(current = i)
                            }
                            navController.navigate("gallery/$fetcherKey")
                        },
                        gap = (gap / 2).dp
                    )
                }
            }
        }
    }

}