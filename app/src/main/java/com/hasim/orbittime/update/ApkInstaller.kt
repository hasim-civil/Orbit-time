package com.hasim.orbittime.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a downloaded APK to Android's own package installer.
 *
 * Nothing here installs anything itself — it launches the standard system installation flow,
 * which shows the user the usual confirmation screen and is the only way a non-system app can
 * install a package. The file is shared through a [FileProvider] `content://` URI because a
 * raw `file://` URI has been rejected by the platform since Android 7.
 */
object ApkInstaller {

    /** Where downloaded APKs are kept: app-private external storage, needing no permission. */
    fun downloadDirectory(context: Context): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir, "updates")

    /** Removes any previously downloaded APKs so they don't accumulate across updates. */
    fun clearPreviousDownloads(context: Context) {
        runCatching {
            downloadDirectory(context).listFiles()
                ?.filter { it.isFile && it.name.endsWith(UpdateConfig.APK_EXTENSION, ignoreCase = true) }
                ?.forEach { it.delete() }
        }
    }

    /**
     * True when the user has already allowed this app to install packages. From Android 8 the
     * install intent is refused outright without it, so it is checked before launching rather
     * than letting the flow dead-end.
     */
    fun canRequestInstall(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    /**
     * Opens the system settings page where the user grants this app permission to install
     * apps. Returns false if the device has no such screen to open.
     */
    fun openInstallPermissionSettings(context: Context): Boolean = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@runCatching false
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    /**
     * Launches the install flow for [apk].
     *
     * @return null on success, or a message to show the user explaining why it couldn't start.
     */
    fun install(context: Context, apk: File): String? {
        if (!apk.exists() || apk.length() == 0L) {
            return "The downloaded update is missing. Please try again."
        }

        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        } catch (error: IllegalArgumentException) {
            return "Couldn't open the downloaded update."
        }

        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, UpdateConfig.APK_CONTENT_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            null
        } catch (error: Exception) {
            "Couldn't start the installer on this device."
        }
    }

    /** The version name this build was compiled with, read from the installed package itself. */
    fun installedVersionName(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()
}
