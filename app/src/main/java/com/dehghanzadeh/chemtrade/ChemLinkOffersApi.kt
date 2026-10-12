package com.dehghanzadeh.chemtrade

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object ChemLinkOffersApi {
    private const val ROOT = "https://chemlink.shop/api"

    private suspend fun request(path: String, method: String = "GET", token: String = "", body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = (URL(ROOT + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 20000
                readTimeout = 20000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
                setRequestProperty("Pragma", "no-cache")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
                if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
            }
            try {
                if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val result = runCatching { JSONObject(raw) }.getOrElse { JSONObject().put("ok", false).put("error", "پاسخ سرور قابل‌خواندن نیست.") }
                if (status !in 200..299 || !result.optBoolean("ok")) {
                    throw IllegalStateException(result.optString("error").ifBlank { "ارتباط با سرور ChemLink برقرار نشد." })
                }
                result
            } finally {
                connection.disconnect()
            }
        }

    suspend fun publicOffers() = request("/offers?_=" + System.currentTimeMillis())
    suspend fun myOffers(token: String) = request("/offers?mine=1&_=" + System.currentTimeMillis(), token = token)
    suspend fun createOrUpdate(token: String, offer: JSONObject) = request("/offers", "POST", token, offer)
    suspend fun adminOffers(token: String) = request("/admin/offers?_=" + System.currentTimeMillis(), token = token)
    suspend fun adminCreate(token: String, offer: JSONObject) = request("/admin/offers", "POST", token, offer)
    suspend fun updateAdmin(token: String, id: String, status: String, marginType: String = "PERCENT", margin: Double = 0.0, reason: String = "") =
        request("/admin/offers", "POST", token, JSONObject().put("id", id).put("status", status).put("marginType", marginType).put("margin", margin).put("reason", reason))
}
