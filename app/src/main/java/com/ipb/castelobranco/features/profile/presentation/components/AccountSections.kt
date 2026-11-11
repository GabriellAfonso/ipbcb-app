package com.ipb.castelobranco.features.profile.presentation.components

import com.ipb.castelobranco.core.presentation.components.AppMenu
import com.ipb.castelobranco.core.presentation.components.AppMenuActions
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.HideImage
import com.ipb.castelobranco.core.presentation.components.MenuAction
import com.ipb.castelobranco.core.presentation.components.OverflowMenu
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.R

/** The round photo; a tap opens "Trocar foto" and, when there is a photo, "Remover foto". */
@Composable
fun ProfilePhotoSection(
    bitmap: Bitmap?,
    hasPhoto: Boolean,
    isBusy: Boolean,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .size(PHOTO_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !isBusy) { menuOpen = true },
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_profile_placeholder),
                    contentDescription = "Selecionar foto do perfil",
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isBusy) {
                CircularProgressIndicator()
            }
        }

        AppMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            AppMenuActions(
                actions = listOfNotNull(
                    MenuAction(Icons.Filled.PhotoCamera, "Trocar foto", onChangePhoto),
                    MenuAction(Icons.Filled.HideImage, "Remover foto", onRemovePhoto, isDanger = true)
                        .takeIf { hasPhoto },
                ),
                onDismiss = { menuOpen = false },
            )
        }
    }
}

/**
 * Pinned to the bottom: "Sair", "Excluir conta" (waits until the password state is known) and "No app desde".
 */
@Composable
fun AccountFooter(
    canDeleteAccount: Boolean,
    joinedSince: String?,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedButton(onClick = onLogout) {
            Icon(painter = painterResource(R.drawable.ic_logout), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sair")
        }
        TextButton(onClick = onDeleteAccount, enabled = canDeleteAccount) {
            Text("Excluir conta", color = if (canDeleteAccount) MaterialTheme.colorScheme.error else Color.Unspecified)
        }
        if (joinedSince != null) {
            Text(
                text = "No app desde $joinedSince",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Top bar three-dot menu: "Editar nome" always, "Trocar senha" only for an account with a password. */
@Composable
fun ProfileOverflowMenu(
    hasPassword: Boolean,
    onEditName: () -> Unit,
    onChangePassword: () -> Unit,
) {
    OverflowMenu(
        listOfNotNull(
            MenuAction(Icons.Filled.Edit, "Editar nome", onEditName),
            MenuAction(Icons.Filled.Lock, "Trocar senha", onChangePassword).takeIf { hasPassword },
        ),
    )
}

private val PHOTO_SIZE = 140.dp
