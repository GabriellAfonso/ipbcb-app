package com.ipb.castelobranco.features.bible.data.local

import com.ipb.castelobranco.features.bible.domain.model.BibleHeading
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BibleLocalStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        explicitNulls = false
    }

    private lateinit var rootDir: File
    private lateinit var legacyDir: File
    private lateinit var store: BibleLocalStore

    private val apiResponse = """
        [
          {
            "abbrev": "gn",
            "name": "Gênesis",
            "chapters": [["No princípio...", ["A terra", "era sem forma."]], ["Assim..."]],
            "headings": [
              [{"verse": 1, "level": 1, "title": "A criação do Universo"}],
              []
            ]
          },
          {"abbrev": "ex", "name": "Êxodo", "chapters": [["Estes são os nomes..."]]}
        ]
    """.trimIndent()

    @Before
    fun setup() {
        rootDir = tmp.newFolder("bible")
        legacyDir = tmp.newFolder("snapshots")
        store = BibleLocalStore(rootDir, legacyDir, json, testDispatcher)
    }

    private suspend fun importNaa(raw: String = apiResponse) =
        store.import(BibleTranslation.NAA, raw.byteInputStream())

    @Test
    fun `import splits the translation into an index and one file per book`() = runTest(testDispatcher) {
        importNaa()

        assertTrue(store.exists(BibleTranslation.NAA))
        assertFalse(store.exists(BibleTranslation.ARA))
        val index = store.loadIndex(BibleTranslation.NAA)!!
        assertEquals(listOf("gn", "ex"), index.map { it.abbrev })
        assertEquals(listOf(2, 1), index.first().verseCounts)
        assertEquals(
            setOf("index.json", "format", "00.json", "00.headings.json", "01.json", "01.headings.json"),
            File(rootDir, "naa").list()!!.toSet(),
        )
        assertTrue(store.isCurrent(BibleTranslation.NAA))
    }

    @Test
    fun `a book is read with its headings, a book without them reads empty`() = runTest(testDispatcher) {
        importNaa()

        val genesis = store.loadBook(BibleTranslation.NAA, position = 0, abbrev = "gn")!!
        val exodus = store.loadBook(BibleTranslation.NAA, position = 1, abbrev = "ex")!!

        assertEquals(listOf(BibleHeading(verse = 1, level = 1, title = "A criação do Universo")), genesis.headings(1))
        assertEquals(emptyList<BibleHeading>(), genesis.headings(2))
        assertEquals(emptyList<BibleHeading>(), exodus.headings(1))
    }

    @Test
    fun `an old-layout folder stays readable without headings and is not current`() = runTest(testDispatcher) {
        importNaa()
        val dir = File(rootDir, "naa")
        File(dir, "format").delete()
        dir.listFiles { file -> file.name.endsWith(".headings.json") }!!.forEach { it.delete() }

        val genesis = store.loadBook(BibleTranslation.NAA, position = 0, abbrev = "gn")!!

        assertEquals(listOf("Assim..."), genesis.chapter(2))
        assertTrue(genesis.headings.isEmpty())
        assertTrue(store.exists(BibleTranslation.NAA))
        assertFalse(store.isCurrent(BibleTranslation.NAA))
    }

    @Test
    fun `a corrupt headings file deletes the translation`() = runTest(testDispatcher) {
        importNaa()
        File(rootDir, "naa/00.headings.json").writeText("[{broken")

        assertNull(store.loadBook(BibleTranslation.NAA, position = 0, abbrev = "gn"))
        assertFalse(store.exists(BibleTranslation.NAA))
    }

    @Test
    fun `a book is read alone, with verse parts joined`() = runTest(testDispatcher) {
        importNaa()

        val genesis = store.loadBook(BibleTranslation.NAA, position = 0, abbrev = "gn")!!

        assertEquals("gn", genesis.abbrev)
        assertEquals(listOf("No princípio...", "A terra era sem forma."), genesis.chapter(1))
        assertEquals(listOf("Assim..."), genesis.chapter(2))
    }

    @Test
    fun `a failed import keeps the previous translation in place`() = runTest(testDispatcher) {
        importNaa()

        runCatching { importNaa("""[{"abbrev": "gn", "name": "Gên""") }

        assertEquals(2, store.loadIndex(BibleTranslation.NAA)?.size)
        assertFalse(File(rootDir, "naa.tmp").exists())
    }

    @Test
    fun `a corrupt index deletes the translation`() = runTest(testDispatcher) {
        importNaa()
        File(rootDir, "naa/index.json").writeText("{not json")

        assertNull(store.loadIndex(BibleTranslation.NAA))
        assertFalse(store.exists(BibleTranslation.NAA))
    }

    @Test
    fun `the old single file is converted and deleted`() = runTest(testDispatcher) {
        File(legacyDir, "bible_naa.json").writeText(apiResponse)
        File(legacyDir, "bible_naa_etag.txt").writeText("\"abc\"")

        store.migrateLegacy()

        assertTrue(store.exists(BibleTranslation.NAA))
        // The old file predates headings: the converted copy must be downloaded again.
        assertFalse(store.isCurrent(BibleTranslation.NAA))
        assertEquals(2, store.loadIndex(BibleTranslation.NAA)?.size)
        assertFalse(File(legacyDir, "bible_naa.json").exists())
        assertFalse(File(legacyDir, "bible_naa_etag.txt").exists())
    }

    @Test
    fun `a corrupt old file is deleted without a translation`() = runTest(testDispatcher) {
        File(legacyDir, "bible_naa.json").writeText("[{broken")

        store.migrateLegacy()

        assertFalse(store.exists(BibleTranslation.NAA))
        assertFalse(File(legacyDir, "bible_naa.json").exists())
    }

    @Test
    fun `clear removes the translation and its size`() = runTest(testDispatcher) {
        importNaa()
        assertTrue(store.sizeBytes(BibleTranslation.NAA) > 0)

        store.clear(BibleTranslation.NAA)

        assertFalse(store.exists(BibleTranslation.NAA))
        assertEquals(0L, store.sizeBytes(BibleTranslation.NAA))
    }
}
