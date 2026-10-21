package com.ipb.castelobranco.features.profile.presentation.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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

        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("Trocar foto") },
                onClick = {
                    menuOpen = false
                    onChangePhoto()
                },
            )
            if (hasPhoto) {
                DropdownMenuItem(
                    text = { Text("Remover foto", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        onRemovePhoto()
                    },
                )
            }
        }
    }
}

/** Name (editable), login name and how long the person has used the app; unknown lines are hidden. */
@Composable
fun AccountSection(
    userName: String,
    username: String?,
    joinedSince: String?,
    onEditName: () -> Unit,
) {
    SectionTitle("Conta")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AccountLine(label = "Nome", value = userName, modifier = Modifier.weight(1f))
        TextButton(onClick = onEditName) { Text("Editar") }
    }
    if (username != null) {
        AccountLine(label = "Login", value = username)
    }
    if (joinedSince != null) {
        Text(
            text = "No app desde $joinedSince",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

/** "Trocar senha" only for an account with a password; "Excluir conta" waits until that is known. */
@Composable
fun AccountActionsSection(
    hasPassword: Boolean?,
    canDeleteAccount: Boolean,
    onChangePassword: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    SectionTitle("Ações")
    if (hasPassword == true) {
        OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
            Text("Trocar senha")
        }
    }
    TextButton(
        onClick = onDeleteAccount,
        enabled = canDeleteAccount,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Excluir conta", color = if (canDeleteAccount) MaterialTheme.colorScheme.error else Color.Unspecified)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp)) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun AccountLine(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private val PHOTO_SIZE = 140.dp
