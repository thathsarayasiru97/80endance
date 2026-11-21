package com.example.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val releaseUrl: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data object Offline : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object AppUpdateChecker {
    const val GITHUB_RELEASES_REPO_PREFIX = "https://github.com/thathsarayasiru97/80endence"
    const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/thathsarayasiru97/80endence/releases/latest"
    const val GITHUB_LATEST_RELEASE_WEB =
        "https://github.com/thathsarayasiru97/80endence/releases/latest"

    private const val CONNECT_TIMEOUT_MS = 6_000
    private const val READ_TIMEOUT_MS = 6_000

    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun observeInternetAvailable(context: Context): Flow<Boolean> = callbackFlow {
        val appContext = context.applicationContext
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) {
            trySend(false)
            close()
            return@callbackFlow
        }

        trySend(isInternetAvailable(appContext))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isInternetAvailable(appContext))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val validated =
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(validated || isInternetAvailable(appContext))
            }

            override fun onLost(network: Network) {
                trySend(isInternetAvailable(appContext))
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }

        val registered = runCatching {
            cm.registerDefaultNetworkCallback(callback)
            true
        }.getOrDefault(false)

        awaitClose {
            if (registered) {
                runCatching { cm.unregisterNetworkCallback(callback) }
            }
        }
    }.distinctUntilChanged()

    suspend fun checkForUpdate(
        context: Context,
        currentVersion: String = BuildConfig.VERSION_NAME
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isInternetAvailable(context)) {
            return@withContext UpdateCheckResult.Offline
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_LATEST_RELEASE_API)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "80endance-Android/$currentVersion")
            }

            when (val responseCode = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    parseReleaseJson(body, currentVersion)
                }

                HttpURLConnection.HTTP_NOT_FOUND -> {
                    // Repository has no published stable releases yet
                    UpdateCheckResult.UpToDate(currentVersion)
                }

                HttpURLConnection.HTTP_FORBIDDEN -> {
                    UpdateCheckResult.Error("GitHub rate limit reached. Try again later.")
                }

                else -> {
                    UpdateCheckResult.Error("Update server returned HTTP $responseCode.")
                }
            }
        } catch (e: IOException) {
            if (!isInternetAvailable(context)) {
                UpdateCheckResult.Offline
            } else {
                UpdateCheckResult.Error("Could not check for updates right now.")
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Could not check for updates right now.")
        } finally {
            connection?.disconnect()
        }
    }

    internal fun parseReleaseJson(
        jsonText: String,
        currentVersion: String
    ): UpdateCheckResult {
        val json = JSONObject(jsonText)
        if (json.optBoolean("draft", false) || json.optBoolean("prerelease", false)) {
            return UpdateCheckResult.UpToDate(currentVersion)
        }

        val tagName = json.optString("tag_name", "").trim()
        if (tagName.isBlank()) {
            return UpdateCheckResult.UpToDate(currentVersion)
        }

        if (!isNewerVersion(latestTag = tagName, currentVersion = currentVersion)) {
            return UpdateCheckResult.UpToDate(currentVersion)
        }

        val releaseTitle = json.optString("name", "").trim().ifBlank {
            "80endance ${formatVersionLabel(tagName)}"
        }
        val releaseNotes = json.optString("body", "").trim()
        val rawHtmlUrl = json.optString("html_url", "").trim()
        val safeUrl = if (rawHtmlUrl.startsWith(GITHUB_RELEASES_REPO_PREFIX)) {
            rawHtmlUrl
        } else {
            GITHUB_LATEST_RELEASE_WEB
        }

        return UpdateCheckResult.UpdateAvailable(
            AppUpdateInfo(
                currentVersion = currentVersion,
                latestVersion = tagName,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                releaseUrl = safeUrl
            )
        )
    }

    /**
     * Semantic version comparison supporting tags such as "v1.2.0", "1.10", "v1.0-build.5", etc.
     * Returns true strictly when [latestTag] has a higher numeric version than [currentVersion].
     */
    fun isNewerVersion(latestTag: String, currentVersion: String): Boolean {
        val latestParts = parseNumericSegments(latestTag) ?: return false
        val currentParts = parseNumericSegments(currentVersion) ?: return false
        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val latest = latestParts.getOrElse(i) { 0 }
            val current = currentParts.getOrElse(i) { 0 }
            if (latest != current) {
                return latest > current
            }
        }
        return false
    }

    fun formatVersionLabel(rawVersion: String): String {
        val trimmed = rawVersion.trim()
        if (trimmed.isEmpty()) return "v1.0"
        return if (trimmed.startsWith("v", ignoreCase = true)) {
            "v${trimmed.substring(1)}"
        } else {
            "v$trimmed"
        }
    }

    fun openLatestRelease(
        context: Context,
        releaseUrl: String = GITHUB_LATEST_RELEASE_WEB
    ): Boolean {
        val targetUrl = if (releaseUrl.startsWith(GITHUB_RELEASES_REPO_PREFIX)) {
            releaseUrl
        } else {
            GITHUB_LATEST_RELEASE_WEB
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun parseNumericSegments(raw: String): List<Int>? {
        val cleaned = raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .substringBefore('+')
            .trim()
        if (cleaned.isEmpty()) return null
        val tokens = cleaned.split('.')
        val numbers = ArrayList<Int>(tokens.size)
        for (token in tokens) {
            val value = token.toIntOrNull() ?: return null
            if (value < 0) return null
            numbers.add(value)
        }
        return numbers
    }
}
