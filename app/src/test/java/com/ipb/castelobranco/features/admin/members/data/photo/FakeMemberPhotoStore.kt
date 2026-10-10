package com.ipb.castelobranco.features.admin.members.data.photo

import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoRevisions
import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoStore
import kotlinx.coroutines.flow.MutableStateFlow

/** Records what the roll asked of the photo cache; [revisions] can be driven by the test. */
class FakeMemberPhotoStore : MemberPhotoStore, MemberPhotoRevisions {
    val removed = mutableListOf<String>()
    val retained = mutableListOf<Set<String>>()
    var wipes = 0
        private set

    override val revisions = MutableStateFlow<Map<String, Int>>(emptyMap())

    override suspend fun remove(url: String) {
        removed += url
    }

    override suspend fun retainOnly(urls: Set<String>) {
        retained += urls
    }

    override suspend fun wipe() {
        wipes++
    }
}
