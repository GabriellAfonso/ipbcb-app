package com.ipb.castelobranco.features.gallery.domain.tags

import com.ipb.castelobranco.features.gallery.domain.model.GalleryMember
import com.ipb.castelobranco.features.gallery.domain.model.GalleryPhoto
import com.ipb.castelobranco.features.gallery.domain.model.GalleryTree

/** A person tagged in at least one photo on the device, with how many. */
data class TaggedPerson(val id: Long, val name: String, val photoCount: Int)

/**
 * Who is in the gallery, derived from the local copy only: the filter works offline and always agrees
 * with the photos on the device. Photos the tree cannot reach are left out, as everywhere else.
 */
object GalleryPeople {

    /** Every person tagged in a reachable photo, with the photo count; by name (accents ignored), then id. */
    fun taggedPeople(tree: GalleryTree): List<TaggedPerson> {
        val counts = linkedMapOf<Long, Int>()
        val names = mutableMapOf<Long, String>()
        tree.allPhotosInTreeOrder().forEach { photo ->
            photo.members.distinctBy { it.id }.forEach { member ->
                counts[member.id] = (counts[member.id] ?: 0) + 1
                names[member.id] = member.name
            }
        }
        return counts.map { (id, count) -> TaggedPerson(id, names.getValue(id), count) }
            .sortedWith(compareBy({ NameSearch.normalize(it.name) }, { it.name }, { it.id }))
    }

    /** The photos in which every one of [memberIds] is tagged, in tree order; none for an empty set. */
    fun photosWithAll(tree: GalleryTree, memberIds: Set<Long>): List<GalleryPhoto> {
        if (memberIds.isEmpty()) return emptyList()
        return tree.allPhotosInTreeOrder().filter { photo ->
            val tagged = photo.members.mapTo(HashSet()) { it.id }
            tagged.containsAll(memberIds)
        }
    }

    /** The distinct people of [photos], by name (accents ignored), then id. */
    fun peopleIn(photos: Collection<GalleryPhoto>): List<GalleryMember> =
        photos.flatMap { it.members }
            .distinctBy { it.id }
            .sortedWith(compareBy({ NameSearch.normalize(it.name) }, { it.name }, { it.id }))
}
