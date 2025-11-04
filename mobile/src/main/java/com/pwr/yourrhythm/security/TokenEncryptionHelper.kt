package com.pwr.yourrhythm.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

object TokenEncryptionHelper {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "TokenKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

        keyStore.getKey(KEY_ALIAS, null)?.let {
            return it as SecretKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    fun saveTokens(context: Context, accessToken: String?, refreshToken: String?) {
        val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("access_token", encrypt(accessToken))
            putString("refresh_token", encrypt(refreshToken))
            apply()
        }
    }

    fun saveAccessToken(context: Context, refreshToken: String?) {
        val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("access_token", encrypt(refreshToken))
            apply()
        }
    }

    fun getAccessToken(context: Context): String? {
        val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        val encrypted = prefs.getString("access_token", null) ?: return null
        return decrypt(encrypted)
    }

    fun getRefreshToken(context: Context): String? {
        val prefs = context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        val encrypted = prefs.getString("refresh_token", null) ?: return null
        return decrypt(encrypted)
    }

    fun encrypt(data: String?): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data?.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.DEFAULT)
    }

    fun decrypt(data: String): String {
        val decoded = Base64.decode(data, Base64.DEFAULT)
        val iv = decoded.copyOfRange(0, 12)
        val encrypted = decoded.copyOfRange(12, decoded.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
