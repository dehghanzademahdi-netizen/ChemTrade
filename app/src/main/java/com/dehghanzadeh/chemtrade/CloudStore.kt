package com.dehghanzadeh.chemtrade

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Dependency-free Firebase Realtime Database REST client for ChemLink data sync. */
object CloudStore {
    private const val SNAPSHOT = "chemlinkSnapshot"

    private fun baseUrl(): String = BuildConfig.CHEMLINK_FIREBASE_DB_URL.trimEnd('/')
    fun enabled(): Boolean = baseUrl().isNotBlank()

    private suspend fun request(method: String, path: String, body: String? = null): String? = withContext(Dispatchers.IO) {
        if (!enabled()) return@withContext null
        runCatching {
            val connection = (URL("${baseUrl()}/$path.json").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10000
                readTimeout = 15000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (body != null) doOutput = true
            }
            body?.let { connection.outputStream.use { out -> out.write(it.toByteArray(Charsets.UTF_8)) } }
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val result = stream?.bufferedReader()?.use { it.readText() }
            connection.disconnect()
            if (result.isNullOrBlank() || result == "null") null else result
        }.getOrNull()
    }

    suspend fun push(prefs: android.content.SharedPreferences): Boolean {
        val payload = JSONObject().apply {
            put("users", prefs.getString("users", "[]") ?: "[]")
            put("offers", prefs.getString("offers", "[]") ?: "[]")
            put("updatedAt", System.currentTimeMillis())
        }
        return request("PUT", SNAPSHOT, payload.toString()) != null
    }

    suspend fun pull(prefs: android.content.SharedPreferences): Boolean {
        val raw = request("GET", SNAPSHOT) ?: return false
        val payload = JSONObject(raw)
        val users = payload.optString("users", "[]")
        val offers = payload.optString("offers", "[]")
        runCatching { JSONArray(users); JSONArray(offers) }.getOrElse { return false }
        prefs.edit().putString("users", users).putString("offers", offers).apply()
        return true
    }
}
