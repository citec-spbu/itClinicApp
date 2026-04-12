package com.spbu.projecttrack.core.update

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class AndroidAppUpdate(
    val versionCode: Long,
    val currentVersionName: String,
    val availableVersionName: String,
    val apkUrl: String,
    val releasePageUrl: String,
)

object AndroidAppUpdateChecker {
    private const val UPDATE_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
    private const val UPDATE_MANIFEST_ASSET_NAME = "android-update.json"

    suspend fun checkForAvailableUpdate(context: Context): AndroidAppUpdate? {
        if (AndroidUpdateChannelConfig.repositoryOwner.isBlank() || AndroidUpdateChannelConfig.repositoryName.isBlank()) {
            return null
        }

        val preferences = AndroidUpdatePreferences(context)
        val now = System.currentTimeMillis()

        if (!preferences.shouldCheck(now)) {
            return null
        }

        return try {
            val installedVersion = readInstalledVersion(context)
            val manifest = fetchLatestMainChannelManifest()
            if (manifest == null) {
                preferences.markChecked(now)
                return null
            }

            preferences.markChecked(now)

            if (manifest.versionCode <= installedVersion.versionCode) {
                return null
            }

            if (preferences.dismissedVersionCode() == manifest.versionCode) {
                return null
            }

            AndroidAppUpdate(
                versionCode = manifest.versionCode,
                currentVersionName = installedVersion.versionName,
                availableVersionName = manifest.versionName,
                apkUrl = manifest.apkUrl,
                releasePageUrl = manifest.releasePageUrl,
            )
        } catch (_: Exception) {
            preferences.markChecked(now)
            null
        }
    }

    fun dismissUpdate(context: Context, versionCode: Long) {
        AndroidUpdatePreferences(context).markDismissed(versionCode)
    }

    fun openUpdatePage(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private suspend fun fetchLatestMainChannelManifest(): AndroidUpdateManifest? {
        createClient().use { client ->
            val releases = client.get(
                "https://api.github.com/repos/${AndroidUpdateChannelConfig.repositoryOwner}/${AndroidUpdateChannelConfig.repositoryName}/releases?per_page=10"
            ) {
                header(HttpHeaders.Accept, "application/vnd.github+json")
                header(HttpHeaders.UserAgent, "itClinicApp-android-update-check")
            }.body<List<GitHubReleaseDto>>()

            val latestMainRelease = releases.firstOrNull { release ->
                !release.draft && release.tagName.startsWith(AndroidUpdateChannelConfig.releaseTagPrefix)
            } ?: return null

            val manifestAsset = latestMainRelease.assets.firstOrNull { asset ->
                asset.name == UPDATE_MANIFEST_ASSET_NAME
            } ?: return null

            return client.get(manifestAsset.browserDownloadUrl) {
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "itClinicApp-android-update-check")
            }.body<AndroidUpdateManifest>()
        }
    }

    private fun createClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    }
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun readInstalledVersion(context: Context): InstalledAndroidVersion {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }

        return InstalledAndroidVersion(
            versionCode = versionCode,
            versionName = packageInfo.versionName ?: versionCode.toString(),
        )
    }
}

private object AndroidUpdateChannelConfig {
    const val repositoryOwner = "FergeSS"
    const val repositoryName = "itClinicApp"
    const val releaseTagPrefix = "android-main-"
}

private data class InstalledAndroidVersion(
    val versionCode: Long,
    val versionName: String,
)

private class AndroidUpdatePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "android_update_preferences",
        Context.MODE_PRIVATE
    )

    fun shouldCheck(nowMillis: Long): Boolean {
        val lastCheck = prefs.getLong(KEY_LAST_CHECK_AT, 0L)
        return nowMillis - lastCheck >= UPDATE_CHECK_INTERVAL_MS
    }

    fun markChecked(nowMillis: Long) {
        prefs.edit().putLong(KEY_LAST_CHECK_AT, nowMillis).apply()
    }

    fun dismissedVersionCode(): Long {
        return prefs.getLong(KEY_DISMISSED_VERSION_CODE, -1L)
    }

    fun markDismissed(versionCode: Long) {
        prefs.edit().putLong(KEY_DISMISSED_VERSION_CODE, versionCode).apply()
    }

    private companion object {
        private const val UPDATE_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        private const val KEY_LAST_CHECK_AT = "last_check_at"
        private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"
    }
}

@Serializable
private data class GitHubReleaseDto(
    @SerialName("tag_name")
    val tagName: String,
    val draft: Boolean,
    val assets: List<GitHubReleaseAssetDto>,
)

@Serializable
private data class GitHubReleaseAssetDto(
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
)

@Serializable
private data class AndroidUpdateManifest(
    val versionCode: Long,
    val versionName: String,
    val releaseTag: String,
    val apkUrl: String,
    val releasePageUrl: String,
    val commitSha: String? = null,
    val generatedAt: String? = null,
)
