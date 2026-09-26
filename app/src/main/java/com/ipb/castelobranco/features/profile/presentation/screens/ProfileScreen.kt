// app/src/main/java/com/gabrielafonso/ipb/castelobranco/ui/screens/profile/ProfileView.kt
package com.ipb.castelobranco.features.profile.presentation.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.rememberSquarePhotoPicker
import com.ipb.castelobranco.features.profile.presentation.viewmodel.ProfileViewModel
import java.io.File

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {

    LaunchedEffect(Unit) { viewModel.initialize() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isUploading = uiState.isUploading
    val error = uiState.error
    val localPhotoPath = uiState.localPhotoPath
    val localPhotoVersion = uiState.localPhotoVersion
    val userName = uiState.userName
    val memberActive = uiState.isMember

    val pickPhoto = rememberSquarePhotoPicker(
        onPicked = { bytes -> viewModel.uploadProfilePhoto(bytes, "profile.jpg") },
        onCancelled = viewModel::clearError,
    )

    BaseScreen(
        tabName = "Perfil",
        logoRes = R.drawable.ic_profile,
        showBackArrow = true,
        onBackClick = onBackClick,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            val photoFile: File? = localPhotoPath
                ?.takeIf { it.isNotBlank() }
                ?.let { File(it) }
                ?.takeIf { it.exists() && it.length() > 0L }

            val bitmap = remember(localPhotoVersion, photoFile?.absolutePath) {
                photoFile?.absolutePath?.let { BitmapFactory.decodeFile(it) }
            }

            Spacer(Modifier.height(70.dp))

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = !isUploading) { pickPhoto() },
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

                if (isUploading) {
                    CircularProgressIndicator()
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = userName?.takeIf { it.isNotBlank() } ?: "Usuário",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(30.dp))

            val statusText = when (memberActive) {
                true -> "Membro: Ativo"
                false -> "Membro: Desativado"
                null -> "Membro: --"
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = when (memberActive) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Erro: ${error.orEmpty()}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}