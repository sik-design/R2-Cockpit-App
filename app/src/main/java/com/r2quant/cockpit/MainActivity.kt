package com.r2quant.cockpit

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {
    private var isReady = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 네이티브 스플래시(로딩) 화면 장착
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // 2. 1.5초 동안 로딩 화면 유지 (시스템 파일 불러오는 연출)
        splashScreen.setKeepOnScreenCondition { !isReady }
        Handler(Looper.getMainLooper()).postDelayed({
            isReady = true
        }, 1500)

        // 3. 웹뷰 화면 세팅 (R2 조종석)
        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            
            // 로컬에 저장된 HTML 불러오기
            loadUrl("file:///android_asset/r2_cockpit.html")
        }
        setContentView(webView)
    }
}
