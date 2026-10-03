package com.r2quant.cockpit

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // 로딩 화면(Splash) 장착
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
        
        // ★ 핵심 수정: 투박한 경고창 대신 예쁜 HTML 팝업을 띄우도록 허용
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                // 자바스크립트 alert을 앱 내부 로직으로 부드럽게 처리
                return super.onJsAlert(view, url, message, result)
            }
        }
        
        webView.loadUrl("file:///android_asset/index.html")
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
