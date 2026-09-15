package com.hasim.orbittime.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Everything the update dialog needs to draw itself. */
data class UpdateUiState(
    val release: GithubRelease? = null,
    val isDownloading: Boolean = false,
    /** 0f..1f, or null while the size is unknown (an indeterminate bar). */
    val progress: Float? = null,
    val errorMessage: String? = null,
    val dismissed: Boolean = false,
) {
    /** The dialog is on screen only for a real, undismissed update. */
    val isVisible: Boolean get() = release != null && !dismissed
}

/**
 * Owns the startup update check and the download that follows it.
 *
 * Scoped to the activity, so the check in [init] runs exactly once per app launch: recomposing
 * a screen, switching bottom-nav tabs, navigating between destinations or rotating the device
 * all reuse this instance rather than starting a second check or raising a second dialog. The
 * check itself is launched into [viewModelScope] and never awaited by the UI, so startup is not
 * blocked and a slow or unreachable GitHub only ever means the dialog doesn't appear.
 */
class UpdateViewModel @JvmOverloads constructor(
    application: Application,
    private val checker: UpdateChecker = UpdateChecker(),
    private val downloader: ApkDownloader = ApkDownloader(),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            val installed = ApkInstaller.installedVersionName(getApplication<Application>())
            when (val result = checker.check(installed)) {
                // Up to date, offline, rate-limited or unreadable release data: show nothing.
                is UpdateCheck.None -> Unit
                is UpdateCheck.Available -> _uiState.update { it.copy(release = result.release) }
            }
        }
    }

    /** "Later" — hides the dialog for this app launch and stops any download in flight. */
    fun dismiss() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(dismissed = true, isDownloading = false, progress = null) }
    }

    /**
     * "Update now" — downloads the APK attached to the detected release and, once it lands,
     * launches the system installer. No browser, no release page, no version to pick: the
     * asset URL was already resolved by the check.
     */
    fun downloadAndInstall() {
        val state = _uiState.value
        val release = state.release ?: return
        val apk = release.apkAsset ?: return
        // Guard against a double tap starting a second transfer over the first.
        if (state.isDownloading) return

        val context = getApplication<Application>()
        if (!ApkInstaller.canRequestInstall(context)) {
            // Android 8+ refuses the install intent outright without this, so the user is sent
            // to the settings screen that grants it rather than watching "Update now" do nothing.
            _uiState.update {
                it.copy(errorMessage = "Allow Orbit Time to install apps, then tap Update now again.")
            }
            ApkInstaller.openInstallPermissionSettings(context)
            return
        }

        _uiState.update { it.copy(isDownloading = true, progress = 0f, errorMessage = null) }

        downloadJob = viewModelScope.launch {
            withContext(Dispatchers.IO) { ApkInstaller.clearPreviousDownloads(context) }
            val destination = File(
                ApkInstaller.downloadDirectory(context),
                UpdateConfig.downloadFileName(release.tag),
            )

            val result = downloader.download(
                url = apk.downloadUrl,
                destination = destination,
                expectedBytes = apk.sizeBytes,
                onProgress = { fraction -> _uiState.update { it.copy(progress = fraction) } },
            )

            when (result) {
                is DownloadResult.Success -> {
                    val error = ApkInstaller.install(context, result.file)
                    _uiState.update {
                        it.copy(
                            isDownloading = false,
                            progress = null,
                            errorMessage = error,
                            // The system installer is now in front of the user; leaving the
                            // dialog up behind it would greet them again when they return,
                            // whether they completed the install or backed out of it.
                            dismissed = error == null,
                        )
                    }
                }
                is DownloadResult.Failure ->
                    _uiState.update { it.copy(isDownloading = false, progress = null, errorMessage = result.reason) }
            }
            downloadJob = null
        }
    }
}
