package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.features.gallery.domain.manage.BatchResult
import com.ipb.castelobranco.features.gallery.domain.model.SubtreeCounts

/** Portuguese texts of the management confirmations and results. Pure, so they are tested as is. */
internal object GalleryManageTexts {

    const val NAME_EMPTY = "Informe um nome"
    const val NAME_TOO_LONG = "Máximo de 100 caracteres"
    const val DELETE_LABEL = "Apagar"
    const val REMOVE_LABEL = "Remover"
    private const val TRASH_ONE = "Fica 30 dias na lixeira."
    private const val TRASH_MANY = "Ficam 30 dias na lixeira."

    /** "Apagar 'Retiro' com 2 subálbuns e 41 fotos? Fica 30 dias na lixeira." — empty parts left out. */
    fun deleteAlbum(name: String, counts: SubtreeCounts): String {
        val parts = listOfNotNull(
            counts.subAlbums.takeIf { it > 0 }?.let { plural(it, "subálbum", "subálbuns") },
            counts.photos.takeIf { it > 0 }?.let { plural(it, "foto", "fotos") },
        )
        val contents = if (parts.isEmpty()) "" else " com ${parts.joinToString(" e ")}"
        return "Apagar '$name'$contents? $TRASH_ONE"
    }

    fun deletePhotos(count: Int): String =
        if (count == 1) "Apagar 1 foto? $TRASH_ONE" else "Apagar $count fotos? $TRASH_MANY"

    fun removeCover(albumName: String) = "Remover a capa de '$albumName'?"

    fun moved(result: BatchResult, albumName: String): String =
        batch(result, doneOne = "movida", doneMany = "movidas", suffix = " para '$albumName'")

    fun deleted(result: BatchResult): String = batch(result, doneOne = "apagada", doneMany = "apagadas", suffix = "")

    /**
     * "3 fotos movidas para 'Sábado'" when all went well; otherwise "2 de 3 fotos movidas. 1: <motivo>",
     * one clause per distinct reason.
     */
    private fun batch(result: BatchResult, doneOne: String, doneMany: String, suffix: String): String {
        if (result.failures.isEmpty()) {
            return if (result.succeeded == 1) "1 foto $doneOne$suffix" else "${result.succeeded} fotos $doneMany$suffix"
        }
        val reasons = result.failures
            .groupingBy { it.toAppError().toGalleryWriteError().message }
            .eachCount()
            .entries
            .joinToString(" ") { (reason, count) -> "$count: $reason" }
        return "${result.succeeded} de ${result.total} fotos $doneMany. $reasons"
    }

    private fun plural(count: Int, one: String, many: String) = if (count == 1) "1 $one" else "$count $many"
}
