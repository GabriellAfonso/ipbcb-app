package com.ipb.castelobranco.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.ipb.castelobranco.core.data.security.AeadCipher
import com.ipb.castelobranco.core.data.security.EncryptedAuthPrefs
import com.ipb.castelobranco.core.data.security.KeystoreAeadCipher
import com.ipb.castelobranco.core.data.security.SessionRecovery
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthPrefs

/** The Keystore cipher that seals the session store. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionCipher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsPrefs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SetlistPrefs

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PushPrefs

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @SessionCipher
    fun provideSessionCipher(): AeadCipher = KeystoreAeadCipher(KeystoreAeadCipher.SESSION_KEY_ALIAS)

    /** The session store, encrypted with a Keystore key (specs/012-encrypted-session-photo-cache). */
    @Provides
    @Singleton
    @AuthPrefs
    fun provideAuthPreferencesDataStore(
        @ApplicationContext context: Context,
        @SessionCipher cipher: AeadCipher,
        recovery: SessionRecovery,
    ): DataStore<Preferences> = EncryptedAuthPrefs.create(context.filesDir, cipher, recovery)

    @Provides
    @Singleton
    @SettingsPrefs
    fun provideSettingsPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("settings_prefs") }
        )

    @Provides
    @Singleton
    @SetlistPrefs
    fun provideSetlistPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("setlist_prefs") }
        )

    @Provides
    @Singleton
    @PushPrefs
    fun providePushPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("push_prefs") }
        )
}
