package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.domain.model.Birthday
import java.util.Locale

/** One birthday line: day badge + name. Shared by the home highlight and the year screen. */
@Composable
fun BirthdayEntry(
    birthday: Birthday,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 26.dp,
    badgeRadius: Dp = 6.dp,
    badgeTextStyle: TextStyle = MaterialTheme.typography.labelMedium,
    nameStyle: TextStyle = MaterialTheme.typography.bodySmall,
    highlighted: Boolean = false,
) {
    val badgeColor = if (highlighted) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primaryContainer
    val badgeTextColor = if (highlighted) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onPrimaryContainer

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(badgeSize)
                .clip(RoundedCornerShape(badgeRadius))
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.getDefault(), "%02d", birthday.day),
                style = badgeTextStyle,
                fontWeight = FontWeight.Bold,
                color = badgeTextColor,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = birthday.name,
            style = nameStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BirthdayEntryPreview() {
    Column(modifier = Modifier.padding(8.dp)) {
        BirthdayEntry(birthday = Birthday(name = "Ana Silva", month = 3, day = 5))
        BirthdayEntry(
            birthday = Birthday(name = "Gabriela Martins de Albuquerque", month = 3, day = 12),
            highlighted = true,
        )
    }
}
