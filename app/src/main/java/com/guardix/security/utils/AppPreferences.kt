package com.guardix.security.utils

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object AppPreferences {

    private const val PREF_NAME = "GuardixAppPreferences"

    private fun getUserKey(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun getPreferences(context: Context) =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    fun isSetupComplete(context: Context): Boolean {
        val uid = getUserKey() ?: return false

        return getPreferences(context)
            .getBoolean("setup_complete_$uid", false)
    }

    fun setSetupComplete(
        context: Context,
        complete: Boolean
    ) {
        val uid = getUserKey() ?: return

        getPreferences(context)
            .edit()
            .putBoolean("setup_complete_$uid", complete)
            .apply()
    }

    fun isFaceRegistered(context: Context): Boolean {
        val uid = getUserKey() ?: return false

        return getPreferences(context)
            .getBoolean("face_registered_$uid", false)
    }

    fun setFaceRegistered(
        context: Context,
        registered: Boolean
    ) {
        val uid = getUserKey() ?: return

        getPreferences(context)
            .edit()
            .putBoolean("face_registered_$uid", registered)
            .apply()
    }

    fun clearUserSetup(context: Context) {
        val uid = getUserKey() ?: return

        getPreferences(context)
            .edit()
            .remove("setup_complete_$uid")
            .remove("face_registered_$uid")
            .apply()
    }
}