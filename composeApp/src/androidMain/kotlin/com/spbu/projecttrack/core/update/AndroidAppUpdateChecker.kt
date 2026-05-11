package com.spbu.projecttrack.core.update

import android.content.Context
import android.content.SharedPreferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class AndroidAppUpdate(
    val channel: AndroidUpdateChannel,
    val versionCode: Long,
    val currentVersionName: String,
    val availableVersionName: String,
    val apkUrl: String,
    val sha256: String,
    val releasePageUrl: String,
    val changelog: List<String>,
    val isRequired: Boolean,
)

object AndroidAppUpdateChecker {
    suspend fun checkForAvailableUpdate(context: Context): AndroidAppUpdate? {
        if (AndroidUpdateChannelConfig.repositoryOwner.isBlank() || AndroidUpdateChannelConfig.repositoryName.isBlank()) {
            return null
        }

        val preferences = AndroidUpdatePreferences(context)

        return try {
            val installedVersion = readInstalledVersion(context)
            val updateChannel = resolveUpdateChannel(installedVersion)
            val manifest = fetchUpdateManifest(updateChannel)
            if (manifest == null) {
                return null
            }

            if (manifest.versionCode <= installedVersion.versionCode) {
                return null
            }

            val isRequired = manifest.forceUpdate || installedVersion.versionCode < manifest.minSupportedVersionCode
            if (!isRequired && preferences.dismissedVersionCode() == manifest.versionCode) {
                return null
            }

            AndroidAppUpdate(
                channel = updateChannel,
                versionCode = manifest.versionCode,
                currentVersionName = installedVersion.versionName,
                availableVersionName = manifest.versionName,
                apkUrl = manifest.apkUrl,
                sha256 = manifest.sha256,
                releasePageUrl = manifest.releasePageUrl,
                changelog = manifest.changelog.filter { it.isNotBlank() },
                isRequired = isRequired,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun dismissUpdate(context: Context, versionCode: Long) {
        AndroidUpdatePreferences(context).markDismissed(versionCode)
    }

    fun resetDismissedUpdate(context: Context) {
        AndroidUpdatePreferences(context).clearDismissed()
    }

    private suspend fun fetchUpdateManifest(channel: AndroidUpdateChannel): AndroidUpdateManifest? {
        createClient().use { client ->
            val responseText = client.get(AndroidUpdateChannelConfig.manifestUrl(channel)) {
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "itClinicApp-android-update-check")
                url {
                    parameters.append("t", System.currentTimeMillis().toString())
                }
            }.bodyAsText()
            return manifestJson.decodeFromString<AndroidUpdateManifest>(responseText)
        }
    }

    private fun createClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 15_000
            }
        }
    }

    private val manifestJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
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

    private fun resolveUpdateChannel(installedVersion: InstalledAndroidVersion): AndroidUpdateChannel {
        return if (installedVersion.versionName.startsWith("v")) {
            AndroidUpdateChannel.Stable
        } else {
            AndroidUpdateChannel.Beta
        }
    }
}

private object AndroidUpdateChannelConfig {
    const val repositoryOwner = "citec-spbu"
    const val repositoryName = "CiteC"
    private const val updatesBranch = "android-updates"

    fun manifestUrl(channel: AndroidUpdateChannel): String {
        return "https://raw.githubusercontent.com/$repositoryOwner/$repositoryName/$updatesBranch/${channel.manifestFileName}"
    }
}

enum class AndroidUpdateChannel(val manifestFileName: String) {
    Beta("beta.json"),
    Stable("stable.json"),
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

    fun dismissedVersionCode(): Long {
        return prefs.getLong(KEY_DISMISSED_VERSION_CODE, -1L)
    }

    fun markDismissed(versionCode: Long) {
        prefs.edit().putLong(KEY_DISMISSED_VERSION_CODE, versionCode).apply()
    }

    fun clearDismissed() {
        prefs.edit().remove(KEY_DISMISSED_VERSION_CODE).apply()
    }

    private companion object {
        private const val KEY_DISMISSED_VERSION_CODE = "dismissed_version_code"
    }
}

@Serializable
private data class AndroidUpdateManifest(
    val channel: String,
    val versionCode: Long,
    val versionName: String,
    val minSupportedVersionCode: Long = 0L,
    val forceUpdate: Boolean = false,
    val releaseTag: String,
    val apkUrl: String,
    val sha256: String,
    val sizeBytes: Long = 0L,
    val releasePageUrl: String,
    val changelog: List<String> = emptyList(),
    val commitSha: String? = null,
    val generatedAt: String? = null,
)
