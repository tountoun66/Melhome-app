package com.example.melhome

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class LoginActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // On purge les vieux cookies au démarrage de l'écran de login
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
                                checkLoginSuccess(url)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString()
                                checkLoginSuccess(url)
                                return false
                            }

                            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                super.doUpdateVisitedHistory(view, url, isReload)
                                checkLoginSuccess(url)
                            }
                        }

                        loadUrl("https://melcloudhome.com/")
                    }
                }
            )
        }
    }

    private fun checkLoginSuccess(url: String?) {
        if (url != null) {
            val isDashboard = url.contains("/dashboard", ignoreCase = true) || url.contains("#/dashboard", ignoreCase = true)
            val isUnits = url.contains("/units", ignoreCase = true) || url.contains("#/units", ignoreCase = true)
            val isHomePath = url.endsWith("/home", ignoreCase = true) || url.contains("/home/", ignoreCase = true)

            if (isDashboard || isUnits || isHomePath) {
                CookieManager.getInstance().flush()
                val cookie = CookieManager.getInstance().getCookie("https://melcloudhome.com")

                if (!cookie.isNullOrEmpty()) {
                    TokenManager.saveToken(this@LoginActivity, cookie)
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}