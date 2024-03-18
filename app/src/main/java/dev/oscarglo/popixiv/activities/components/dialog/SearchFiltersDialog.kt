package dev.oscarglo.popixiv.activities.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.oscarglo.popixiv.activities.components.Select
import dev.oscarglo.popixiv.activities.components.TitleDialog
import dev.oscarglo.popixiv.api.Illust

val sortLabels = mapOf(
    "date_desc" to "Date (new)",
    "date_asc" to "Date (old)",
    "popular_desc" to "Bookmarks"
)

val durationLabels = mapOf(
    "within_last_day" to "Day",
    "within_last_week" to "Week",
    "within_last_month" to "Month",
    "within_half_year" to "6 months",
    "within_year" to "Year",
    null to "All time"
)

data class SearchFilters(
    val sort: String = "date_desc",
    val duration: String? = null,
    val minBookmarks: Int? = null,
) {
    fun filter(illusts: List<Illust>) = illusts.filter { it.total_bookmarks >= (minBookmarks ?: 0) }
}

val searchFiltersSaver = Saver<SearchFilters, String>(
    { it.sort + "–" + it.duration + "–" + it.minBookmarks.toString() },
    {
        val parts = it.split("–")
        SearchFilters(parts[0], parts[1], parts[2].toIntOrNull())
    }
)

@Composable
fun SearchFiltersDialog(filters: SearchFilters, onClose: (filters: SearchFilters?) -> Unit = {}) {
    var state by remember { mutableStateOf(filters) }

    TitleDialog("Filters", onClose = { onClose(null) }) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sort")

            Select(
                values = listOf("date_desc", "date_asc", "popular_desc"),
                value = state.sort,
                onChange = { state = state.copy(sort = it) },
                render = { Text(sortLabels[it]!!) },
                modifier = Modifier.width(144.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Duration")

            Select(
                values = listOf(
                    "within_last_day",
                    "within_last_week",
                    "within_last_month",
                    "within_half_year",
                    "within_year",
                    null
                ),
                value = state.duration,
                onChange = { state = state.copy(duration = it) },
                render = { Text(durationLabels[it]!!) },
                modifier = Modifier.width(144.dp)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Min. bookmarks")

            TextField(
                value = if (state.minBookmarks != null) state.minBookmarks.toString() else "",
                onValueChange = { state = state.copy(minBookmarks = it.toIntOrNull()) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
            )
        }

        Button(
            onClick = { onClose(state) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply")
        }
    }
}