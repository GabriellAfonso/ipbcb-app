package com.ipb.castelobranco.features.admin.members.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * The device copy of member photos — encrypted, separate from member data, which never reaches
 * the disk (specs/012-encrypted-session-photo-cache, FR-008–FR-014). The roll uses it to drop
 * photos that no longer apply and to erase everything when access is lost.
 */
interface MemberPhotoStore {

    /** Drops the copy of [url] — the photo was replaced, removed or its member deleted. */
    suspend fun remove(url: String)

    /** Keeps only the photos still in the roll; the rest were removed by someone else. */
    suspend fun retainOnly(urls: Set<String>)

    /** Erases every photo and the key that opens them (sign-out, lost access). */
    suspend fun wipe()
}

/**
 * Bumps whenever the device copy of a photo changes or disappears, keyed by URL, so a screen
 * already showing it loads it again.
 */
interface MemberPhotoRevisions {
    val revisions: Flow<Map<String, Int>>
}
