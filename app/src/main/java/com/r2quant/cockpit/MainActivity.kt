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
            // ★ 마감 1: 촌스러운 file:// 팝업을 차단하고, 세련된 안드로이드 순정 알림(Toast)으로 변환
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                result?.confirm() // 백그라운드에서 확인 버튼을 자동 처리
                return true // 투박한 경고창을 화면에 띄우지 않음
            }
        }
        
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidVault")
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun saveApiKeys(appKey: String, appSecret: String, accountNum: String) {
            
            // ★ 마감 2: 필수 기입 누락 및 계좌번호 자리수(8자리) 검사 (오입력 방지)
            if (appKey.isBlank() || appSecret.isBlank() || accountNum.length != 8) {
                (context as AppCompatActivity).runOnUiThread {
                    Toast.makeText(context, "입력 오류: 계좌번호(8자리) 및 키 값을 정확히 확인해주세요.", Toast.LENGTH_LONG).show()
                }
                return
            }

            try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                val sharedPrefs = EncryptedSharedPreferences.create(
                    "R2_SECURE_VAULT",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                
                sharedPrefs.edit().apply {
                    putString("APP_KEY", appKey)
                    putString("APP_SECRET", appSecret)
                    putString("ACCOUNT_NUM", accountNum)
                    apply()
                }
                
                (context as AppCompatActivity).runOnUiThread {
                    Toast.makeText(context, "보안 금고에 성공적으로 장착되었습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                (context as AppCompatActivity).runOnUiThread {
                    Toast.makeText(context, "금고 저장 실패: 기기 보안 모듈 에러", Toast.LENGTH_SHORT).show()
                }
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
