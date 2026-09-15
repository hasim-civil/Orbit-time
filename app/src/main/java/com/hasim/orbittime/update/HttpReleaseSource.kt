package com.hasim.orbittime.update

import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Reads the latest public release straight from GitHub's REST API over HTTPS.
 *
 * Unauthenticated on purpose. The releases are public, so no token is needed — and no token
 * could be shipped safely anyway, since anything embedded in an APK can be read back out of
 * it. [HttpsURLConnection] also means the connection can only ever be TLS: an `http://` URL
 * would fail the cast below rather than quietly downgrading.
 */
class HttpReleaseSource(
    private val url: String = UpdateConfig.latestReleaseUrl,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 15_000,
) : ReleaseSource {

    override suspend fun fetchLatestRelease(): String {
        val parsed = URL(url)
        require(parsed.protocol.equals("https", ignoreCase = true)) {
            "Update checks must use HTTPS"
        }

        val connection = parsed.openConnection() as HttpsURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.setRequestProperty("Accept", UpdateConfig.ACCEPT_HEADER)
            connection.setRequestProperty("User-Agent", UpdateConfig.USER_AGENT)

            val status = connection.responseCode
            // 404 (no releases yet) and 403 (anonymous rate limit) are ordinary outcomes here,
            // not bugs — they surface as an exception and the checker reports "no update".
            if (status != HttpsURLConnection.HTTP_OK) {
                throw IOException("GitHub returned HTTP $status")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
