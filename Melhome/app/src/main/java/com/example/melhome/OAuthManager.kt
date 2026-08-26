package com.example.melhome

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

object OAuthManager {
    const val REDIRECT_URI = "melcloudhome://"
    const val CALLBACK_SCHEME = "melcloudhome"

    private const val AUTH_BASE = "https://auth.melcloudhome.com"
    private const val PAR_URL = "$AUTH_BASE/connect/par"
    private const val AUTHORIZE_URL = "$AUTH_BASE/connect/authorize"
    private const val TOKEN_URL = "$AUTH_BASE/connect/token"
    private const val CLIENT_ID = "monitorandcontrolhome"
    private const val SCOPE = "openid profile offline_access"

    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    data class AuthorizationRequest(val url: String, val state: String, val verifier: String)
    data class TokenResponse(val accessToken: String, val refreshToken: String, val expiresAt: Long)

    private fun base64Url(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun randomUrlSafe(bytes: Int = 32): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return base64Url(data)
    }

    private fun codeChallenge(verifier: String): String =
        base64Url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    suspend fun createAuthorizationRequest(context: Context): AuthorizationRequest = withContext(Dispatchers.IO) {
        val state = UUID.randomUUID().toString()
        val verifier = randomUrlSafe()
        val challenge = codeChallenge(verifier)

        val form = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("response_type", "code")
            .add("redirect_uri", REDIRECT_URI)
            .add("scope", SCOPE)
            .add("state", state)
            .add("code_challenge", challenge)
            .add("code_challenge_method", "S256")
            .add("nonce", state)
            .build()

        val request = Request.Builder()
            .url(PAR_URL)
            .post(form)
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("PAR OAuth HTTP ${response.code}: ${body.take(300)}")
            val requestUri = JSONObject(body).optString("request_uri")
            if (requestUri.isBlank()) throw IllegalStateException("PAR OAuth : request_uri absent")

            TokenManager.savePendingOAuth(context, state, verifier)
            val url = "$AUTHORIZE_URL?client_id=${Uri.encode(CLIENT_ID)}&request_uri=${Uri.encode(requestUri)}"
            AuthorizationRequest(url, state, verifier)
        }
    }

    suspend fun exchangeAuthorizationCode(context: Context, code: String, state: String?): TokenResponse = withContext(Dispatchers.IO) {
        val savedState = TokenManager.getPendingOAuthState(context) ?: throw IllegalStateException("État OAuth manquant")
        val verifier = TokenManager.getPendingOAuthVerifier(context) ?: throw IllegalStateException("PKCE verifier manquant")
        if (state.isNullOrBlank() || state != savedState) throw IllegalStateException("État OAuth invalide")

        val form = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", CLIENT_ID)
            .add("code", code)
            .add("redirect_uri", REDIRECT_URI)
            .add("code_verifier", verifier)
            .build()

        val request = Request.Builder().url(TOKEN_URL).post(form).addHeader("Accept", "application/json").build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IllegalStateException("Token OAuth HTTP ${response.code}: ${body.take(300)}")
            val json = JSONObject(body)
            val access = json.optString("access_token")
            val refresh = json.optString("refresh_token")
            if (access.isBlank() || refresh.isBlank()) throw IllegalStateException("Réponse OAuth incomplète")
            val expiresAt = System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L
            TokenManager.saveOAuthSession(context, access, refresh, expiresAt)
            TokenManager.clearPendingOAuth(context)
            TokenResponse(access, refresh, expiresAt)
        }
    }

    suspend fun syncWithRender(context: Context, tokens: TokenResponse): Boolean = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val serverUrl = (prefs.getString("render_server_url", "https://melhome-bridge.onrender.com") ?: "").trim().trimEnd('/')
        if (serverUrl.isBlank()) return@withContext false

        val expiresIn = ((tokens.expiresAt - System.currentTimeMillis()) / 1000L).coerceAtLeast(60L)
        val json = JSONObject().apply {
            put("access_token", tokens.accessToken)
            put("refresh_token", tokens.refreshToken)
            put("expires_in", expiresIn)
            put("expires_at", tokens.expiresAt)
        }

        val request = Request.Builder()
            .url("$serverUrl/api/save-oauth")
            .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), json.toString()))
            .build()

        client.newCall(request).execute().use { it.isSuccessful }
    }

    fun callbackUriFromIntent(uri: Uri?): Boolean = uri?.scheme.equals(CALLBACK_SCHEME, ignoreCase = true)
}
