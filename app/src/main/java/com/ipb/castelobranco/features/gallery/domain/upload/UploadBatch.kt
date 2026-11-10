package com.ipb.castelobranco.features.gallery.domain.upload

/** What the person chose for one item of a batch. */
sealed interface VideoDecision {
    data object Undecided : VideoDecision

    /** Goes as it was picked. Photos and videos that cannot open the editor start here. */
    data object Whole : VideoDecision
    data class Cut(val entry: UploadEntry.Cut, val sizeBytes: Long) : VideoDecision
}

data class BatchItem(val media: PickedMedia, val decision: VideoDecision)

/** The videos the mobile-data question counts. */
data class MobileDataLoad(val count: Int, val bytes: Long)

/**
 * A picked batch held until its last video is decided, so the album keeps the picked order and one back
 * press can discard it without leaving part of it in the queue.
 */
data class UploadBatch(val albumId: Long, val items: List<BatchItem>) {
    val currentIndex: Int get() = items.indexOfFirst { it.decision == VideoDecision.Undecided }
    val current: BatchItem? get() = items.getOrNull(currentIndex)
    val isComplete: Boolean get() = currentIndex < 0
    val undecidedCount: Int get() = items.count { it.decision == VideoDecision.Undecided }

    /** How many videos the editor goes through. */
    val editableCount: Int get() = items.count { it.media.trimmable != null }

    /** The current video's place among them, from 1. */
    val position: Int get() = items.take(currentIndex + 1).count { it.media.trimmable != null }

    val cutFileNames: List<String>
        get() = items.mapNotNull { (it.decision as? VideoDecision.Cut)?.entry?.fileName }

    /** In picked order. */
    val entries: List<UploadEntry>
        get() = items.map { item ->
            (item.decision as? VideoDecision.Cut)?.entry ?: UploadEntry.Picked(item.media.source)
        }

    /** Real sizes: a cut counts what it became. A too-big whole video is not counted, it will be refused. */
    val mobileDataLoad: MobileDataLoad
        get() {
            val sizes = items.mapNotNull { item ->
                when (val decision = item.decision) {
                    is VideoDecision.Cut -> decision.sizeBytes
                    else -> (item.media.sizeBytes ?: 0L).takeIf {
                        item.media.isVideo && it <= VideoUploadCheck.MAX_BYTES
                    }
                }
            }
            return MobileDataLoad(sizes.size, sizes.sum())
        }

    fun decided(index: Int, decision: VideoDecision): UploadBatch =
        copy(items = items.mapIndexed { i, item -> if (i == index) item.copy(decision = decision) else item })

    fun allWhole(): UploadBatch = copy(
        items = items.map { item ->
            if (item.decision == VideoDecision.Undecided) item.copy(decision = VideoDecision.Whole) else item
        },
    )

    companion object {
        fun of(albumId: Long, picked: List<PickedMedia>): UploadBatch = UploadBatch(
            albumId,
            picked.map { media ->
                BatchItem(media, if (media.trimmable != null) VideoDecision.Undecided else VideoDecision.Whole)
            },
        )
    }
}
