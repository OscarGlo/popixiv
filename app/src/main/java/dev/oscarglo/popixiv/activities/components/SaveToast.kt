package dev.oscarglo.popixiv.activities.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import dev.oscarglo.popixiv.util.globalViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class SaveViewModel : ViewModel() {
    val saving = MutableStateFlow(setOf<String>())
}

@Composable
fun SaveToast(modifier: Modifier = Modifier) {
    val saveViewModel = globalViewModel<SaveViewModel>()
    val saving by saveViewModel.saving.collectAsState()

    AnimatedVisibility(
        visible = saving.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .padding(bottom = 8.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(top = 8.dp, bottom = 8.dp, start = 8.dp, end = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CircularProgressIndicator(
                strokeWidth = 3.dp,
                color = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Text("Saving ${saving.size} image${if (saving.size > 1) "s" else ""}...", color = Color.White)
        }
    }
}