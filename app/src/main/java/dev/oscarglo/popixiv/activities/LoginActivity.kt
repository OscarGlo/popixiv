package dev.oscarglo.popixiv.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.oscarglo.popixiv.api.AuthApi
import dev.oscarglo.popixiv.ui.theme.AppTheme
import dev.oscarglo.popixiv.util.Pkce
import dev.oscarglo.popixiv.util.Prefs
import kotlinx.coroutines.runBlocking

class LoginActivity : ComponentActivity() {
    private val webClient: WebViewClient = object : WebViewClient() {
        @SuppressLint("ApplySharedPref")
        override fun onPageFinished(view: WebView, url: String) {
            if (url.startsWith("pixiv://account/login")) {
                Prefs.init(this@LoginActivity)

                val code = Uri.parse(url).getQueryParameter("code").toString()
                if (code.isBlank()) {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error retrieving login code from pixiv",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return
                }

                val res = runBlocking { AuthApi.instance.postAuthToken(code, Pkce.get().verify) }

                Prefs.PIXIV_ACCESS_TOKEN.set(res.access_token)
                Prefs.PIXIV_REFRESH_TOKEN.set(res.refresh_token)

                startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                AndroidView(factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = webClient
                    }
                }, update = { webView ->
                    webView.loadUrl(
                        "https://app-api.pixiv.net/web/v1/login" +
                                "?code_challenge=" + Pkce.get().challenge +
                                "&code_challenge_method=S256&client=pixiv-android"
                    )
                }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}