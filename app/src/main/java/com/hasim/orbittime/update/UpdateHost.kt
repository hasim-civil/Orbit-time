package com.hasim.orbittime.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hasim.orbittime.ui.components.OrbitUpdateDialog

/**
 * Drops the update dialog over whatever is on screen.
 *
 * Placed once at the root of the activity, above the navigation graph, so the check belongs to
 * the app launch rather than to a screen: it survives navigating between destinations and
 * switching tabs, and the [UpdateViewModel] behind it is created once, which is what keeps a
 * second dialog from ever appearing. Renders nothing at all unless a genuinely newer release
 * with a downloadable APK was found.
 */
@Composable
fun UpdateHost(viewModel: UpdateViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val release = state.release

    if (!state.isVisible || release == null) return

    val context = LocalContext.current
    // Read once rather than on every recomposition — the installed version can't change while
    // the app is running.
    val installed = remember(context) { ApkInstaller.installedVersionName(context) }

    OrbitUpdateDialog(
        versionLabel = displayVersion(release.tag),
        currentVersionLabel = installed?.let { displayVersion(it) },
        releaseNotes = release.notes,
        isDownloading = state.isDownloading,
        progress = state.progress,
        errorMessage = state.errorMessage,
        onUpdateNow = viewModel::downloadAndInstall,
        onLater = viewModel::dismiss,
    )
}
