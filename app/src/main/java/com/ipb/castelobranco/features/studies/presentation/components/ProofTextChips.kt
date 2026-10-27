package com.ipb.castelobranco.features.studies.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.ProofText
import com.ipb.castelobranco.features.studies.domain.model.VerseSpan

/**
 * One chip per proof text, as printed. A chip with a [ProofText.reference] opens the Bible; one
 * without is plain text with an outline, and does nothing when tapped.
 */
@Composable
fun ProofTextChips(
    proofTexts: List<ProofText>,
    onReferenceClick: (BibleReference) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        proofTexts.forEach { proofText ->
            val reference = proofText.reference
            if (reference != null) {
                Surface(
                    onClick = { onReferenceClick(reference) },
                    shape = CHIP_SHAPE,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    ChipLabel(text = proofText.printed, bold = true)
                }
            } else {
                Surface(
                    shape = CHIP_SHAPE,
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    ChipLabel(text = proofText.printed, bold = false)
                }
            }
        }
    }
}

@Composable
private fun ChipLabel(text: String, bold: Boolean) {
    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 26.dp)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private val CHIP_SHAPE = RoundedCornerShape(13.dp)

@Preview(showBackground = true)
@Composable
private fun ProofTextChipsPreview() {
    MaterialTheme {
        ProofTextChips(
            proofTexts = listOf(
                ProofText("Rm 11.36", BibleReference("Rm", 11, listOf(VerseSpan(36, 36)))),
                ProofText("61.3", BibleReference("Is", 61, listOf(VerseSpan(3, 3)))),
                ProofText("Sl 86-8", null),
            ),
            onReferenceClick = {},
        )
    }
}
