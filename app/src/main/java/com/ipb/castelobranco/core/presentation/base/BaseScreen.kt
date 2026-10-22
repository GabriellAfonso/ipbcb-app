package com.ipb.castelobranco.core.presentation.base

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.data.local.StorageDirConstants
import com.ipb.castelobranco.core.presentation.components.TopBar
import com.ipb.castelobranco.core.presentation.navigation.LocalAppNavigator
import com.ipb.castelobranco.features.auth.data.local.AuthSession
import com.ipb.castelobranco.features.profile.data.local.ProfilePhotoBus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthSessionEntryPoint {
    fun authSession(): AuthSession
}

private class TopBarProfileViewModel(
    private val authSession: AuthSession
) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _profilePhotoVersion = MutableStateFlow(0)
    val profilePhotoVersion: StateFlow<Int> = _profilePhotoVersion.asStateFlow()

    init {
        viewModelScope.launch {
            authSession.isLoggedInFlow.collect { logged ->
                _isLoggedIn.value = logged
                bumpPhotoVersion()
            }
        }
    }

    fun bumpPhotoVersion() {
        _profilePhotoVersion.value += 1
    }
}

@Composable
fun BaseScreen(
    tabName: String,
    @DrawableRes logoRes: Int = R.drawable.ic_sarca_ipb,
    showBackArrow: Boolean = false,
    onMenuClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onAccountClick: (() -> Unit)? = null,
    showAccountAction: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.background,
    extraActions: @Composable () -> Unit = {},
    topBarExtension: @Composable (() -> Unit)? = null,
    content: @Composable (innerPadding: PaddingValues) -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findActivity() as? ComponentActivity
        ?: error("ComponentActivity não encontrada")

    val entryPoint = remember(context.applicationContext) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AuthSessionEntryPoint::class.java
        )
    }

    val topBarVm: TopBarProfileViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TopBarProfileViewModel(entryPoint.authSession()) as T
            }
        }
    )

    val isLoggedIn by topBarVm.isLoggedIn.collectAsStateWithLifecycle()
    val profilePhotoVersion by topBarVm.profilePhotoVersion.collectAsStateWithLifecycle()
    val globalPhotoVersion by ProfilePhotoBus.version.collectAsStateWithLifecycle()

    LaunchedEffect(globalPhotoVersion) {
        if (isLoggedIn) topBarVm.bumpPhotoVersion()
    }

    // Resolvemos apenas o arquivo. O Coil fará o decode em background.
    val photoFile: File? = remember(isLoggedIn, profilePhotoVersion) {
        if (!isLoggedIn) return@remember null
        val dir = File(context.filesDir, StorageDirConstants.PROFILE)
        if (!dir.exists()) return@remember null

        dir.listFiles()
            ?.asSequence()
            // Apenas metadados rápidos, sem decodificar pixels aqui
            ?.filter { it.isFile && it.name.startsWith("profile_photo.") && it.length() > 0L }
            ?.maxByOrNull { it.lastModified() }
    }

    val logo: Painter = painterResource(id = logoRes)

    // O modelo que passamos para o Coil: Se não houver arquivo, usa o placeholder do drawable
    val accountImageModel: Any? = remember(showAccountAction, isLoggedIn, photoFile) {
        if (!showAccountAction) null
        else if (isLoggedIn && photoFile != null) photoFile
        else R.drawable.ic_profile_placeholder
    }

    val appNavigator = LocalAppNavigator.current
    val resolvedOnAccountClick: () -> Unit = onAccountClick ?: {
        if (isLoggedIn) appNavigator.navigateToProfile()
        else appNavigator.navigateToAuth()
    }

    Scaffold(
        containerColor = containerColor,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopBar(
                    tabName = tabName,
                    logo = logo,
                    accountImageModel = accountImageModel,
                    showBackArrow = showBackArrow,
                    onMenuClick = onMenuClick,
                    onBackClick = onBackClick,
                    onAccountClick = resolvedOnAccountClick,
                    extraActions = extraActions,
                )
                topBarExtension?.invoke()
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}