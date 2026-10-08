package com.hasim.orbittime.update

import android.content.Context

/**
 * Remembers which release the user answered "Later" to, so that release isn't offered again on
 * every launch. Only that release is muted: a newer one is offered as usual.
 */
object UpdatePromptStore {

    private const val PREFS_NAME = "orbit_update_prompt"
    private const val KEY_DISMISSED_TAG = "dismissed_release_tag"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun dismissedTag(context: Context): String? = prefs(context).getString(KEY_DISMISSED_TAG, null)

    fun rememberDismissed(context: Context, tag: String) {
        prefs(context).edit().putString(KEY_DISMISSED_TAG, tag).apply()
    }

    fun shouldPrompt(latestTag: String, dismissedTag: String?): Boolean = shouldPromptForRelease(latestTag, dismissedTag)
}
