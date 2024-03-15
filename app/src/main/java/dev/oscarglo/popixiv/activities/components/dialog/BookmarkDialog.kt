package dev.oscarglo.popixiv.activities.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.oscarglo.popixiv.activities.components.TitleDialog
import dev.oscarglo.popixiv.api.PixivApi
import dev.oscarglo.popixiv.ui.theme.switchColors
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException

@Composable
fun BookmarkDialog(id: Long, onClose: () -> Unit = {}) {
    var loading by remember { mutableStateOf(false) }
    var private by remember { mutableStateOf(false) }

    TitleDialog("Edit bookmark", onClose) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { private = !private }
        ) {
            Text("Private")
            Switch(
                checked = private,
                onCheckedChange = { private = it },
                colors = switchColors()
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (loading) return@Button
                    loading = true
                    try {
                        runBlocking {
                            PixivApi.instance.addBookmark(
                                id,
                                restrict = if (private) "private" else "public"
                            )
                            onClose()
                        }
                    } catch (e: HttpException) {
                        e.printStackTrace()
                        loading = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                if (loading)
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                else
                    Text("Save")
            }

            Button(
                onClick = {
                    if (loading) return@Button
                    loading = true
                    try {
                        runBlocking {
                            PixivApi.instance.deleteBookmark(id)
                            onClose()
                        }
                    } catch (e: HttpException) {
                        e.printStackTrace()
                        loading = false
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
                modifier = Modifier.weight(1f)
            ) {
                if (loading)
                    CircularProgressIndicator(
                        strokeWidth = 3.dp,
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                else
                    Text("Remove")
            }
        }
    }
}