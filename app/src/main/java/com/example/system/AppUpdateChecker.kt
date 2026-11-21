package com.example.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val isPrerelease: Boolean = false
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(
        val currentVersion: String,
        val latestFoundTag: String? = null,
        val releaseUrl: String = AppUpdateChecker.GITHUB_LATEST_RELEASE_WEB
    ) : UpdateCheckResult()
    data class NoReleases(val currentVersion: String) : UpdateCheckResult()
    data object Offline : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

data class UpdateCheckStatusSummary(
    val checkedAtMillis: Long,
    val statusLine: String,
    val detailLine: String? = null,
    val hasUpdate: Boolean = false,
    val isError: Boolean = false
)

object AppUpdateChecker {
    private const val TAG = "AppUpdateChecker"
    const val GITHUB_RELEASES_REPO_PREFIX = "https://github.com/thathsarayasiru97/80endence"
    const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/thathsarayasiru97/80endence/releases/latest"
    const val GITHUB_RELEASES_LIST_API =
        "https://api.github.com/repos/thathsarayasiru97/80endence/releases?per_page=10"
    const val GITHUB_LATEST_RELEASE_WEB =
        "https://github.com/thathsarayasiru97/80endence/releases/latest"
    const val GITHUB_RELEASES_WEB =
        "https://github.com/thathsarayasiru97/80endence/releases"

    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 8_000
    private val DOTTED_VERSION_REGEX = Regex("(?:^|[^0-9])v?(\\d+\\.\\d+(?:\\.\\d+)*)", RegexOption.IGNORE_CASE)
    private val SIMPLE_V_VERSION_REGEX = Regex("(?:^|[^0-9])v(\\d+)", RegexOption.IGNORE_CASE)
    private val BARE_NUMBER_REGEX = Regex("^(\\d+)$")

