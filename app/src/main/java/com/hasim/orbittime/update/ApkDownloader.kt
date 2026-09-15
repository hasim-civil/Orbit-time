package com.hasim.orbittime.update

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.coroutineContext

/**
 * Outcome of downloading a release APK. There is no "cancelled" case: cancelling the dialog
 * cancels the coroutine, so the caller's own cancellation handling covers it — the downloader
 * only deletes the partial file and rethrows.
 */
sealed interface DownloadResult {
    data class Success(val file: File) : DownloadResult
    data class Failure(val reason: String) : DownloadResult
}

/** Opens a byte stream for an HTTPS URL, alongside the length the server reported. */
fun interface HttpStreamOpener {
    /** @return the stream and its expected length in bytes, or -1 when the server omits it. */
    fun open(url: String): Pair<InputStream, Long>
}

/**
 * Downloads a release APK to a file, reporting progress as it goes.
 *
 * The download is a plain streamed HTTPS read rather than a `DownloadManager` enqueue, so it
 * follows the dialog's own lifecycle: progress updates the UI directly, and dismissing the
 * dialog cancels the coroutine, which stops the transfer and deletes the half-written file
 * instead of leaving a system download running behind a screen the user has closed.
 */
class ApkDownloader(
    private val opener: HttpStreamOpener = HttpsStreamOpener(),
) {

    /**
     * @param onProgress fraction complete in `0f..1f`, or `null` when the server didn't say how
     *   large the file is and progress can't be known.
     */
    suspend fun download(
        url: String,
        destination: File,
        expectedBytes: Long = 0L,
        onProgress: (Float?) -> Unit = {},
    ): DownloadResult = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://", ignoreCase = true)) {
            return@withContext DownloadResult.Failure("The download link isn't a secure HTTPS address.")
        }

        try {
            destination.parentFile?.mkdirs()
            // A leftover file from an earlier, failed attempt would otherwise be appended to
            // or mistaken for a complete download.
            destination.delete()

            val (stream, reportedLength) = opener.open(url)
            val total = if (reportedLength > 0) reportedLength else expectedBytes

            stream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var written = 0L
                    onProgress(if (total > 0) 0f else null)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                    output.flush()

                    // A transfer cut off mid-flight closes cleanly and looks like success, so
                    // the size is checked rather than trusted — installing a truncated APK
                    // would fail much later with a far less useful message.
                    if (total > 0 && written < total) {
                        destination.delete()
                        return@withContext DownloadResult.Failure("The download ended early. Please try again.")
                    }
                }
            }

            if (!destination.exists() || destination.length() == 0L) {
                destination.delete()
                return@withContext DownloadResult.Failure("The downloaded file was empty.")
            }

            onProgress(1f)
            DownloadResult.Success(destination)
        } catch (cancellation: CancellationException) {
            destination.delete()
            throw cancellation
        } catch (error: IOException) {
            destination.delete()
            DownloadResult.Failure("Couldn't download the update. Check your connection and try again.")
        } catch (error: Exception) {
            destination.delete()
            DownloadResult.Failure("Couldn't download the update. Please try again.")
        }
    }
}

/**
 * The real opener: HTTPS only, following GitHub's redirect from `github.com` to its asset host.
 */
class HttpsStreamOpener(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
) : HttpStreamOpener {

    override fun open(url: String): Pair<InputStream, Long> {
        val connection = URL(url).openConnection() as HttpsURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", UpdateConfig.APK_CONTENT_TYPE + ", */*")
        connection.setRequestProperty("User-Agent", UpdateConfig.USER_AGENT)

        val status = connection.responseCode
        if (status != HttpsURLConnection.HTTP_OK) {
            connection.disconnect()
            throw IOException("Download failed with HTTP $status")
        }
        return connection.inputStream to connection.contentLengthLong
    }
}
