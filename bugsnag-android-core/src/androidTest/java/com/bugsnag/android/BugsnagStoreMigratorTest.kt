package com.bugsnag.android

import com.bugsnag.android.internal.BugsnagStoreMigrator
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class BugsnagStoreMigratorTest {
    private lateinit var tmpdir: File

    @Before
    fun setUp() {
        tmpdir = File(System.getProperty("java.io.tmpdir"), UUID.randomUUID().toString())
        tmpdir.mkdirs()
    }

    @After
    fun tearDown() {
        tmpdir.deleteRecursively()
    }

    @Test
    fun tesMoveFilesToNewDir() {
        val filesToMove = listOf(
            "bugsnag-sessions" to "bugsnag/sessions",
            "last-run-info" to "bugsnag/last-run-info",
            "user-info" to "bugsnag/user-info",
            "bugsnag-native" to "bugsnag/native",
            "bugsnag-errors" to "bugsnag/errors"
        )
        filesToMove.forEach { (from, to) ->
            val file = File(tmpdir, from).apply { mkdirs() }
            val newDir = File(tmpdir, to)
            BugsnagStoreMigrator.migrateLegacyFiles(lazyOf(tmpdir))
            assertFalse(file.isDirectory)
            assertFalse(file.exists())
            assertTrue(newDir.isDirectory)
            assertTrue(newDir.exists())
        }
    }

    @Test
    fun testMoveOneFileToNewDirectory() {
        val file = File(tmpdir, "bugsnag-native").apply { mkdirs() }
        val newDirFile = File(tmpdir, "bugsnag/native")
        BugsnagStoreMigrator.migrateLegacyFiles(lazyOf(tmpdir))
        assertFalse(file.isDirectory)
        assertFalse(file.exists())
        assertTrue(newDirFile.exists())
        assertTrue(newDirFile.isDirectory)
    }

    @Test
    fun testNotToMoveUndefinedFile() {
        val newDirFile = File(tmpdir, "bugsnag/test")
        val file = File(tmpdir, "test").apply {
            mkdirs()
            createNewFile()
        }
        assertTrue(file.isDirectory)
        assertTrue(file.exists())
        BugsnagStoreMigrator.migrateLegacyFiles(lazyOf(tmpdir))
        assertFalse(newDirFile.isDirectory)
        assertFalse(newDirFile.exists())
        assertTrue(file.isDirectory)
        assertTrue(file.exists())
    }

    @Test
    fun testDeepPathUndefinedFile() {
        val file = File(tmpdir, "test/tes2/test3").apply { mkdirs() }
        val newDirFile = File(tmpdir, "bugsnag/test/tes2/test3")
        BugsnagStoreMigrator.migrateLegacyFiles(lazyOf(tmpdir))
        assertFalse(newDirFile.exists())
        assertTrue(file.isDirectory)
        assertTrue(file.exists())
    }

    @Test
    fun testDeepPathDefinedFile() {
        val file = File(tmpdir, "bugsnag-sessions").apply { mkdirs() }
        File(file, "test1/tes2/test3").apply { mkdirs() }
        val newDirFile = File(tmpdir, "bugsnag/sessions/test1/tes2/test3")
        BugsnagStoreMigrator.migrateLegacyFiles(lazyOf(tmpdir))
        assertTrue(newDirFile.exists())
        assertFalse(file.isDirectory)
        assertFalse(file.exists())
    }

    @Test
    fun testMoveFilesToNewDirectory() {
        val file = File(tmpdir, "bugsnag-errors").apply { mkdirs() }
        File(file, "test1").apply { createNewFile() }
        File(file, "test2").apply { createNewFile() }
        val newDirFile = File(tmpdir, "bugsnag/errors")
        val test1From = File(tmpdir, "bugsnag-errors/test1")
        val test2From = File(tmpdir, "bugsnag-errors/test2")
        val test1moved = File(tmpdir, "bugsnag/errors/test1")
        val test2moved = File(tmpdir, "bugsnag/errors/test2")

        BugsnagStoreMigrator.migrateLegacyFiles(lazyOf(tmpdir))
        assertFalse(file.isDirectory)
        assertFalse(file.exists())
        assertFalse(test1From.exists())
        assertFalse(test2From.exists())
        assertTrue(newDirFile.exists())
        assertTrue(newDirFile.isDirectory)
        assertTrue(test1moved.exists())
        assertTrue(test2moved.exists())
    }
}
