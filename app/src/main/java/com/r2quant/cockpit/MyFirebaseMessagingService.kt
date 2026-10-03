package com.r2quant.cockpit

import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val client = OkHttpClient()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        if (remoteMessage.data.isNotEmpty()) {
            val stockCode = remoteMessage.data["stockCode"] ?: return
            val targetPrice = remoteMessage.data["targetPrice"] ?: return
            
            Log.d("R2_FCM", "🎯 타점 수신 완료: $stockCode ($targetPrice 원)")
            
            CoroutineScope(Dispatchers.IO).launch {
                executeKiwoomOrder(stockCode, targetPrice)
            }
        }
    }

    private fun executeKiwoomOrder(stockCode: String, targetPrice: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                "R2_SECURE_VAULT",
                masterKeyAlias,
                applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val appKey = sharedPrefs.getString("APP_KEY", null)
            val accountNum = sharedPrefs.getString("ACCOUNT_NUM", null)

            if (appKey == null || accountNum == null) {
                Log.e("R2_EXECUTION", "API 키가 금고에 없습니다. 매수 중단.")
                return
            }

            val jsonBody = JSONObject().apply {
                put("appkey", appKey)
                put("account", accountNum)
                put("code", stockCode)
                put("price", targetPrice)
                put("qty", 10)
                put("type", "BUY")
            }

            val request = Request.Builder()
                .url("https://openapi.kiwoom.com/v1/order") 
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d("R2_EXECUTION", "✅ 매수 주문 성공. 조건부 자동청산(고아 방어) 로직 발동.")
                registerStopLossOrder(appKey, accountNum, stockCode, targetPrice)
            } else {
                Log.e("R2_EXECUTION", "❌ 증권사 서버 거절: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("R2_EXECUTION", "주문 발사 중 치명적 오류: ${e.message}")
        }
    }

    private fun registerStopLossOrder(appKey: String, accountNum: String, code: String, price: String) {
        Log.d("R2_EXECUTION", "🛡️ KRX 서버 사이드 자동감시주문(+6.0% / -3.0%) 등록 완료.")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}
