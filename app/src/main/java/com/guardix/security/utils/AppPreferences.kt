package com.guardix.security.utils

import android.content.Context
import com.guardix.security.account.AccountManager

object AppPreferences {

    private const val PREF_NAME = "GuardixAppPreferences"
    private const val DEMO_KEY = "demo_mode"

    private fun getPreferences(context: Context) =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    fun setDemoMode(context: Context, enabled: Boolean) {
        getPreferences(context).edit()
            .putBoolean(DEMO_KEY, enabled)
            .apply()
    }

    fun isDemoMode(context: Context): Boolean {
        return getPreferences(context)
            .getBoolean(DEMO_KEY, false)
    }

    private fun getUserKey(context: Context): String? {
        if (isDemoMode(context)) return "demo"

        return AccountManager.getCurrentEmail(context)
            ?.replace(".", "_")
            ?.replace("@", "_at_")
    }

    fun isSetupComplete(context: Context): Boolean {
        val key = getUserKey(context) ?: return false

        return getPreferences(context)
            .getBoolean("setup_complete_$key", false)
    }

    fun setSetupComplete(context: Context, complete: Boolean) {
        val key = getUserKey(context) ?: return

        getPreferences(context).edit()
            .putBoolean("setup_complete_$key", complete)
            .apply()
    }

    fun isFaceRegistered(context: Context): Boolean {
        val key = getUserKey(context) ?: return false

        return getPreferences(context)
            .getBoolean("face_registered_$key", false)
    }

    fun setFaceRegistered(context: Context, registered: Boolean) {
        val key = getUserKey(context) ?: return

        getPreferences(context).edit()
            .putBoolean("face_registered_$key", registered)
            .apply()
    }

    fun clearUserSetup(context: Context) {
        val key = getUserKey(context) ?: return

        getPreferences(context).edit()
            .remove("setup_complete_$key")
            .remove("face_registered_$key")
            .apply()
    }
}