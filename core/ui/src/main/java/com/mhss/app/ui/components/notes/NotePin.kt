package com.mhss.app.ui.components.notes

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mhss.app.ui.R

/**
 * Vård-pins som kan läggas på en vårdtagare. [id] sparas i databasen, ändra den aldrig
 * efter release. Varje pin har egen ikon, färg och förklarande text.
 */
enum class NotePin(
    val id: String,
    @DrawableRes val iconRes: Int,
    val label: String,
    val color: Color
) {
    DANGER("ffara", R.drawable.ffara, "Fara", Color(0xFFF30000)),
    INFECTION("smittvirus", R.drawable.smittvirus, "Smittvirus", Color(0xFF66BB6A)),
    SUPERVISION("tillsyn", R.drawable.tillsyn, "Tillsyn", Color(0xFF26C6DA)),
    POSITION_CHANGE("legesendring", R.drawable.legesendring, "Lägesändring", Color(0xFFD31626)),
    DIAPER("bloybyte", R.drawable.bloybyte, "Byta blöja", Color(0xFFFFFFFF)),
    MEDICATION("pillmed", R.drawable.pillmed, "Mediciner", Color(0xFF42A5F5)),
    KATETERPASE("kateterpase", R.drawable.kateterpase, "Kateterpåse", Color(0xFF423FEC)),
    BED_RAIL("senggavel", R.drawable.senggavel, "Sänggavel", Color(0xFF90A4AE)),
    BED_ALARM("senglarm", R.drawable.senglarm, "Sänglarm", Color(0xFFC4852A)),
    SNACK("mellanmol", R.drawable.mellanmol, "Mellanmål", Color(0xFFFFCA28)),
    CAMERA("cameraovervakning", R.drawable.cameraovervakning, "Kameraövervakning", Color(0xFFAB47BC));



    companion object {
        fun fromId(id: String): NotePin? = entries.firstOrNull { it.id == id }

        /** Returnerar pins i enumens ordning, så visningen alltid blir konsekvent. */
        fun fromIds(ids: List<String>): List<NotePin> =
            entries.filter { it.id in ids }
    }
}

/** Visar valda pins i en rad (bryter rad vid behov). Visar inget om listan är tom. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotePinsRow(
    pinIds: List<String>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp
) {
    val pins = NotePin.fromIds(pinIds)
    if (pins.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        pins.forEach { pin ->
            Icon(
                painter = painterResource(pin.iconRes),
                contentDescription = pin.label,
                tint = pin.color,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
