package com.example.melhome

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var oauthStarted = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthIntent(intent)

        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!handleOAuthCallback(url)) checkLoginSuccess(view, url)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString()
                                if (handleOAuthCallback(url)) return true
                                checkLoginSuccess(view, url)
                                return false
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                if (!handleOAuthCallback(url)) checkLoginSuccess(view, url)
                            }
                        }
                        loadUrl("https://melcloudhome.com/")
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (OAuthManager.callbackUriFromIntent(uri)) handleOAuthCallback(uri.toString())
    }

    private fun isDashboardUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.contains("/dashboard", true) || url.contains("#/dashboard", true) ||
            url.contains("/units", true) || url.contains("#/units", true) ||
            url.endsWith("/home", true) || url.contains("/home/", true)
    }

    private fun checkLoginSuccess(view: WebView?, url: String?) {
        if (!isDashboardUrl(url)) return
        CookieManager.getInstance().flush()
        val cookie = CookieManager.getInstance().getCookie("https://melcloudhome.com")
        if (cookie.isNullOrEmpty()) return

        TokenManager.saveToken(this, cookie)
        if (!oauthStarted && !TokenManager.hasOAuthSession(this)) {
            oauthStarted = true
            scope.launch {
                try {
                    val auth = OAuthManager.createAuthorizationRequest(this@LoginActivity)
                    view?.loadUrl(auth.url)
                } catch (e: Exception) {
                    oauthStarted = false
                    Toast.makeText(this@LoginActivity, "OAuth indisponible : ${e.message}", Toast.LENGTH_LONG).show()
                    openMainActivity()
                }
            }
        } else if (!oauthStarted) {
            openMainActivity()
        }
    }

    private fun handleOAuthCallback(url: String?): Boolean {
        if (url.isNullOrBlank() || !url.startsWith("${OAuthManager.CALLBACK_SCHEME}://", true)) return false
        val uri = try { Uri.parse(url) } catch (_: Exception) { return true }
        val code = uri.getQueryParameter("code")
        val state = uri.getQueryParameter("state")
        if (code.isNullOrBlank()) {
            oauthStarted = false
            openMainActivity()
            return true
        }

        scope.launch {
            try {
                val tokens = OAuthManager.exchangeAuthorizationCode(this@LoginActivity, code, state)
                val synced = OAuthManager.syncWithRender(this@LoginActivity, tokens)
                Toast.makeText(this@LoginActivity, if (synced) "Session OAuth synchronisée" else "OAuth OK, Render non synchronisé", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "OAuth non finalisé : ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                oauthStarted = false
                openMainActivity()
            }
        }
        return true
    }

    private fun openMainActivity() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
