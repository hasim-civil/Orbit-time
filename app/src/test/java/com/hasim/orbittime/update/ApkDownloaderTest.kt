package com.hasim.orbittime.update

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * The download half of "Update now": the bytes land in a file, and every way that can go wrong
 * ends in a message rather than a crash or a half-written APK left on disk.
 */
class ApkDownloaderTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val apkBytes = ByteArray(4096) { (it % 251).toByte() }

    private fun destination(): File = File(temporaryFolder.newFolder("updates"), "OrbitTime-v.1.2.apk")

    private fun downloaderServing(
        bytes: ByteArray = apkBytes,
        reportedLength: Long = bytes.size.toLong(),
    ) = ApkDownloader { ByteArrayInputStream(bytes) to reportedLength }

    @Test
    fun `a complete download lands on disk with the right bytes`() = runTest {
        val file = destination()

        val result = downloaderServing().download(
            url = "https://github.com/hasim-civil/Orbit-time/releases/download/v.1.2/OrbitTime-v.1.2.apk",
            destination = file,
            expectedBytes = apkBytes.size.toLong(),
        )

        assertTrue(result is DownloadResult.Success)
        assertArrayEqualsBytes(apkBytes, file.readBytes())
    }

    @Test
    fun `progress runs from zero to one`() = runTest {
        val seen = mutableListOf<Float?>()

        downloaderServing().download(
            url = "https://example.com/OrbitTime-v.1.2.apk",
            destination = destination(),
            expectedBytes = apkBytes.size.toLong(),
            onProgress = { seen += it },
        )

        assertEquals(0f, seen.first())
        assertEquals(1f, seen.last())
        assertTrue(seen.filterNotNull().zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun `an unknown length reports indeterminate progress rather than a guess`() = runTest {
        val seen = mutableListOf<Float?>()

        val result = ApkDownloader { ByteArrayInputStream(apkBytes) to -1L }.download(
            url = "https://example.com/OrbitTime-v.1.2.apk",
            destination = destination(),
            expectedBytes = 0L,
            onProgress = { seen += it },
        )

        assertTrue(result is DownloadResult.Success)
        assertTrue(seen.first() == null)
    }

    @Test
    fun `a network failure mid-download reports a message and leaves no partial file`() = runTest {
        val file = destination()
        val failing = ApkDownloader {
            object : InputStream() {
                override fun read(): Int = throw IOException("Connection reset")
                override fun read(b: ByteArray, off: Int, len: Int): Int = throw IOException("Connection reset")
            } to apkBytes.size.toLong()
        }

        val result = failing.download("https://example.com/a.apk", file, apkBytes.size.toLong())

        assertTrue(result is DownloadResult.Failure)
        assertTrue((result as DownloadResult.Failure).reason.isNotBlank())
        assertFalse(file.exists())
    }

    @Test
    fun `a failure to open the connection is reported, not thrown`() = runTest {
        val failing = ApkDownloader { throw IOException("Unable to resolve host") }

        val result = failing.download("https://example.com/a.apk", destination(), 1024L)

        assertTrue(result is DownloadResult.Failure)
    }

    @Test
    fun `a truncated transfer is rejected instead of installed`() = runTest {
        val file = destination()
        // The server promised the full size but the stream ended early — a closed socket looks
        // like a clean end of file, so the written length is what catches it.
        val truncated = downloaderServing(bytes = apkBytes.copyOf(1000), reportedLength = apkBytes.size.toLong())

        val result = truncated.download("https://example.com/a.apk", file, apkBytes.size.toLong())

        assertTrue(result is DownloadResult.Failure)
        assertFalse(file.exists())
    }

    @Test
    fun `an empty response is rejected`() = runTest {
        val file = destination()

        val result = downloaderServing(bytes = ByteArray(0), reportedLength = 0L).download(
            url = "https://example.com/a.apk",
            destination = file,
            expectedBytes = 0L,
        )

        assertTrue(result is DownloadResult.Failure)
        assertFalse(file.exists())
    }

    @Test
    fun `a non-HTTPS URL is refused outright`() = runTest {
        val result = downloaderServing().download(
            url = "http://github.com/hasim-civil/Orbit-time/releases/download/v.1.2/OrbitTime-v.1.2.apk",
            destination = destination(),
            expectedBytes = apkBytes.size.toLong(),
        )

        assertTrue(result is DownloadResult.Failure)
    }

    @Test
    fun `cancelling mid-download stops the transfer and deletes the partial file`() = runTest {
        val file = destination()
        val started = CompletableDeferred<Unit>()
        // A stream that never ends, so cancellation always lands mid-transfer. The download
        // loop checks for cancellation before each read, so it stops within one chunk.
        val stalling = ApkDownloader {
            object : InputStream() {
                override fun read(): Int = -1
                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    started.complete(Unit)
                    Thread.sleep(50)
                    val chunk = minOf(len, 256)
                    apkBytes.copyInto(b, off, 0, chunk)
                    return chunk
                }
            } to Long.MAX_VALUE
        }

        val job = launch(Dispatchers.IO) {
            stalling.download("https://example.com/a.apk", file, apkBytes.size.toLong() * 10)
        }
        started.await()
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        withContext(Dispatchers.IO) { assertFalse(file.exists()) }
    }

    @Test
    fun `an earlier partial download is replaced rather than appended to`() = runTest {
        val file = destination()
        file.parentFile?.mkdirs()
        file.writeBytes(ByteArray(9999) { 7 })

        downloaderServing().download("https://example.com/a.apk", file, apkBytes.size.toLong())

        assertEquals(apkBytes.size.toLong(), file.length())
    }

    private fun assertArrayEqualsBytes(expected: ByteArray, actual: ByteArray) {
        assertEquals(expected.size, actual.size)
        assertTrue(expected.contentEquals(actual))
    }
}
