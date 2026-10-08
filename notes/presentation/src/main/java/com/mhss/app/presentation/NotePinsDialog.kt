package com.mhss.app.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mhss.app.ui.R
import com.mhss.app.ui.components.notes.NotePin
import com.mhss.app.ui.theme.Orange

/**
 * Dialog med alla tillgängliga pins. Överst finns den gamla röda "viktig"-pinnen,
 * därefter alla vård-pins med ikon, förklarande text och kryssruta.
 */
@Composable
fun NotePinsDialog(
    selectedPins: List<String>,
    important: Boolean,
    onToggleImportant: () -> Unit,
    onTogglePin: (NotePin) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        shape = RoundedCornerShape(25.dp),
        onDismissRequest = onDismiss,
        title = { Text("Välj pins") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                PinRow(
                    iconPainter = painterResource(R.drawable.ic_pin_filled),
                    tint = Orange,
                    label = "Viktig",
                    checked = important,
                    onClick = onToggleImportant
                )
                NotePin.entries.forEach { pin ->
                    PinRow(
                        iconPainter = painterResource(pin.iconRes),
                        tint = pin.color,
                        label = pin.label,
                        checked = pin.id in selectedPins,
                        onClick = { onTogglePin(pin) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Klar") }
        }
    )
}

@Composable
private fun PinRow(
    iconPainter: androidx.compose.ui.graphics.painter.Painter,
    tint: androidx.compose.ui.graphics.Color,
    label: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = iconPainter,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Checkbox(checked = checked, onCheckedChange = { onClick() })
    }
}
