package com.ipb.castelobranco.features.admin.members.di

import android.content.Context
import coil.ImageLoader
import com.ipb.castelobranco.core.data.security.AeadCipher
import com.ipb.castelobranco.core.data.security.KeystoreAeadCipher
import com.ipb.castelobranco.core.di.Client
import com.ipb.castelobranco.features.admin.members.data.photo.EncryptedMemberPhotoCache
import com.ipb.castelobranco.features.admin.members.data.photo.MemberPhotoFetcher
import com.ipb.castelobranco.features.admin.members.data.photo.MemberPhotoGallerySaver
import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoExporter
import com.ipb.castelobranco.features.admin.members.data.photo.MemberPhotoSource
import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoRevisions
import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Qualifier
import javax.inject.Singleton

/** The image loader for member photos — authenticated, with the encrypted disk cache. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MemberPhotoLoader

/** The Keystore cipher that seals member photos — a key of its own, apart from the session's. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MemberPhotoCipher

/**
 * Member photos open only with a leader's token. They may stay on the device only encrypted
 * (LGPD art. 11; specs/012-encrypted-session-photo-cache): Coil's own disk cache stays off, and
 * [MemberPhotoFetcher] reads them through [MemberPhotoSource] — the encrypted cache in
 * `no_backup/member_photos`, revalidated with the server. Coil's memory cache keeps a photo shown
 * in this session; both are erased on sign-out with the rest of the members data.
 */
@Module
@InstallIn(SingletonComponent::class)
object MemberPhotoLoaderModule {

    @Provides
    @Singleton
    @MemberPhotoCipher
    fun provideMemberPhotoCipher(): AeadCipher = KeystoreAeadCipher(KeystoreAeadCipher.MEMBER_PHOTOS_KEY_ALIAS)

    @Provides
    @Singleton
    fun provideMemberPhotoSource(
        @ApplicationContext context: Context,
        @MemberPhotoCipher cipher: AeadCipher,
        @Client client: OkHttpClient,
    ): MemberPhotoSource = MemberPhotoSource(
        cache = EncryptedMemberPhotoCache(File(context.noBackupFilesDir, EncryptedMemberPhotoCache.DIRECTORY), cipher),
        client = client,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    )

    @Provides
    fun provideMemberPhotoStore(source: MemberPhotoSource): MemberPhotoStore = source

    @Provides
    fun provideMemberPhotoRevisions(source: MemberPhotoSource): MemberPhotoRevisions = source

    @Provides
    fun provideMemberPhotoExporter(saver: MemberPhotoGallerySaver): MemberPhotoExporter = saver

    @Provides
    @Singleton
    @MemberPhotoLoader
    fun provideMemberPhotoLoader(
        @ApplicationContext context: Context,
        @Client client: OkHttpClient,
        source: MemberPhotoSource,
    ): ImageLoader = ImageLoader.Builder(context)
        .okHttpClient(client)
        .diskCache(null)
        .respectCacheHeaders(false)
        .components { add(MemberPhotoFetcher.Factory(source)) }
        .build()
}
