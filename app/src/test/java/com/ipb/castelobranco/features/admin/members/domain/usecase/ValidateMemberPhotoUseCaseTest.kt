package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberPhotoUseCase.Companion.INVALID_FORMAT
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberPhotoUseCase.Companion.MAX_BYTES
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberPhotoUseCase.Companion.TOO_LARGE
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateMemberPhotoUseCaseTest {

    private val validate = ValidateMemberPhotoUseCase()

    private fun bytes(vararg head: Int, size: Int = 32) =
        ByteArray(size).also { array -> head.forEachIndexed { i, b -> array[i] = b.toByte() } }

    @Test
    fun `each accepted format is recognised by its signature`() {
        assertEquals("image/jpeg", validate(bytes(0xFF, 0xD8, 0xFF)).getOrThrow())
        assertEquals("image/png", validate(bytes(0x89, 0x50, 0x4E, 0x47)).getOrThrow())
        assertEquals("image/gif", validate(bytes(0x47, 0x49, 0x46, 0x38)).getOrThrow())
        assertEquals(
            "image/webp",
            validate(bytes(0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50)).getOrThrow(),
        )
    }

    @Test
    fun `unknown or empty content is refused`() {
        assertEquals(INVALID_FORMAT, messageOf(validate(bytes(0x25, 0x50, 0x44, 0x46))))
        assertEquals(INVALID_FORMAT, messageOf(validate(ByteArray(0))))
    }

    @Test
    fun `files over 10 MB are refused`() {
        assertEquals(TOO_LARGE, messageOf(validate(bytes(0xFF, 0xD8, 0xFF, size = MAX_BYTES + 1))))
    }

    private fun messageOf(result: Result<String>): String? =
        (result.exceptionOrNull() as AppError).userMessage
}
