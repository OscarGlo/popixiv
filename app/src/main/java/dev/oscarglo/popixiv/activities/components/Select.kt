package dev.oscarglo.popixiv.activities.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> Select(
    values: List<T>,
    value: T,
    modifier: Modifier = Modifier,
    onChange: (T) -> Unit = {},
    render: @Composable (T) -> Unit = { Text(it.toString()) }
) {

    var currentValue by rememberSaveable { mutableStateOf(value) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded,
        { expanded = it },
        modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            render(value)
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            values.map {
                DropdownMenuItem(onClick = {
                    currentValue = it
                    onChange(it)
                    expanded = false
                }) {
                    render(it)
                }
            }
        }
    }
}