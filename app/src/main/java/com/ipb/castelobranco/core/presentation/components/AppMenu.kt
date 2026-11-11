package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme

// The app's options menu (`specs/core/spec.md` §6.7.2): every "⋮", "+" and pick list opens this card.

private const val MORE_LABEL = "Mais opções"

private val ItemShape = RoundedCornerShape(10.dp)
private val MenuInset = 6.dp
private val ItemIconSize = 21.dp

/** The card's look, also for menus [AppMenu] cannot wrap (`ExposedDropdownMenu` of a form field). */
object AppMenuDefaults {
    val shape = RoundedCornerShape(16.dp)
    val shadowElevation = 8.dp

    @Composable
    fun containerColor() = highlightCardColor()

    @Composable
    fun border() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
}

/** One option of a menu: the icon, the name, and red when it destroys something. */
data class MenuAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val isDanger: Boolean = false,
    val enabled: Boolean = true,
)

/**
 * The card that drops from its anchor: 16dp corners, a thin `outlineVariant` border, the card color of the theme
 * ([highlightCardColor]) and a soft shadow. Place it in the same `Box` as the button that opens it.
 */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.padding(horizontal = MenuInset),
        offset = offset,
        shape = AppMenuDefaults.shape,
        containerColor = AppMenuDefaults.containerColor(),
        tonalElevation = 0.dp,
        shadowElevation = AppMenuDefaults.shadowElevation,
        border = AppMenuDefaults.border(),
        content = content,
    )
}

/**
 * A row of [AppMenu]: rounded highlight, an optional icon ([icon], or any [leadingContent]) and the name. A danger
 * option is red, text and icon.
 */
@Composable
fun AppMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    isDanger: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val leading: (@Composable () -> Unit)? = leadingContent ?: icon?.let {
        { Icon(it, contentDescription = null, modifier = Modifier.size(ItemIconSize)) }
    }
    DropdownMenuItem(
        text = { Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium) },
        onClick = onClick,
        modifier = modifier.clip(ItemShape),
        leadingIcon = leading,
        enabled = enabled,
        colors = MenuDefaults.itemColors(
            textColor = if (isDanger) colors.error else colors.onSurface,
            leadingIconColor = if (isDanger) colors.error else colors.onSurfaceVariant,
        ),
    )
}

/** The line that sets a group apart, e.g. before the danger options. */
@Composable
fun AppMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** [actions] in order, the danger ones last after a divider; picking one closes the menu first. */
@Composable
fun AppMenuActions(actions: List<MenuAction>, onDismiss: () -> Unit) {
    val (danger, normal) = actions.partition { it.isDanger }
    normal.forEach { MenuActionItem(it, onDismiss) }
    if (danger.isNotEmpty()) {
        if (normal.isNotEmpty()) AppMenuDivider()
        danger.forEach { MenuActionItem(it, onDismiss) }
    }
}

@Composable
private fun MenuActionItem(action: MenuAction, onDismiss: () -> Unit) {
    AppMenuItem(
        text = action.label,
        icon = action.icon,
        isDanger = action.isDanger,
        enabled = action.enabled,
        onClick = {
            onDismiss()
            action.onClick()
        },
    )
}

/**
 * The three dots and their [AppMenu]. [controls] go on top and keep the menu open when used (the text size of
 * chord charts and lyrics), then a divider and [actions]. Nothing is shown when there is neither.
 */
@Composable
fun OverflowMenu(
    actions: List<MenuAction>,
    modifier: Modifier = Modifier,
    controls: (@Composable () -> Unit)? = null,
) {
    if (actions.isEmpty() && controls == null) return
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.MoreVert, contentDescription = MORE_LABEL) }
        AppMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            controls?.let {
                it()
                if (actions.isNotEmpty()) AppMenuDivider()
            }
            AppMenuActions(actions, onDismiss = { expanded = false })
        }
    }
}

@Preview(name = "Menu de opções")
@Composable
private fun AppMenuPreview() {
    IPBCasteloBrancoTheme {
        Surface {
            Box(Modifier.padding(8.dp)) {
                AppMenu(expanded = true, onDismissRequest = {}) {
                    AppMenuActions(
                        actions = listOf(
                            MenuAction(Icons.Filled.Edit, "Editar álbum", {}),
                            MenuAction(Icons.Filled.FolderOpen, "Mover álbum", {}),
                            MenuAction(Icons.Filled.Delete, "Apagar álbum", {}, isDanger = true),
                        ),
                        onDismiss = {},
                    )
                }
            }
        }
    }
}
