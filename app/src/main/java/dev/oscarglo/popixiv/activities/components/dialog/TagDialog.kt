package dev.oscarglo.popixiv.activities.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dev.oscarglo.popixiv.api.Tag
import dev.oscarglo.popixiv.util.Prefs

@Composable
fun TagDialog(tag: Tag, onClose: () -> Unit = {}) {
    var highlightTags by Prefs.HIGHLIGHT_TAGS.listState()
    var mutedTags by Prefs.MUTED_TAGS.listState()

    println(Prefs.MUTED_TAGS.get())

    val highlighted = highlightTags.any { tag.match(it) }
    val muted = mutedTags.any { tag.match(it) }

    Dialog(onDismissRequest = onClose) {
        Surface {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clickable {
                            highlightTags = if (highlighted)
                                highlightTags.filter { !tag.match(it) }
                            else
                                highlightTags + tag.toString()
                            onClose()
                        }
                        .padding(16.dp)
                ) {
                    if (highlighted) {
                        Icon(Icons.Default.StarBorder, contentDescription = "unhighlight")
                        Text("Un-highlight tag")
                    } else {
                        Icon(Icons.Default.Star, contentDescription = "highlight")
                        Text("Highlight tag")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clickable {
                            mutedTags = if (muted)
                                mutedTags.filter { !tag.match(it) }
                            else
                                mutedTags + tag.toString()
                            onClose()
                        }
                        .padding(16.dp)
                ) {
                    if (muted) {
                        Icon(Icons.Default.Visibility, contentDescription = "unmute")
                        Text("Unmute tag")
                    } else {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "mute")
                        Text("Mute tag")
                    }
                }
            }
        }
    }
}