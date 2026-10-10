package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoExporter
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.util.DateProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MemberPhotoFileNameTest {

    private val day = LocalDate.of(2026, 10, 9)

    @Test
    fun `name and date`() {
        assertEquals("Maria Souza 2026-10-09", MemberPhotoFileName.build("Maria Souza", 7, day))
    }

    @Test
    fun `characters a file name cannot hold are dropped`() {
        assertEquals("AnaPaula 2026-10-09", MemberPhotoFileName.build(" Ana/Paula?: \t", 7, day))
        assertEquals("José da Silva 2026-10-09", MemberPhotoFileName.build("José \"da\" Silva", 7, day))
    }

    @Test
    fun `blank name falls back to the member id`() {
        assertEquals("Membro 7 2026-10-09", MemberPhotoFileName.build("  <>|  ", 7, day))
    }

    @Test
    fun `very long names are cut`() {
        val built = MemberPhotoFileName.build("A".repeat(300), 7, day)
        assertTrue(built.length <= 100 + " 2026-10-09".length)
    }

    @Test
    fun `download passes the built name and the url to the exporter`() = runTest {
        val saved = mutableListOf<Pair<String, String>>()
        val exporter = object : MemberPhotoExporter {
            override suspend fun save(fileBaseName: String, url: String): Result<Unit> {
                saved += fileBaseName to url
                return Result.success(Unit)
            }
        }

        DownloadMemberPhotoUseCase(exporter, DateProvider { day })(7, "Maria Souza", "u")

        assertEquals(listOf("Maria Souza 2026-10-09" to "u"), saved)
    }

    @Test
    fun `download failure is passed through`() = runTest {
        val exporter = object : MemberPhotoExporter {
            override suspend fun save(fileBaseName: String, url: String): Result<Unit> =
                Result.failure(AppError.Network())
        }

        val result = DownloadMemberPhotoUseCase(exporter, DateProvider { day })(7, "Maria", "u")

        assertTrue(result.exceptionOrNull() is AppError.Network)
    }
}
