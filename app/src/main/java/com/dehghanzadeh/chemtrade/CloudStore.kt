package com.dehghanzadeh.chemtrade

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CloudStore {
    private const val ROOT = "chemlink"
    private fun baseUrl(): String = BuildConfig.CHEMLINK_FIREBASE_DB_URL.trimEnd('/')
    fun enabled(): Boolean = baseUrl().isNotBlank()

    private suspend fun request(method: String, path: String, body: String? = null): String? = withContext(Dispatchers.IO) {
        if (!enabled()) return@withContext null
        runCatching {
            val c = (URL("${baseUrl()}/$path.json").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10000
                readTimeout = 15000
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

    private fun array(value: String?): JSONArray = runCatching { JSONArray(value ?: "[]") }.getOrDefault(JSONArray())

    private fun mergeByKey(remote: JSONArray, local: JSONArray, key: (JSONObject) -> String): JSONArray {
        val map = linkedMapOf<String, JSONObject>()
        for (i in 0 until remote.length()) remote.optJSONObject(i)?.let { o -> key(o).takeIf { it.isNotBlank() }?.let { map[it] = o } }
        for (i in 0 until local.length()) local.optJSONObject(i)?.let { o -> key(o).takeIf { it.isNotBlank() }?.let { map[it] = o } }
        return JSONArray().also { out -> map.values.forEach(out::put) }
    }

    private fun offerKey(o: JSONObject): String = listOf(o.optString("phone").ifBlank { o.optString("owner") }, o.optString("id"), o.optString("createdAt"), o.optString("name")).joinToString("|")

    private fun snapshotPayload(users: JSONArray, offers: JSONArray): JSONObject = JSONObject().apply {
        put("users", users)
        put("offers", offers)
        put("updatedAt", System.currentTimeMillis())
    }

    /** Uploads the complete local cache without deleting anything already on the server. */
    suspend fun push(prefs: SharedPreferences): Boolean {
        if (!enabled()) return false
        val remote = request("GET", ROOT)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val users = mergeByKey(array(remote?.optString("users")), array(prefs.getString("users", "[]"))) { it.optString("phone") }
        val offers = mergeByKey(array(remote?.optString("offers")), array(prefs.getString("offers", "[]")), ::offerKey)
        return request("PUT", ROOT, snapshotPayload(users, offers).toString()) != null
    }

    /** Downloads the server copy and merges it into the phone cache; server data wins on conflicts. */
    suspend fun pull(prefs: SharedPreferences): Boolean {
        if (!enabled()) return false
        val raw = request("GET", ROOT) ?: return false
        val payload = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        val users = mergeByKey(array(payload.optString("users")), array(prefs.getString("users", "[]"))) { it.optString("phone") }
        val offers = mergeByKey(array(payload.optString("offers")), array(prefs.getString("offers", "[]")), ::offerKey)
        prefs.edit().putString("users", users.toString()).putString("offers", offers.toString()).apply()
        return true
    }

    suspend fun saveUser(prefs: SharedPreferences, userJson: JSONObject): Boolean {
        if (!enabled()) return false
        val phone = userJson.optString("phone")
        if (phone.isBlank()) return false
        val existing = request("GET", "$ROOT/usersByPhone/${encode(phone)}")?.let { runCatching { JSONObject(it) }.getOrNull() }
        val merged = existing?.let { mergeObjects(it, userJson) } ?: userJson
        return request("PUT", "$ROOT/usersByPhone/${encode(phone)}", merged.toString()) != null
    }

    suspend fun loadUser(prefs: SharedPreferences, phone: String): JSONObject? {
        if (!enabled() || phone.isBlank()) return null
        return request("GET", "$ROOT/usersByPhone/${encode(phone)}")?.let { runCatching { JSONObject(it) }.getOrNull() }
    }

    private fun mergeObjects(remote: JSONObject, local: JSONObject): JSONObject {
        val out = JSONObject(remote.toString())
        val keys = local.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = local.opt(k)
            if (v != null && v != JSONObject.NULL && v.toString().isNotBlank()) out.put(k, v)
        }
        return out
    }

    private fun encode(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8.name())
}
