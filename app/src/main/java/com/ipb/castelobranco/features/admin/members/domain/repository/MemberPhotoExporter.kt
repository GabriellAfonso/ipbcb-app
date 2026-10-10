package com.ipb.castelobranco.features.admin.members.domain.repository

/**
 * Saves a member photo, unencrypted, to the device's shared pictures ("IPB Castelo Branco" folder).
 * Either the whole file is saved or nothing is: a failure leaves no partial file behind.
 */
interface MemberPhotoExporter {

    /** @param fileBaseName the file name without extension; the extension follows the photo's format. */
    suspend fun save(fileBaseName: String, url: String): Result<Unit>
}
