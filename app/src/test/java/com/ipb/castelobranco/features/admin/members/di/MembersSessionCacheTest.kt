package com.ipb.castelobranco.features.admin.members.di

import com.ipb.castelobranco.features.admin.members.data.api.FakeMembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.photo.FakeMemberPhotoStore
import com.ipb.castelobranco.features.admin.members.data.repository.MembersAdminRepositoryImpl
import com.ipb.castelobranco.features.admin.members.data.dto.MemberListDto
import com.ipb.castelobranco.features.admin.members.ok
import com.ipb.castelobranco.features.admin.members.summaryDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MembersSessionCacheTest {

    @Test
    fun `ending the session clears the roll and wipes the photo cache`() = runTest {
        val api = FakeMembersAdminApi().apply { onGetMembers = { ok(MemberListDto(listOf(summaryDto(1, "Ana")))) } }
        val photos = FakeMemberPhotoStore()
        val repository = MembersAdminRepositoryImpl(api, photos)
        repository.refreshMembers().getOrThrow()

        membersSessionCache(repository, imageLoader = null, photoStore = photos).clear()

        assertNull(repository.observeMembers().first())
        assertEquals(1, photos.wipes)
    }
}
