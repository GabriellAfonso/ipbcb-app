package com.ipb.castelobranco.features.auth.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.components.isCompactHeight
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.auth.presentation.state.FieldHelp
import com.ipb.castelobranco.features.auth.presentation.state.HelpTone

// Shared by the login and register screens (`specs/auth/spec.md` §3 and §4).

private const val GOOGLE_LOADING_LABEL = "Conectando…"
private const val SUBTITLE_ALPHA = 0.82f
private const val WATERMARK_ALPHA = 0.07f

private val WatermarkWidth = 240.dp
private val WatermarkOffsetX = 60.dp
private val WatermarkOffsetY = 70.dp
private val SheetOverlap = 28.dp
private val SheetCorner = 28.dp
private val SheetHorizontalPadding = 20.dp
private val SheetTopPadding = 22.dp
private val ItemSpacing = 12.dp
private val FieldCorner = 16.dp
private val BannerCorner = 12.dp
private val SmallIconSize = 18.dp
private val HelpHorizontalPadding = 14.dp
private val LandscapeBandWidth = 300.dp
private val LandscapeLogoWidth = 56.dp
private val LandscapeFormMaxWidth = 440.dp

internal val AuthButtonHeight = 52.dp
internal val PillShape = RoundedCornerShape(percent = 50)

/** Band sizes: the login's, and the lower one of the register screen. */
internal enum class AuthBandSize(val topPadding: Dp, val bottomPadding: Dp, val logoWidth: Dp) {
    Login(topPadding = 48.dp, bottomPadding = 60.dp, logoWidth = 84.dp),
    Register(topPadding = 42.dp, bottomPadding = 58.dp, logoWidth = 58.dp),
}

/** Green band with the sheet climbing over it; [sheet] lays its items out with the shared spacing. */
@Composable
internal fun AuthLayout(
    title: String,
    subtitle: String,
    bandSize: AuthBandSize,
    onBackClick: () -> Unit,
    sheet: @Composable ColumnScope.() -> Unit,
) {
    if (isCompactHeight()) {
        LandscapeAuthLayout(title, subtitle, onBackClick, sheet)
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        AuthBand(title = title, subtitle = subtitle, size = bandSize, onBackClick = onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = -SheetOverlap)
                .clip(RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner))
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .padding(start = SheetHorizontalPadding, end = SheetHorizontalPadding, top = SheetTopPadding),
            verticalArrangement = Arrangement.spacedBy(ItemSpacing),
            content = sheet,
        )
    }
}

/**
 * Lying down (`specs/auth/spec.md`, "Celular deitado"): the band becomes a green column on the left and the form
 * scrolls on the right, at most [LandscapeFormMaxWidth] wide, so the first field shows without scrolling.
 */
@Composable
private fun LandscapeAuthLayout(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    sheet: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .width(LandscapeBandWidth)
                .fillMaxHeight()
                .background(ipbGreen)
                .clipToBounds()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start)),
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.statusBarsPadding()) {
                Icon(
                    painter = painterResource(id = R.drawable.back_arrow_icon),
                    contentDescription = "Voltar",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_sarca_ipb),
                    contentDescription = "Logo SARCA IPB",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.width(LandscapeLogoWidth),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = SUBTITLE_ALPHA),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical))
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = LandscapeFormMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(ItemSpacing),
                content = sheet,
            )
        }
    }
}

/** Green band behind the status bar: back button, the burning bush, title and subtitle, with a faint watermark. */
@Composable
private fun AuthBand(title: String, subtitle: String, size: AuthBandSize, onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ipbGreen)
            .clipToBounds(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_sarca_ipb),
            contentDescription = null,
            colorFilter = ColorFilter.tint(Color.White),
            alpha = WATERMARK_ALPHA,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = WatermarkOffsetX, y = WatermarkOffsetY)
                .width(WatermarkWidth),
        )

        IconButton(onClick = onBackClick, modifier = Modifier.statusBarsPadding()) {
            Icon(
                painter = painterResource(id = R.drawable.back_arrow_icon),
                contentDescription = "Voltar",
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = size.topPadding, bottom = size.bottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_sarca_ipb),
                contentDescription = "Logo SARCA IPB",
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.width(size.logoWidth),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = SUBTITLE_ALPHA),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun GoogleButton(label: String, isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = PillShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(AuthButtonHeight),
    ) {
        if (isLoading) {
            ButtonSpinner()
            Spacer(modifier = Modifier.width(8.dp))
            Text(GOOGLE_LOADING_LABEL)
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = null,
                modifier = Modifier.size(SmallIconSize),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label)
        }
    }
}

@Composable
internal fun ButtonSpinner() {
    CircularProgressIndicator(
        modifier = Modifier.size(SmallIconSize),
        strokeWidth = 2.dp,
        color = LocalContentColor.current,
    )
}

@Composable
internal fun OrDivider(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
internal fun AuthErrorBanner(message: String) {
    AuthBanner(
        message = message,
        icon = Icons.Filled.Error,
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.onErrorContainer,
    )
}

@Composable
internal fun AuthSuccessBanner(message: String) {
    AuthBanner(
        message = message,
        icon = Icons.Filled.CheckCircle,
        container = MaterialTheme.colorScheme.primaryContainer,
        content = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}

@Composable
private fun AuthBanner(message: String, icon: ImageVector, container: Color, content: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BannerCorner))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
        )
    }
}

/**
 * Filled field without the underline; the border only shows on focus or error. [help] is the line under it; an
 * error help also marks the field.
 */
@Composable
internal fun TonalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector?,
    isError: Boolean,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    help: FieldHelp? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(FieldCorner)
    val showError = isError || help?.isError == true
    val borderColor = when {
        showError -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }
    val container = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, borderColor, shape),
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
            trailingIcon = trailingIcon,
            isError = showError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                disabledContainerColor = container,
                errorContainerColor = container,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
            ),
        )
        help?.let { FieldHelpText(it) }
    }
}

@Composable
private fun FieldHelpText(help: FieldHelp) {
    Text(
        text = help.text,
        style = MaterialTheme.typography.bodySmall,
        color = when (help.tone) {
            HelpTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            HelpTone.Ok -> MaterialTheme.colorScheme.primary
            HelpTone.Error -> MaterialTheme.colorScheme.error
        },
        modifier = Modifier
            .padding(start = HelpHorizontalPadding, end = HelpHorizontalPadding, top = 4.dp)
            .semantics { if (help.isError) liveRegion = LiveRegionMode.Polite },
    )
}
