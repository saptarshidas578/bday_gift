package com.pixelgift.app.ui.screens.studio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.pixelgift.app.R
import com.pixelgift.app.ui.theme.PixelGiftSpacing

@Composable
fun RgbHexInputSection(
    hexInput: String,
    rInput: String,
    gInput: String,
    bInput: String,
    onHexChanged: (String) -> Unit,
    onRgbChanged: (r: String, g: String, b: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hexCd = stringResource(R.string.cd_hex_input)
    val rCd = stringResource(R.string.cd_r_input)
    val gCd = stringResource(R.string.cd_g_input)
    val bCd = stringResource(R.string.cd_b_input)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PixelGiftSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(PixelGiftSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = hexInput,
                onValueChange = onHexChanged,
                label = { Text(stringResource(R.string.studio_hex_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .semantics { contentDescription = hexCd }
            )

            OutlinedTextField(
                value = rInput,
                onValueChange = { onRgbChanged(it, gInput, bInput) },
                label = { Text(stringResource(R.string.studio_r_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = rCd }
            )

            OutlinedTextField(
                value = gInput,
                onValueChange = { onRgbChanged(rInput, it, bInput) },
                label = { Text(stringResource(R.string.studio_g_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = gCd }
            )

            OutlinedTextField(
                value = bInput,
                onValueChange = { onRgbChanged(rInput, gInput, it) },
                label = { Text(stringResource(R.string.studio_b_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = bCd }
            )
        }
    }
}

