package com.r2quant.cockpit

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        webView = WebView(this)
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.webViewClient = WebViewClient()
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                return super.onJsAlert(view, url, message, result)
            }
        }
        
        // ★ 핵심 추가: 웹뷰(조종석 UI)와 안드로이드 금고를 연결하는 데이터 파이프라인 개통
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidVault")
        
        webView.loadUrl("file:///android_asset/index.html")
    }

    // ★ 최신 AES-256-GCM 군사급 암호화 금고 로직 (BYOK 원칙)
    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun saveApiKeys(appKey: String, appSecret: String, accountNum: String) {
            try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                val sharedPrefs = EncryptedSharedPreferences.create(
                    "R2_SECURE_VAULT",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                
                // 폰 내부에 암호화하여 철저히 봉인
                sharedPrefs.edit().apply {
                    putString("APP_KEY", appKey)
                    putString("APP_SECRET", appSecret)
                    putString("ACCOUNT_NUM", accountNum)
                    apply()
                }
                
                Toast.makeText(context, "보안 금고에 안전하게 암호화되어 저장되었습니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "금고 저장 실패: 보안 모듈 오류", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
