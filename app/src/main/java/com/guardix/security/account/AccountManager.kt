package com.guardix.security.account

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AccountManager {

    private const val PREFS = "GuardixAccounts"
    private const val ITERATIONS = 120000
    private const val KEY_LENGTH = 256

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun hashPassword(
        password: String,
        salt: ByteArray
    ): String {

        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )

        val factory = SecretKeyFactory.getInstance(
            "PBKDF2WithHmacSHA256"
        )

        val hash = factory.generateSecret(spec).encoded
        spec.clearPassword()

        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun createAccount(
        context: Context,
        name: String,
        email: String,
        phone: String,
        password: String
    ): String {

        if (name.isBlank()) return "Please enter your full name."

        if (!android.util.Patterns.EMAIL_ADDRESS
                .matcher(email.trim()).matches()
        ) return "Please enter a valid email."

        if (!phone.matches(Regex("^[0-9]{10}$"))) {
            return "Mobile number must contain exactly 10 digits."
        }

        if (password.length < 8) {
            return "Password must be at least 8 characters."
        }

        val p = prefs(context)
        val normalizedEmail = email.trim().lowercase()

        if (p.contains("account_$normalizedEmail")) {
            return "An account with this email already exists."
        }

        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)

        val hash = hashPassword(password, salt)

        val saved = p.edit()
            .putString("account_$normalizedEmail", hash)
            .putString("salt_$normalizedEmail",
                Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("name_$normalizedEmail", name.trim())
            .putString("phone_$normalizedEmail", phone)
            .putString("current_email", normalizedEmail)
            .commit()

        return if (saved) "SUCCESS" else "Account creation failed."
    }

    fun signIn(
        context: Context,
        email: String,
        password: String
    ): Boolean {

        val normalizedEmail = email.trim().lowercase()
        val p = prefs(context)

        val savedHash = p.getString(
            "account_$normalizedEmail", null
        ) ?: return false

        val saltString = p.getString(
            "salt_$normalizedEmail", null
        ) ?: return false

        val salt = Base64.decode(saltString, Base64.NO_WRAP)

        val enteredHash = hashPassword(password, salt)

        if (savedHash != enteredHash) return false

        return p.edit()
            .putString("current_email", normalizedEmail)
            .commit()
    }

    fun isSignedIn(context: Context): Boolean {
        return prefs(context).getString(
            "current_email", null
        ) != null
    }

    fun getCurrentEmail(context: Context): String? {
        return prefs(context).getString(
            "current_email", null
        )
    }

    fun getCurrentName(context: Context): String? {
        val email = getCurrentEmail(context) ?: return null
        return prefs(context).getString("name_$email", null)
    }

    fun signOut(context: Context) {
        prefs(context).edit()
            .remove("current_email")
            .apply()
    }
}