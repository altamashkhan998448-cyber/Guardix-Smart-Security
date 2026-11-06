package com.guardix.security.security

import android.content.Context
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinManager {

    private const val PREF_NAME = "GuardixPinSecurity"
    private const val ITERATIONS = 120000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    private fun getPreferences(context: Context) =
        context.getSharedPreferences(
            PREF_NAME,
            Context.MODE_PRIVATE
        )

    private fun getUserKey(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {

        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )

        return try {
            SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun hasPin(context: Context): Boolean {

        val uid = getUserKey() ?: return false

        val preferences = getPreferences(context)

        return preferences.contains("${uid}_hash") &&
                preferences.contains("${uid}_salt")
    }

    fun savePin(context: Context, pin: String): Boolean {

        val uid = getUserKey() ?: return false

        if (!pin.matches(Regex("^\\d{4,8}$"))) {
            return false
        }

        return try {

            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)

            val hash = hashPin(pin, salt)

            getPreferences(context).edit()
                .putString(
                    "${uid}_salt",
                    Base64.encodeToString(salt, Base64.NO_WRAP)
                )
                .putString(
                    "${uid}_hash",
                    Base64.encodeToString(hash, Base64.NO_WRAP)
                )
                .commit()

        } catch (e: Exception) {
            false
        }
    }

    fun verifyPin(context: Context, enteredPin: String): Boolean {

        val uid = getUserKey() ?: return false

        if (!enteredPin.matches(Regex("^\\d{4,8}$"))) {
            return false
        }

        return try {

            val preferences = getPreferences(context)

            val saltString = preferences.getString(
                "${uid}_salt",
                null
            ) ?: return false

            val hashString = preferences.getString(
                "${uid}_hash",
                null
            ) ?: return false

            val salt = Base64.decode(
                saltString,
                Base64.NO_WRAP
            )

            val savedHash = Base64.decode(
                hashString,
                Base64.NO_WRAP
            )

            val enteredHash = hashPin(enteredPin, salt)

            MessageDigest.isEqual(
                savedHash,
                enteredHash
            )

        } catch (e: Exception) {
            false
        }
    }

    fun deletePin(context: Context): Boolean {

        val uid = getUserKey() ?: return false

        return getPreferences(context).edit()
            .remove("${uid}_salt")
            .remove("${uid}_hash")
            .commit()
    }
}