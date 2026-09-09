package com.dehghanzadeh.chemtrade

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small dependency-free Firebase Realtime Database REST client.
 *
 * Set CHEMLINK_FIREBASE_DB_URL in the build environment (for example
 * https://your-project-default-rtdb.firebaseio.com). Until it is configured,
 * the app safely falls back to the local cache.
 */
object CloudStore {
    private const val PREFS = "chemlink"
    private const val DB_URL = "CHEMLINK_FIREBASE_DB_URL"
    private const val USERS = "users"
    private const val OFFERS = "offers"

    private fun baseUrl(): String = BuildConfig.CHEMLINK_FIREBASE_DB_URL.trimEnd('/')
    fun enabled(): Boolean = baseUrl().isNotBlank() && !baseUrl().contains("CHEMLINK_FIREBASE_DB_URL")

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

    suspend fun saveUser(userJson: JSONObject): Boolean = request("PUT", "$USERS/${safe(userJson.optString("phone"))}", userJson.toString()) != null

    suspend fun loadUser(phone: String): JSONObject? = request("GET", "$USERS/${safe(phone)}")?.let { JSONObject(it) }

    suspend fun loadUsers(): List<JSONObject> = request("GET", USERS)?.let { raw ->
        val root = JSONObject(raw)
        root.keys().asSequence().mapNotNull { key -> root.optJSONObject(key) }.toList()
    } ?: emptyList()

    suspend fun saveOffer(id: String, offerJson: JSONObject): Boolean = request("PUT", "$OFFERS/${safe(id)}", offerJson.toString()) != null

    suspend fun loadOffers(): List<JSONObject> = request("GET", OFFERS)?.let { raw ->
        val root = JSONObject(raw)
        root.keys().asSequence().mapNotNull { key -> root.optJSONObject(key) }.toList()
    } ?: emptyList()

    suspend fun deleteOffer(id: String): Boolean = request("DELETE", "$OFFERS/${safe(id)}") != null

    private fun safe(value: String): String = value.replace(".", "_dot_").replace("#", "_hash_").replace("$", "_dollar_").replace("[", "_lb_").replace("]", "_rb_").replace("/", "_slash_")

    fun userToJson(phone: String, type: String, name: String, company: String, address: String = "", city: String = "", landline: String = ""): JSONObject = JSONObject().apply {
        put("phone", phone); put("type", type); put("name", name); put("company", company)
        put("address", address); put("city", city); put("landline", landline)
        put("updatedAt", System.currentTimeMillis())
    }
}
