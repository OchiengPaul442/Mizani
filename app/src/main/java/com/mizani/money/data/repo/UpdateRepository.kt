package com.mizani.money.data.repo

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class AppRelease(
    val versionName: String,
    val versionCode: Int,
    val pageUrl: String,
)

sealed interface UpdateCheckResult {
    data object NoReleases : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Available(val release: AppRelease) : UpdateCheckResult
}

/** Checks public GitHub release metadata only. APK downloads and installation stay user-controlled. */
class UpdateRepository(
    private val connectionFactory: (URL) -> HttpURLConnection = {
        it.openConnection() as HttpURLConnection
    },
) {
    fun checkForUpdate(currentVersionCode: Int): UpdateCheckResult {
        val connection = connectionFactory(URL(LATEST_RELEASE_API))
        try {
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Mizani-Android")
            connection.instanceFollowRedirects = false

            return when (val responseCode = connection.responseCode) {
                HttpURLConnection.HTTP_NOT_FOUND -> UpdateCheckResult.NoReleases
                HttpURLConnection.HTTP_OK -> parseRelease(readLimitedBody(connection), currentVersionCode)
                else -> throw IOException("Release service returned HTTP $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRelease(body: String, currentVersionCode: Int): UpdateCheckResult {
        val json = JSONObject(body)
        if (json.optBoolean("draft") || json.optBoolean("prerelease")) {
            return UpdateCheckResult.NoReleases
        }

        val tag = json.getString("tag_name")
        val versionName = tag.removePrefix("v").removePrefix("V")
        val parts = VERSION_PATTERN.matchEntire(versionName)
            ?: throw IOException("Release version is not MAJOR.MINOR.PATCH")
        val (major, minor, patch) = parts.groupValues.drop(1).map(String::toInt)
        val versionCode = major * VERSION_MAJOR_MULTIPLIER + minor * VERSION_MINOR_MULTIPLIER + patch
        val pageUrl = json.getString("html_url")
        if (!isTrustedReleaseUrl(pageUrl, tag)) {
            throw IOException("Release page URL is invalid")
        }

        return if (versionCode > currentVersionCode) {
            UpdateCheckResult.Available(AppRelease(versionName, versionCode, pageUrl))
        } else {
            UpdateCheckResult.UpToDate
        }
    }

    private fun readLimitedBody(connection: HttpURLConnection): String =
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val body = StringBuilder()
            val buffer = CharArray(RESPONSE_BUFFER_SIZE)
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                if (body.length + count > MAX_RESPONSE_CHARS) {
                    throw IOException("Release response is too large")
                }
                body.append(buffer, 0, count)
            }
            body.toString()
        }

    private fun isTrustedReleaseUrl(value: String, tag: String): Boolean = try {
        val uri = URI(value)
        uri.scheme == "https" &&
            uri.host == GITHUB_HOST &&
            (uri.port == -1 || uri.port == 443) &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null &&
            uri.path == "$RELEASE_PAGE_PATH$tag"
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val GITHUB_HOST = "github.com"
        const val LATEST_RELEASE_API = "https://api.github.com/repos/OchiengPaul442/budgeting-application/releases/latest"
        const val RELEASE_PAGE_PATH = "/OchiengPaul442/budgeting-application/releases/tag/"
        const val CONNECT_TIMEOUT_MILLIS = 3_000
        const val READ_TIMEOUT_MILLIS = 4_000
        const val RESPONSE_BUFFER_SIZE = 2_048
        const val MAX_RESPONSE_CHARS = 64 * 1_024
        const val VERSION_MAJOR_MULTIPLIER = 1_000_000
        const val VERSION_MINOR_MULTIPLIER = 1_000
        val VERSION_PATTERN = Regex("^(0|[1-9]\\d{0,2})\\.(0|[1-9]\\d{0,2})\\.(0|[1-9]\\d{0,2})$")
    }
}
