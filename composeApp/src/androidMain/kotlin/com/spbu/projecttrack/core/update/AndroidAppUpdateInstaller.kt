package com.spbu.projecttrack.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.spbu.projecttrack.core.settings.localizeRuntime
import io.ktor.client.call.body
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object AndroidAppUpdateInstaller {
    suspend fun installUpdate(context: Context, update: AndroidAppUpdate) {
        ensurePackageInstallerPermission(context)

        val apkFile = downloadAndVerifyUpdate(context, update)
        openSystemInstaller(context, apkFile)
    }

    private suspend fun downloadAndVerifyUpdate(context: Context, update: AndroidAppUpdate): File {
        val updatesDir = File(context.cacheDir, "android_updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "itclinicapp-${update.channel.name.lowercase()}-${update.versionCode}.apk")

        return withContext(Dispatchers.IO) {
            if (targetFile.exists()) {
                targetFile.delete()
            }

            createClient().use { client ->
                val apkBytes = client.get(update.apkUrl).body<ByteArray>()
                targetFile.writeBytes(apkBytes)
            }

            val actualSha256 = sha256(targetFile)
            if (!actualSha256.equals(update.sha256, ignoreCase = true)) {
                targetFile.delete()
                throw AndroidUpdateInstallException(
                    localizeRuntime(
                        russian = "Не удалось проверить загруженное обновление. Попробуйте ещё раз.",
                        english = "Failed to verify the downloaded update. Please try again.",
                    )
                )
            }

            targetFile
        }
    }

    private fun openSystemInstaller(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(installIntent)
    }

    private fun ensurePackageInstallerPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        if (context.packageManager.canRequestPackageInstalls()) {
            return
        }

        val settingsIntent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(settingsIntent)

        throw AndroidUpdateInstallException(
            localizeRuntime(
                russian = "Разрешите установку из этого приложения и повторите обновление.",
                english = "Allow installs from this app and try the update again.",
            )
        )
    }

    private fun createClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
                connectTimeoutMillis = 20_000
                socketTimeoutMillis = 120_000
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) {
                    break
                }
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
}

class AndroidUpdateInstallException(message: String) : IllegalStateException(message)
