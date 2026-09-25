package com.ipb.castelobranco.core.testing

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.rules.TemporaryFolder

/**
 * A [Context] whose `filesDir` and `cacheDir` are real directories inside [folder], so storages
 * that write under the app's private dirs can run against the real file system in a JVM test.
 */
fun tempDirContext(folder: TemporaryFolder): Context {
    val filesDir = folder.newFolder("files")
    val cacheDir = folder.newFolder("cache")
    return mockk {
        every { this@mockk.filesDir } returns filesDir
        every { this@mockk.cacheDir } returns cacheDir
    }
}
