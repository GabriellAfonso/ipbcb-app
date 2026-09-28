package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.features.admin.members.data.mapper.toDomain
import com.ipb.castelobranco.features.admin.members.historyDto
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildHistorySentenceUseCaseTest {

    private val build = BuildHistorySentenceUseCase()

    private fun text(field: String, old: String? = null, new: String? = null) =
        build(historyDto(1, field, old, new).toDomain()).text

    @Test
    fun `creation and photo entries`() {
        assertEquals("cadastrou o membro", text("created"))
        assertEquals("trocou a foto", text("photo", new = "photo changed"))
        assertEquals("removeu a foto", text("photo", new = "photo removed"))
    }

    @Test
    fun `field entries name the field in portuguese`() {
        assertEquals("alterou Situação de Visitante para Ativo", text("status", "Visitante", "Ativo"))
        assertEquals(
            "alterou Ministérios de Louvor, Recepção para Louvor",
            text("ministries", "Louvor, Recepção", "Louvor"),
        )
    }

    @Test
    fun `raw codes and dates are translated`() {
        assertEquals("alterou Nascimento de vazio para 02/04/1990", text("birth_date", null, "1990-04-02"))
        assertEquals(
            "alterou Nascimento de 02/04/1990 para 02/04",
            text("birth_date", "1990-04-02", "0001-04-02"),
        )
        assertEquals("alterou Sexo de Masculino para Feminino", text("gender", "M", "F"))
        assertEquals("alterou Perfil de Válido para Inválido", text("is_active", "true", "false"))
        assertEquals("alterou Cargo de Diácono para vazio", text("role", "Diácono", null))
    }

    @Test
    fun `unknown field still reads as a sentence`() {
        assertEquals("alterou nickname de A para B", text("nickname", "A", "B"))
    }

    @Test
    fun `entry by a deleted account names an unknown editor`() {
        val line = build(historyDto(1, "created", editor = null).toDomain())

        assertEquals("Usuário removido", line.editorName)
    }
}
