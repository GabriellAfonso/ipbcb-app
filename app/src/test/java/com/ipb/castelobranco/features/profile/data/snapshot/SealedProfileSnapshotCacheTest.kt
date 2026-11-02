package com.ipb.castelobranco.features.profile.data.snapshot

import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.local.RecoveryEmailVault
import com.ipb.castelobranco.features.profile.domain.model.RecoveryEmailStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SealedProfileSnapshotCacheTest {

    private class FakeInner : SnapshotCache<MeProfileDto> {
        var stored: MeProfileDto? = null
        var etag: String? = null

        override suspend fun load() = stored
        override suspend fun save(dto: MeProfileDto, etag: String?) {
            stored = dto
            this.etag = etag
        }
        override suspend fun loadETag() = etag
        override suspend fun clear() {
            stored = null
            etag = null
        }
    }

    private class FakeVault : RecoveryEmailVault {
        var value: String? = null

        override suspend fun load() = value
        override suspend fun save(address: String) {
            value = address
        }
        override suspend fun clear() {
            value = null
        }
    }

    private val inner = FakeInner()
    private val vault = FakeVault()
    private val cache = SealedProfileSnapshotCache(inner, vault)

    private val withAddress = MeProfileDto(
        name = "Ana",
        recoveryEmail = "ana@example.com",
        recoveryEmailConfirmed = false,
    )

    @Test
    fun `the address never reaches the plain snapshot`() = runTest {
        cache.save(withAddress, "etag")

        assertNull(inner.stored?.recoveryEmail)
        assertEquals("ana@example.com", vault.value)
    }

    @Test
    fun `load puts the address back`() = runTest {
        cache.save(withAddress, "etag")

        assertEquals(withAddress, cache.load())
        assertEquals("etag", cache.loadETag())
    }

    @Test
    fun `a known absence stays known`() = runTest {
        cache.save(withAddress.copy(recoveryEmail = null), "etag")

        assertEquals(RecoveryEmailStatus.None, cache.load()?.toRecoveryEmailStatus())
        assertEquals("etag", cache.loadETag())
    }

    @Test
    fun `a profile cached before this feature is unknown, not absent, and asks for a full download`() = runTest {
        inner.save(MeProfileDto(name = "Ana"), "old")

        assertEquals(RecoveryEmailStatus.Unknown, cache.load()?.toRecoveryEmailStatus())
        assertNull(cache.loadETag())
    }

    @Test
    fun `a wiped vault makes the address unknown and asks for a full download`() = runTest {
        cache.save(withAddress, "etag")
        vault.clear()

        assertEquals(RecoveryEmailStatus.Unknown, cache.load()?.toRecoveryEmailStatus())
        assertNull(cache.loadETag())
    }

    @Test
    fun `clear erases both`() = runTest {
        cache.save(withAddress, "etag")

        cache.clear()

        assertNull(vault.value)
        assertNull(inner.stored)
        assertFalse(cache.exists())
    }

    @Test
    fun `status mapping`() {
        assertEquals(RecoveryEmailStatus.Unknown, MeProfileDto(name = "A").toRecoveryEmailStatus())
        assertEquals(
            RecoveryEmailStatus.None,
            MeProfileDto(name = "A", recoveryEmail = " ", recoveryEmailConfirmed = false).toRecoveryEmailStatus(),
        )
        assertEquals(
            RecoveryEmailStatus.Set("a@b.com", confirmed = true),
            MeProfileDto(name = "A", recoveryEmail = "a@b.com", recoveryEmailConfirmed = true).toRecoveryEmailStatus(),
        )
    }
}
