package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.ipbGreen

private val ButtonCornerRadius = 16.dp
private val ButtonContentPadding = 8.dp
private const val ImageSizeRatio = 0.6f
private val TextSpacing = 4.dp

@Composable
fun CustomButton(
    onClick: () -> Unit,
    image: Painter,
    text: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    backgroundColor: Color = ipbGreen,
    playSoundOnClick: Boolean = true,
) {
    val view = LocalView.current
    Button(
        onClick = {
            if (playSoundOnClick) view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
            onClick()
        },
        modifier = modifier.size(size),
        shape = RoundedCornerShape(ButtonCornerRadius),
        contentPadding = PaddingValues(ButtonContentPadding),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = image,
                contentDescription = text,
                modifier = Modifier.size(size * ImageSizeRatio)
            )

            if (text.isNotBlank()) {
                Spacer(modifier = Modifier.height(TextSpacing))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
