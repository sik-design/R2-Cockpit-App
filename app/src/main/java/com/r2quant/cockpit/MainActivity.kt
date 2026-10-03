package com.r2quant.cockpit

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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

        // ★ 마감 1: 텔레그램 등 외부 링크를 스마트폰 진짜 앱으로 튕겨서 열어주는 로직
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                
                // 링크가 텔레그램(t.me)이거나 안드로이드 앱 호출(intent)일 경우
                if (url.startsWith("intent://") || url.startsWith("tg://") || url.contains("t.me")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true // 폰 시스템(외부)으로 보냈으므로 웹뷰 자체 처리는 중단
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "텔레그램 앱이 폰에 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
                        return true
                    }
                }
                return false // 일반 내부 화면 이동은 그대로 진행
            }
        }
        
        // ★ 마감 2: 촌스러운 file:// 하얀 경고창을 차단하고, 세련된 하단 알림으로 변환
        webView.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                result?.confirm() // 백그라운드에서 확인 버튼 자동 클릭 처리
                return true 
            }
        }
        
        webView.addJavascriptInterface(WebAppInterface(this), "AndroidVault")
        webView.loadUrl("file:///android_asset/index.html")
    }

    inner class WebAppInterface(private val context: Context) {
        @JavascriptInterface
        fun saveApiKeys(appKey: String, appSecret: String, accountNum: String) {
            
            // ★ 마감 3: 빈칸이거나 계좌번호가 8자리가 아니면 금고 저장을 튕겨냄 (오입력 치명타 방어)
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
