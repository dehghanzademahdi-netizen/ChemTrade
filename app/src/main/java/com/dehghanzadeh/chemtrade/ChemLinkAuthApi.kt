package com.dehghanzadeh.chemtrade

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object ChemLinkAuthApi {
    private const val ROOT = "https://chemlink.shop/api/auth"

    private suspend fun request(path: String, method: String = "GET", token: String = "", body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = (URL(ROOT + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val result = runCatching { JSONObject(text) }.getOrElse { JSONObject().put("ok", false).put("error", "پاسخ سرور قابل‌خواندن نیست.") }
                if (connection.responseCode !in 200..299 || !result.optBoolean("ok")) {
                    throw IllegalStateException(result.optString("error").ifBlank { "ارتباط با سرور ChemLink برقرار نشد." })
                }
                result
            } finally {
                connection.disconnect()
            }
        }

    suspend fun sendOtp(phone: String, purpose: String) =
        request("/send-otp", "POST", body = JSONObject().put("phone", phone).put("purpose", purpose))

    suspend fun verifyOtp(phone: String, code: String, purpose: String) =
        request("/verify-otp", "POST", body = JSONObject().put("phone", phone).put("code", code).put("purpose", purpose))

    suspend fun me(token: String) = request("/me", token = token)

    suspend fun profile(token: String) = request("/profile", token = token)

    suspend fun saveProfile(token: String, profile: JSONObject) =
        request("/profile", "POST", token = token, body = profile)
}