    fun getInstalledVersionName(context: Context): String {
        val pkgVersion = runCatching {
            val pm = context.applicationContext.packageManager
            val pkgName = context.applicationContext.packageName
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(pkgName, android.content.pm.PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(pkgName, 0).versionName
            }
        }.getOrNull()?.trim()
        return pkgVersion?.takeIf { it.isNotEmpty() } ?: BuildConfig.VERSION_NAME
    }

    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        // Rely on NET_CAPABILITY_INTERNET (some VPNs/mobile carriers/emulators omit VALIDATED even when online)
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
                trySend(true)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet || isInternetAvailable(appContext))
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
        currentVersion: String = getInstalledVersionName(context)
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!isInternetAvailable(context)) {
            return@withContext UpdateCheckResult.Offline
        }

        try {
            val latestResponse = fetchJson(GITHUB_LATEST_RELEASE_API, currentVersion)
            when (latestResponse.first) {
                HttpURLConnection.HTTP_OK -> {
                    val body = latestResponse.second.orEmpty()
                    Log.d(TAG, "GitHub /releases/latest returned 200")
                    parseReleaseJson(body, currentVersion)
                }

                HttpURLConnection.HTTP_NOT_FOUND -> {
                    // GitHub returns 404 on /releases/latest when only pre-releases exist OR no releases exist yet.
                    // Fall back to /releases?per_page=10 so we can distinguish and still detect pre-releases!
                    Log.d(TAG, "GitHub /releases/latest returned 404; checking /releases list fallback")
                    val listResponse = fetchJson(GITHUB_RELEASES_LIST_API, currentVersion)
                    when (listResponse.first) {
                        HttpURLConnection.HTTP_OK -> {
                            parseReleasesArrayJson(listResponse.second.orEmpty(), currentVersion)
                        }
                        HttpURLConnection.HTTP_NOT_FOUND -> {
                            UpdateCheckResult.Error("GitHub repository not found or private (HTTP 404).")
                        }
                        HttpURLConnection.HTTP_FORBIDDEN -> {
                            UpdateCheckResult.Error("GitHub API rate limit reached. Try again later.")
                        }
                        else -> {
                            UpdateCheckResult.Error("Update server returned HTTP ${listResponse.first}.")
                        }
                    }
                }

                HttpURLConnection.HTTP_FORBIDDEN -> {
                    UpdateCheckResult.Error("GitHub API rate limit reached. Try again later.")
                }

                else -> {
                    UpdateCheckResult.Error("Update server returned HTTP ${latestResponse.first}.")
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Network error checking updates: ${e.message}")
            if (!isInternetAvailable(context)) {
                UpdateCheckResult.Offline
            } else {
                UpdateCheckResult.Error("Network error: ${e.localizedMessage ?: "Could not reach GitHub"}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error checking updates: ${e.message}")
            UpdateCheckResult.Error("Could not parse GitHub release info right now.")
        }
    }

    private fun fetchJson(endpointUrl: String, currentVersion: String): Pair<Int, String?> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(endpointUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                setRequestProperty("User-Agent", "80endance-Android/$currentVersion")
            }
            val code = connection.responseCode
            val body = if (code in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
            code to body
        } finally {
            connection?.disconnect()
        }
    }

    internal fun parseReleasesArrayJson(
        jsonArrayText: String,
        currentVersion: String
    ): UpdateCheckResult {
        val array = JSONArray(jsonArrayText)
        val nonDrafts = mutableListOf<JSONObject>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            if (!obj.optBoolean("draft", false)) {
                nonDrafts.add(obj)
            }
        }
        if (nonDrafts.isEmpty()) {
            return UpdateCheckResult.NoReleases(currentVersion)
        }
        // Prefer the newest stable release; if only pre-releases exist, inspect the newest pre-release.
        val best = nonDrafts.firstOrNull { !it.optBoolean("prerelease", false) } ?: nonDrafts.first()
        return evaluateReleaseObject(best, currentVersion, allowPrerelease = best.optBoolean("prerelease", false))
    }

    internal fun parseReleaseJson(
        jsonText: String,
        currentVersion: String
    ): UpdateCheckResult {
        val json = JSONObject(jsonText)
        return evaluateReleaseObject(json, currentVersion, allowPrerelease = false)
    }

    private fun evaluateReleaseObject(
        json: JSONObject,
        currentVersion: String,
        allowPrerelease: Boolean
    ): UpdateCheckResult {
        if (json.optBoolean("draft", false)) {
            return UpdateCheckResult.NoReleases(currentVersion)
        }
        val isPrerelease = json.optBoolean("prerelease", false)
        if (isPrerelease && !allowPrerelease) {
            return UpdateCheckResult.UpToDate(currentVersion)
        }

        val tagName = json.optString("tag_name", "").trim()
        val releaseName = json.optString("name", "").trim()
        val effectiveTag = tagName.ifBlank { releaseName }
        if (effectiveTag.isBlank()) {
            return UpdateCheckResult.NoReleases(currentVersion)
        }

        val rawHtmlUrl = json.optString("html_url", "").trim()
        val safeUrl = if (rawHtmlUrl.startsWith(GITHUB_RELEASES_REPO_PREFIX)) {
            rawHtmlUrl
        } else {
            GITHUB_LATEST_RELEASE_WEB
        }

        if (!isNewerVersion(latestTag = effectiveTag, currentVersion = currentVersion)) {
            return UpdateCheckResult.UpToDate(
                currentVersion = currentVersion,
                latestFoundTag = effectiveTag,
                releaseUrl = safeUrl
            )
        }

        val releaseTitle = releaseName.ifBlank {
            "80endance ${formatVersionLabel(effectiveTag)}"
        }
        val releaseNotes = json.optString("body", "").trim()

        return UpdateCheckResult.UpdateAvailable(
            AppUpdateInfo(
                currentVersion = currentVersion,
                latestVersion = effectiveTag,
                releaseTitle = releaseTitle,
                releaseNotes = releaseNotes,
                releaseUrl = safeUrl,
                isPrerelease = isPrerelease
            )
        )
    }

    /**
     * Flexible semantic version comparison supporting tags such as:
     * "v1.2.0", "1.10", "Release 1.2", "80endance-v1.3.1", "v1.0.1-beta.2", etc.
     * Returns true strictly when [latestTag] is newer than [currentVersion].
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
        // Same numeric base: a stable release is newer than a pre-release (e.g., 1.2.0 > 1.2.0-beta)
        val latestIsPre = hasPrereleaseSuffix(latestTag)
        val currentIsPre = hasPrereleaseSuffix(currentVersion)
        if (!latestIsPre && currentIsPre) return true
        return false
    }

    fun formatVersionLabel(rawVersion: String): String {
        val trimmed = rawVersion.trim()
        if (trimmed.isEmpty()) return "v1.0"
        return if (trimmed.startsWith("v", ignoreCase = true)) {
            "v${trimmed.substring(1)}"
        } else if (trimmed.first().isDigit()) {
            "v$trimmed"
        } else {
            trimmed
        }
    }

    fun openLatestRelease(
        context: Context,
        releaseUrl: String = GITHUB_LATEST_RELEASE_WEB
    ): Boolean {
        val targetUrl = if (releaseUrl.startsWith(GITHUB_RELEASES_REPO_PREFIX)) {
            releaseUrl
        } else {
            GITHUB_RELEASES_WEB
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
        val trimmed = raw.trim()
        val match = DOTTED_VERSION_REGEX.find(trimmed)
            ?: SIMPLE_V_VERSION_REGEX.find(trimmed)
            ?: BARE_NUMBER_REGEX.find(trimmed)
            ?: return null
        val tokens = match.groupValues[1].split('.')
        val numbers = ArrayList<Int>(tokens.size)
        for (token in tokens) {
            val value = token.toIntOrNull() ?: return null
            if (value < 0) return null
            numbers.add(value)
        }
        return numbers.takeIf { it.isNotEmpty() }
    }

    private fun hasPrereleaseSuffix(raw: String): Boolean {
        val lower = raw.lowercase()
        return lower.contains("-alpha") ||
            lower.contains("-beta") ||
            lower.contains("-rc") ||
            lower.contains("-dev") ||
            lower.contains("-pre")
    }
}
