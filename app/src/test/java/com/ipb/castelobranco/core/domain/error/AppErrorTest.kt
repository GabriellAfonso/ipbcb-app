package com.ipb.castelobranco.core.domain.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AppErrorTest {

    // region toAppError()

    @Test
    fun `IOException maps to AppError Network preserving cause`() {
        val cause = IOException("timeout")

        val result = cause.toAppError()

        assertTrue(result is AppError.Network)
        assertSame(cause, result.cause)
        assertEquals("timeout", result.message)
    }

    @Test
    fun `AppError already mapped is returned unchanged`() {
        val original = AppError.Auth(message = "unauthorized")

        val result = original.toAppError()

        assertSame(original, result)
    }

    @Test
    fun `RuntimeException maps to AppError Unknown preserving cause`() {
        val cause = RuntimeException("boom")

        val result = cause.toAppError()

        assertTrue(result is AppError.Unknown)
        assertSame(cause, result.cause)
        assertEquals("boom", result.message)
    }

    // endregion

    // region mapError()

    @Test
    fun `mapError on success returns unchanged result`() {
        val success = Result.success(42)

        val result = success.mapError()

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `mapError on failure with IOException wraps into AppError Network`() {
        val failure = Result.failure<Int>(IOException("no connection"))

        val result = failure.mapError()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Network)
    }

    @Test
    fun `mapError on failure with AppError returns same AppError instance`() {
        val original = AppError.Server(code = 503)
        val failure = Result.failure<Int>(original)

        val result = failure.mapError()

        assertTrue(result.isFailure)
        assertSame(original, result.exceptionOrNull())
    }

    @Test
    fun `mapError on failure with RuntimeException wraps into AppError Unknown`() {
        val failure = Result.failure<Int>(RuntimeException("unexpected"))

        val result = failure.mapError()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AppError.Unknown)
    }

    // endregion
}
