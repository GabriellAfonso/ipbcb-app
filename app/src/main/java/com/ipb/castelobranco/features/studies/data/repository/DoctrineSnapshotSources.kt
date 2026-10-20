package com.ipb.castelobranco.features.studies.data.repository

import com.ipb.castelobranco.core.data.snapshot.RetrofitSnapshotFetcher
import com.ipb.castelobranco.core.data.snapshot.SnapshotCacheFactory
import com.ipb.castelobranco.core.domain.snapshot.SnapshotCache
import com.ipb.castelobranco.core.domain.snapshot.SnapshotFetcher
import com.ipb.castelobranco.features.studies.data.api.DoctrineApi
import com.ipb.castelobranco.features.studies.data.dto.DoctrineDocumentDto
import com.ipb.castelobranco.features.studies.data.dto.DoctrineListDto
import javax.inject.Inject

/**
 * Where each doctrine snapshot is stored and fetched. Document slugs come from the server list,
 * so caches and fetchers are built per slug instead of being fixed in a Hilt map.
 */
interface DoctrineSnapshotSources {
    fun listCache(): SnapshotCache<DoctrineListDto>
    fun listFetcher(): SnapshotFetcher<DoctrineListDto>
    fun documentCache(slug: String): SnapshotCache<DoctrineDocumentDto>
    fun documentFetcher(slug: String): SnapshotFetcher<DoctrineDocumentDto>
}

class DefaultDoctrineSnapshotSources @Inject constructor(
    private val factory: SnapshotCacheFactory,
    private val api: DoctrineApi,
) : DoctrineSnapshotSources {

    override fun listCache(): SnapshotCache<DoctrineListDto> =
        factory.create(key = LIST_KEY, serializer = DoctrineListDto.serializer())

    override fun listFetcher(): SnapshotFetcher<DoctrineListDto> =
        RetrofitSnapshotFetcher { etag -> api.getDocuments(etag) }

    override fun documentCache(slug: String): SnapshotCache<DoctrineDocumentDto> =
        factory.create(key = documentKey(slug), serializer = DoctrineDocumentDto.serializer())

    override fun documentFetcher(slug: String): SnapshotFetcher<DoctrineDocumentDto> =
        RetrofitSnapshotFetcher { etag -> api.getDocument(slug, etag) }

    companion object {
        const val LIST_KEY = "doctrine_list"
        private const val DOCUMENT_KEY_PREFIX = "doctrine_"

        fun documentKey(slug: String): String = DOCUMENT_KEY_PREFIX + slug
    }
}
