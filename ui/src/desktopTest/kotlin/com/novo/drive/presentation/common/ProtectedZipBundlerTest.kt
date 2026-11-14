package com.novo.drive.presentation.common

import java.io.File
import kotlinx.coroutines.runBlocking
import net.lingala.zip4j.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** The protected ZIP export must really encrypt with AES-256 and must survive name sanitising. */
class ProtectedZipBundlerTest {

    private val outDir = File.createTempFile("novodrive-zip", "").apply {
        delete()
        mkdirs()
        deleteOnExit()
    }
    private val srcA = File(outDir, "a.txt").apply { writeText("alpha-content"); deleteOnExit() }
    private val srcB = File(outDir, "b.txt").apply { writeText("beta-content"); deleteOnExit() }

    @Test
    fun `aes zip round-trips only with the right password`() = runBlocking {
        val zip = IplusProtectedZipBundler.createZip(
            outputDir = outDir,
            archiveBaseName = "bundle",
            localFilePaths = listOf(srcA.absolutePath, srcB.absolutePath),
            password = "s3cret",
            encryptAes256 = true
        )
        assertNotNull(zip)
        assertTrue(zip!!.exists() && zip.length() > 0)
        assertTrue(zip.name.endsWith("-protected.zip"))

        val extractDir = File(outDir, "out").apply { mkdirs() }
        ZipFile(zip, "s3cret".toCharArray()).extractAll(extractDir.absolutePath)
        assertEquals("alpha-content", File(extractDir, "a.txt").readText())
        assertEquals("beta-content", File(extractDir, "b.txt").readText())

        try {
            ZipFile(zip, "wrong".toCharArray()).extractAll(File(outDir, "bad").apply { mkdirs() }.absolutePath)
            fail("wrong password must not extract")
        } catch (_: Exception) {
            // expected: AEAD bad tag / zip exception
        }
    }

    @Test
    fun `encrypt flag off produces a readable plain zip even when a password is passed`() = runBlocking {
        val zip = IplusProtectedZipBundler.createZip(
            outputDir = outDir,
            archiveBaseName = "plain",
            localFilePaths = listOf(srcA.absolutePath),
            password = "ignored",
            encryptAes256 = false
        )
        assertNotNull(zip)
        val extractDir = File(outDir, "plain-out").apply { mkdirs() }
        ZipFile(zip!!).extractAll(extractDir.absolutePath) // no password
        assertEquals("alpha-content", File(extractDir, "a.txt").readText())
    }

    @Test
    fun `archive name is sanitised against path separators`() = runBlocking {
        val zip = IplusProtectedZipBundler.createZip(
            outputDir = outDir,
            archiveBaseName = """../evil:name""",
            localFilePaths = listOf(srcA.absolutePath),
            password = "",
            encryptAes256 = false
        )
        assertNotNull(zip)
        assertTrue(zip!!.name.startsWith(".._evil_name-protected.zip"))
        assertEquals(outDir.canonicalPath, zip.parentFile.canonicalPath)
    }

    @Test
    fun `no valid sources returns null`() = runBlocking {
        assertNull(
            IplusProtectedZipBundler.createZip(
                outputDir = outDir,
                archiveBaseName = "empty",
                localFilePaths = listOf(File(outDir, "does-not-exist.bin").absolutePath),
                password = "",
                encryptAes256 = false
            )
        )
    }

    @Test
    fun `blank password rejects AES encryption instead of producing unprotected zip`() = runBlocking {
        val sample = File(outDir, "secret.txt").apply { writeText("top secret") }
        assertNull(
            IplusProtectedZipBundler.createZip(
                outputDir = outDir,
                archiveBaseName = "weak",
                localFilePaths = listOf(sample.absolutePath),
                password = "   ",
                encryptAes256 = true
            )
        )
    }
}
