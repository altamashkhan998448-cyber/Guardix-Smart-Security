package com.guardix.security.face

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class FaceStorage(context: Context) {

    companion object {
        private const val PREFS = "guardix_face_storage"
        private const val KEY_ALIAS = "guardix_face_encryption_key"
        private const val DATA_KEY = "registered_face_embedding"
        private const val IV_KEY = "registered_face_iv"
        private const val EMBEDDING_SIZE = 192
    }

    private val preferences =
        context.applicationContext.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

    private fun getSecretKey(): SecretKey {

        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val existingKey = keyStore.getKey(KEY_ALIAS, null)

        if (existingKey is SecretKey) {
            return existingKey
        }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )

        return generator.generateKey()
    }

    @Synchronized
    fun saveFaceEmbedding(embedding: FloatArray): Boolean {

        if (
            embedding.size != EMBEDDING_SIZE ||
            embedding.any { !it.isFinite() }
        ) {
            return false
        }

        return try {

            val buffer = ByteBuffer.allocate(
                embedding.size * 4
            ).order(ByteOrder.LITTLE_ENDIAN)

            embedding.forEach {
                buffer.putFloat(it)
            }

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            cipher.init(
                Cipher.ENCRYPT_MODE,
                getSecretKey()
            )

            val encrypted = cipher.doFinal(buffer.array())

            preferences.edit()
                .putString(
                    DATA_KEY,
                    Base64.encodeToString(
                        encrypted,
                        Base64.NO_WRAP
                    )
                )
                .putString(
                    IV_KEY,
                    Base64.encodeToString(
                        cipher.iv,
                        Base64.NO_WRAP
                    )
                )
                .commit()

        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun getFaceEmbedding(): FloatArray? {

        return try {

            val encryptedString =
                preferences.getString(DATA_KEY, null)
                    ?: return null

            val ivString =
                preferences.getString(IV_KEY, null)
                    ?: return null

            val encrypted = Base64.decode(
                encryptedString,
                Base64.NO_WRAP
            )

            val iv = Base64.decode(
                ivString,
                Base64.NO_WRAP
            )

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")

            cipher.init(
                Cipher.DECRYPT_MODE,
                getSecretKey(),
                GCMParameterSpec(128, iv)
            )

            val decrypted = cipher.doFinal(encrypted)

            if (decrypted.size != EMBEDDING_SIZE * 4) {
                return null
            }

            val buffer = ByteBuffer.wrap(decrypted)
                .order(ByteOrder.LITTLE_ENDIAN)

            FloatArray(EMBEDDING_SIZE) {
                buffer.float
            }.takeIf { values ->
                values.all { it.isFinite() } &&
                    values.any { it != 0f }
            }

        } catch (_: Exception) {
            null
        }
    }

    fun hasRegisteredFace(): Boolean {
        return preferences.contains(DATA_KEY) &&
            preferences.contains(IV_KEY)
    }

    @Synchronized
    fun deleteFace(): Boolean {

        return preferences.edit()
            .remove(DATA_KEY)
            .remove(IV_KEY)
            .commit()
    }
}