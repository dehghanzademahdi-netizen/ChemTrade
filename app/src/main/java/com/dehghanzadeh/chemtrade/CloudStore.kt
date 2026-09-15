package com.dehghanzadeh.chemtrade

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object CloudStore {
    private const val ROOT = "chemlink"
    private const val USERS = "users"
    private const val USERS_BY_PHONE = "usersByPhone"
    private const val OFFERS = "offers"
    private const val DB_URL = "https://chemlink-8909b-default-rtdb.firebaseio.com"

    private fun baseUrl(): String = DB_URL
    fun enabled(): Boolean = true

    private data class Response(val code: Int, val body: String?)

    private suspend fun request(method: String, path: String, body: String? = null): Response = withContext(Dispatchers.IO) {
        runCatching {
            val c = (URL("${baseUrl()}/$path.json").openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15000
                readTimeout = 20000
                useCaches = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                if (body != null) doOutput = true
            }
            body?.let { c.outputStream.use { out -> out.write(it.toByteArray(Charsets.UTF_8)) } }
            val code = c.responseCode
            val stream = if (code in 200..299) c.inputStream else c.errorStream
            val result = stream?.bufferedReader()?.use { it.readText() }
            c.disconnect()
            Response(code, result)
        }.getOrElse { Response(-1, null) }
    }

    private fun array(value: String?): JSONArray = runCatching { JSONArray(value ?: "[]") }.getOrDefault(JSONArray())
    private fun encode(value: String): String = URLEncoder.encode(value.filter { it.isDigit() }, Charsets.UTF_8.name())
    private fun offerKey(o: JSONObject): String = listOf(o.optString("phone").ifBlank { o.optString("owner") }, o.optString("id"), o.optString("createdAt"), o.optString("name")).joinToString("|")

    suspend fun saveUser(prefs: SharedPreferences, userJson: JSONObject): Boolean {
        val phone = userJson.optString("phone").filter { it.isDigit() }
        if (phone.isBlank()) return false
        val path = "$ROOT/$USERS_BY_PHONE/${encode(phone)}"
        val existingResponse = request("GET", path)
        val existing = if (existingResponse.code in 200..299) existingResponse.body?.let { runCatching { JSONObject(it) }.getOrNull() } else null
        val merged = JSONObject(existing?.toString() ?: "{}")
        val keys = userJson.keys()
        while (keys.hasNext()) { val k = keys.next(); merged.put(k, userJson.opt(k)) }

        val put = request("PUT", path, merged.toString())
        if (put.code !in 200..299) return false

        val verify = request("GET", path)
        if (verify.code !in 200..299) return false
        val verified = verify.body?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return false
        if (verified.optString("phone").filter { it.isDigit() } != phone) return false

        val usersResponse = request("GET", "$ROOT/$USERS")
        val users = array(if (usersResponse.code in 200..299) usersResponse.body else null)
        var found = false
        for (i in 0 until users.length()) {
            val u = users.optJSONObject(i) ?: continue
            if (u.optString("phone").filter { it.isDigit() } == phone) { users.put(i, verified); found = true; break }
        }
        if (!found) users.put(verified)
        request("PUT", "$ROOT/$USERS", users.toString())
        return true
    }

    suspend fun loadUser(prefs: SharedPreferences, phone: String): JSONObject? {
        val normalized = phone.filter { it.isDigit() }
        if (normalized.isBlank()) return null
        val direct = request("GET", "$ROOT/$USERS_BY_PHONE/${encode(normalized)}")
        if (direct.code in 200..299) return direct.body?.let { runCatching { JSONObject(it) }.getOrNull() }
        val usersResponse = request("GET", "$ROOT/$USERS")
        val users = array(if (usersResponse.code in 200..299) usersResponse.body else null)
        for (i in 0 until users.length()) {
            val u = users.optJSONObject(i) ?: continue
            if (u.optString("phone").filter { it.isDigit() } == normalized) return u
        }
        return null
    }

    suspend fun push(prefs: SharedPreferences): Boolean {
        val localUsers = array(prefs.getString(USERS, "[]"))
        val localOffers = array(prefs.getString(OFFERS, "[]"))
        val remoteResponse = request("GET", ROOT)
        val remote = if (remoteResponse.code in 200..299) remoteResponse.body?.let { runCatching { JSONObject(it) }.getOrNull() } else null
        val remoteUsers = array(remote?.optString(USERS))
        val remoteOffers = array(remote?.optString(OFFERS))
        val mergedUsers = merge(remoteUsers, localUsers) { it.optString("phone").filter { c -> c.isDigit() } }
        val mergedOffers = merge(remoteOffers, localOffers, ::offerKey)
        val usersByPhone = JSONObject()
        for (i in 0 until mergedUsers.length()) {
            val u = mergedUsers.optJSONObject(i) ?: continue
            val p = u.optString("phone").filter { it.isDigit() }
            if (p.isNotBlank()) usersByPhone.put(p, u)
        }
        val payload = JSONObject().apply { put(USERS, mergedUsers); put(USERS_BY_PHONE, usersByPhone); put(OFFERS, mergedOffers); put("updatedAt", System.currentTimeMillis()) }
        val result = request("PUT", ROOT, payload.toString())
        return result.code in 200..299
    }

    suspend fun pull(prefs: SharedPreferences): Boolean {
        val response = request("GET", ROOT)
        if (response.code !in 200..299) return false
        val payload = response.body?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return false
        val byPhone = payload.optJSONObject(USERS_BY_PHONE)
        val users = if (byPhone != null) JSONArray().also { out -> val keys = byPhone.keys(); while (keys.hasNext()) { byPhone.optJSONObject(keys.next())?.let(out::put) } } else array(payload.optString(USERS))
        val offers = array(payload.optString(OFFERS))
        prefs.edit().putString(USERS, users.toString()).putString(OFFERS, offers.toString()).apply()
        return true
    }

    private fun merge(remote: JSONArray, local: JSONArray, key: (JSONObject) -> String): JSONArray {
        val map = linkedMapOf<String, JSONObject>()
        for (i in 0 until remote.length()) remote.optJSONObject(i)?.let { o -> key(o).takeIf { it.isNotBlank() }?.let { map[it] = o } }
        for (i in 0 until local.length()) local.optJSONObject(i)?.let { o -> key(o).takeIf { it.isNotBlank() }?.let { map[it] = o } }
        return JSONArray().also { out -> map.values.forEach(out::put) }
    }
}
