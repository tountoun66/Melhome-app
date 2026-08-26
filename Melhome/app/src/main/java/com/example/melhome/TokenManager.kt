package com.example.melhome

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenManager {
    private const val PREFS_NAME = "secure_mel_prefs"
    private const val KEY_SESSION_COOKIE = "session_cookie"
    private const val KEY_ACCESS_TOKEN = "oauth_access_token"
    private const val KEY_REFRESH_TOKEN = "oauth_refresh_token"
    private const val KEY_EXPIRES_AT = "oauth_expires_at"
    private const val KEY_PENDING_STATE = "oauth_pending_state"
    private const val KEY_PENDING_VERIFIER = "oauth_pending_verifier"

    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(context: Context, token: String) {
        getEncryptedPrefs(context).edit().putString(KEY_SESSION_COOKIE, token).apply()
    }

    fun getToken(context: Context): String? =
        getEncryptedPrefs(context).getString(KEY_SESSION_COOKIE, null)

    fun saveOAuthSession(
        context: Context,
        accessToken: String,
        refreshToken: String,
        expiresAt: Long
    ) {
        getEncryptedPrefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun getAccessToken(context: Context): String? =
        getEncryptedPrefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(context: Context): String? =
        getEncryptedPrefs(context).getString(KEY_REFRESH_TOKEN, null)

    fun getExpiresAt(context: Context): Long =
        getEncryptedPrefs(context).getLong(KEY_EXPIRES_AT, 0L)

    fun hasOAuthSession(context: Context): Boolean =
        !getRefreshToken(context).isNullOrBlank()

    fun savePendingOAuth(context: Context, state: String, verifier: String) {
        getEncryptedPrefs(context).edit()
            .putString(KEY_PENDING_STATE, state)
            .putString(KEY_PENDING_VERIFIER, verifier)
            .apply()
    }

    fun getPendingOAuthState(context: Context): String? =
        getEncryptedPrefs(context).getString(KEY_PENDING_STATE, null)

    fun getPendingOAuthVerifier(context: Context): String? =
        getEncryptedPrefs(context).getString(KEY_PENDING_VERIFIER, null)

    fun clearPendingOAuth(context: Context) {
        getEncryptedPrefs(context).edit()
            .remove(KEY_PENDING_STATE)
            .remove(KEY_PENDING_VERIFIER)
            .apply()
    }

    fun clearOAuthSession(context: Context) {
        getEncryptedPrefs(context).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }
}
