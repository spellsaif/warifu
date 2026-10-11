package org.warifu.platform.android.security

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Android operating system security hardening utilities.
 */
object SecurityHardening {

    private const val CLIPBOARD_PURGE_DELAY_MS = 30_000L // 30 seconds

    /**
     * Prevents screen recordings, screenshots, and task manager preview snapshots.
     * Must be called in Activity.onCreate() before setContentView().
     */
    fun enableFlagSecure(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    /**
     * Copies an OTP token to the system clipboard securely:
     * 1. Marks payload as `EXTRA_IS_SENSITIVE` on Android 13+ (disables clipboard toast overlay).
     * 2. Launches a background worker to sanitize the clipboard after 30 seconds.
     */
    fun copyTokenToClipboard(
        context: Context,
        token: String,
        label: String = "Warifu Token"
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(label, token)

        // Android 13+ (API 33) sensitive content flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clipData.description.extras = extras
        }

        clipboard.setPrimaryClip(clipData)

        // Schedule auto-purge after 30 seconds
        CoroutineScope(Dispatchers.Main).launch {
            delay(CLIPBOARD_PURGE_DELAY_MS)
            purgeClipboardIfUnchanged(clipboard, token)
        }
    }

    /**
     * Overwrites clipboard with empty contents ONLY if the clipboard still contains the original token.
     * Prevents accidentally wiping unrelated user data if they copied something else afterwards.
     */
    private fun purgeClipboardIfUnchanged(
        clipboard: ClipboardManager,
        originalToken: String
    ) {
        val currentClip = clipboard.primaryClip
        if (currentClip != null && currentClip.itemCount > 0) {
            val currentText = currentClip.getItemAt(0).text?.toString()
            if (currentText == originalToken) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            }
        }
    }
}
