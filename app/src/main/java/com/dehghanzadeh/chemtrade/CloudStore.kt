package com.dehghanzadeh.chemtrade

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CloudStore {
    private const val SNAPSHOT = "chemlinkSnapshot"
    private fun baseUrl(): String = BuildConfig.CHEMLINK_FIREBASE_DB_URL.trimEnd('/')
    fun enabled(): Boolean = baseUrl().isNotBlank()

    private suspend fun request(method: String, path: String, body: String? = null): String? = withContext(Dispatchers.IO) {
        if (!enabled()) return@withContext null
        runCatching {
            val c = (URL("${baseUrl()}/$path.json").openConnection() as HttpURLConnection).apply {
                requestMethod = method; connectTimeout = 10000; readTimeout = 15000
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (body != null) doOutput = true
            }
            body?.let { c.outputStream.use { out -> out.write(it.toByteArray(Charsets.UTF_8)) } }
            val stream = if (c.responseCode in 200..299) c.inputStream else c.errorStream
            val result = stream?.bufferedReader()?.use { it.readText() }
            c.disconnect()
            if (result.isNullOrBlank() || result == "null") null else result
        }.getOrNull()
    }

    private fun arr(value: String?): JSONArray = runCatching { JSONArray(value ?: "[]") }.getOrDefault(JSONArray())

    private fun mergeUsers(remote: JSONArray, local: JSONArray): JSONArray {
        val map = linkedMapOf<String, JSONObject>()
        for (i in 0 until remote.length()) remote.optJSONObject(i)?.let { o -> if (o.optString("phone").isNotBlank()) map[o.optString("phone")] = o }
        for (i in 0 until local.length()) local.optJSONObject(i)?.let { o -> if (o.optString("phone").isNotBlank()) map[o.optString("phone")] = o }
        return JSONArray().also { out -> map.values.forEach(out::put) }
    }

    private fun offerKey(o: JSONObject): String = o.optString("phone").ifBlank { o.optString("owner") } + "|" + o.optInt("id") + "|" + o.optLong("createdAt") + "|" + o.optString("name")

    private fun mergeOffers(remote: JSONArray, local: JSONArray): JSONArray {
        val map = linkedMapOf<String, JSONObject>()
        for (i in 0 until remote.length()) remote.optJSONObject(i)?.let { map[offerKey(it)] = it }
        for (i in 0 until local.length()) local.optJSONObject(i)?.let { map[offerKey(it)] = it }
        return JSONArray().also { out -> map.values.forEach(out::put) }
    }

    suspend fun push(prefs: android.content.SharedPreferences): Boolean {
        if (!enabled()) return false
        val remote = request("GET", SNAPSHOT)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val users = mergeUsers(arr(remote?.optString("users")), arr(prefs.getString("users", "[]")))
        val offers = mergeOffers(arr(remote?.optString("offers")), arr(prefs.getString("offers", "[]")))
        val payload = JSONObject().apply { put("users", users.toString()); put("offers", offers.toString()); put("updatedAt", System.currentTimeMillis()) }
        val ok = request("PUT", SNAPSHOT, payload.toString()) != null
        if (ok) prefs.edit().putString("users", users.toString()).putString("offers", offers.toString()).apply()
        return ok
    }

    suspend fun pull(prefs: android.content.SharedPreferences): Boolean {
        val raw = request("GET", SNAPSHOT) ?: return false
        val payload = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        val users = mergeUsers(arr(payload.optString("users")), arr(prefs.getString("users", "[]")))
        val offers = mergeOffers(arr(payload.optString("offers")), arr(prefs.getString("offers", "[]")))
        prefs.edit().putString("users", users.toString()).putString("offers", offers.toString()).apply()
        return true
    }
}
