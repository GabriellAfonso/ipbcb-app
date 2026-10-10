package com.ipb.castelobranco.features.admin.members.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import java.nio.ByteBuffer

/**
 * The member's photo, or their initials. The photo goes through the leader-only [imageLoader]
 * (encrypted device copy first); while it loads, and whenever it cannot be shown (refused, missing,
 * rate-limited, offline), the initials stand in — never a broken image or an error.
 *
 * @param pickedPhoto a photo picked but not uploaded yet; shown instead of [photoUrl].
 * @param photoRevision bumps when the device copy of [photoUrl] changed, so it loads again.
 */
@Composable
fun MemberAvatar(
    initials: String,
    photoUrl: String?,
    imageLoader: ImageLoader?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    initialsSize: TextUnit = 16.sp,
    pickedPhoto: ByteArray? = null,
    photoRevision: Int = 0,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        val initialsContent = @Composable { Initials(initials, initialsSize) }
        val context = LocalContext.current
        // Coil 2 reads raw bytes as a ByteBuffer. The revision is part of the memory key, so a photo
        // that changed on the device is not served from memory.
        val model: Any? = pickedPhoto?.let(ByteBuffer::wrap) ?: photoUrl?.let { url ->
            remember(url, photoRevision) {
                ImageRequest.Builder(context).data(url).memoryCacheKey("$url#$photoRevision").build()
            }
        }
        if (model == null || imageLoader == null) {
            initialsContent()
        } else {
            SubcomposeAsyncImage(
                model = model,
                imageLoader = imageLoader,
                contentDescription = "Foto do membro",
                contentScale = ContentScale.Crop,
                loading = { initialsContent() },
                error = { initialsContent() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Initials(initials: String, size: TextUnit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text(
            text = initials,
            fontSize = size,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MemberAvatarPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        MemberAvatar(initials = "AS", photoUrl = null, imageLoader = null, modifier = Modifier.size(64.dp))
    }
}
