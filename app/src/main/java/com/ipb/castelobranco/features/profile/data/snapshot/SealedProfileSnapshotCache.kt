package com.ipb.castelobranco.features.profile.data.snapshot

import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.local.RecoveryEmailVault

/**
 * Keeps the recovery address out of the plain profile snapshot (`specs/profile/spec.md` §2): it goes to the [vault] on
 * save and comes back on load. With no vault entry, or a profile cached before this field existed, the address is
 * unknown (`recoveryEmailConfirmed == null`) and no ETag is offered: a 304 would leave it unknown for good.
 */
class SealedProfileSnapshotCache(
    private val inner: SnapshotCache<MeProfileDto>,
    private val vault: RecoveryEmailVault,
) : SnapshotCache<MeProfileDto> {

    override suspend fun load(): MeProfileDto? {
        val cached = inner.load() ?: return null
        if (cached.recoveryEmailConfirmed == null) return cached
        val address = vault.load() ?: return cached.copy(recoveryEmailConfirmed = null)
        return cached.copy(recoveryEmail = address.ifEmpty { null })
    }

    override suspend fun exists(): Boolean = inner.exists()

    override suspend fun save(dto: MeProfileDto, etag: String?) {
        vault.save(dto.recoveryEmail.orEmpty())
        inner.save(dto.copy(recoveryEmail = null), etag)
    }

    override suspend fun loadETag(): String? =
        if (load()?.recoveryEmailConfirmed == null) null else inner.loadETag()

    override suspend fun clear() {
        inner.clear()
        vault.clear()
    }
}
