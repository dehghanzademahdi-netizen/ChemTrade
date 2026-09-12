from pathlib import Path
import re

PKG = Path('app/src/main/java/com/dehghanzadeh/chemtrade')
M = PKG / 'MainActivity.kt'
E = PKG / 'EntryActivity.kt'
C = PKG / 'CloudStore.kt'

# 1) Replace the admin login dialog with the same runtime SEND_SMS permission
# flow used by the customer login. Password is checked first, then Android asks
# for SEND_SMS, and only after permission is granted is the OTP sent.
m = M.read_text(encoding='utf-8')
start = m.find('@Composable\nprivate fun AdminLoginDialog')
if start < 0:
    raise SystemExit('AdminLoginDialog not found')
next_pos = m.find('\n@Composable', start + 10)
if next_pos < 0:
    raise SystemExit('AdminLoginDialog end not found')
admin = r'''@Composable
private fun AdminLoginDialog(onClose: () -> Unit, onSuccess: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    fun sendAdminOtp() {
        val code = SecureRandom().nextInt(900000).plus(100000).toString()
        expected = code
        val sms = "ChemLink | کد ورود مدیریت: $code\nاین کد را در اختیار دیگران قرار ندهید."
        try {
            @Suppress("DEPRECATION")
            val manager = SmsManager.getDefault()
            val parts = manager.divideMessage(sms)
            if (parts.size == 1) manager.sendTextMessage(ADMIN_PHONE, null, sms, null, null)
            else manager.sendMultipartTextMessage(ADMIN_PHONE, null, parts, null, null)
            step = 1
            message = "کد ورود مدیریت ارسال شد."
            error = ""
        } catch (e: Exception) {
            error = "ارسال پیامک ناموفق بود: ${e.message ?: "خطای سیم‌کارت یا مجوز SMS"}"
        }
        sending = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendAdminOtp()
        else {
            sending = false
            error = "برای ورود مدیریت باید مجوز ارسال SMS را فعال کنید."
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (step == 0) "ورود مدیریت" else "تأیید ورود مدیریت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step == 0) {
                    Text("رمز مدیریت را وارد کنید.")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        singleLine = true,
                        label = { Text("رمز مدیریت") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                } else {
                    Text("کد تأیید به شماره مدیریت ارسال شد.")
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = digits(it).filter(Char::isDigit).take(6) },
                        singleLine = true,
                        label = { Text("کد پیامکی") }
                    )
                }
                if (message.isNotBlank()) Text(message, color = Green)
                if (error.isNotBlank()) Text(error, color = Red)
            }
        },
        confirmButton = {
            Button(enabled = !sending, onClick = {
                if (step == 0) {
                    if (hash(password) != ADMIN_HASH) {
                        error = "رمز مدیریت صحیح نیست."
                        return@Button
                    }
                    error = ""
                    message = ""
                    sending = true
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                        sendAdminOtp()
                    } else {
                        permissionLauncher.launch(Manifest.permission.SEND_SMS)
                    }
                } else {
                    if (otp == expected && expected.isNotBlank()) onSuccess()
                    else error = "کد واردشده صحیح نیست."
                }
            }) { Text(if (step == 0) "ادامه" else "ورود به مدیریت") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("انصراف") } }
    )
}
'''
m = m[:start] + admin + m[next_pos:]
M.write_text(m, encoding='utf-8')

# 2) Customer must verify by SMS every login, but an existing account must never
# ask for its profile again. Keep the last phone number prefilled for convenience.
e = E.read_text(encoding='utf-8')
old = '''        val prefs = getSharedPreferences("chemlink", Context.MODE_PRIVATE)\n        if (prefs.getString("phone", "").orEmpty().isNotBlank()) {\n            startActivity(Intent(this, MainActivity::class.java))\n            finish()\n            return\n        }\n        setContent { ChemLinkEntryApp() }'''
new = '''        val prefs = getSharedPreferences("chemlink", Context.MODE_PRIVATE)\n        if (CloudStore.enabled()) runBlocking { CloudStore.pull(prefs) }\n        setContent { ChemLinkEntryApp() }'''
if old in e:
    e = e.replace(old, new, 1)
else:
    raise SystemExit('EntryActivity session bypass block not found')
old = 'var phone by rememberSaveable { mutableStateOf("") }'
new = 'var phone by rememberSaveable { mutableStateOf(context.getSharedPreferences("chemlink", Context.MODE_PRIVATE).getString("phone", "").orEmpty()) }'
if old in e:
    e = e.replace(old, new, 1)
# Existing accounts should be restored from cloud/local and go straight to the app
# after OTP verification; only genuinely new accounts see ProfileScreen.
E.write_text(e, encoding='utf-8')

# 3) Make the Firebase snapshot merge instead of last-writer-wins replacement.
# This prevents one phone/device from deleting other customers or their ads.
c = C.read_text(encoding='utf-8')
start = c.find('object CloudStore {')
if start < 0:
    raise SystemExit('CloudStore object not found')
header = c[:start]
body = r'''object CloudStore {
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

    private fun array(value: String?): JSONArray = runCatching { JSONArray(value ?: "[]") }.getOrDefault(JSONArray())

    private fun mergeUsers(remote: JSONArray, local: JSONArray): JSONArray {
        val map = linkedMapOf<String, JSONObject>()
        for (i in 0 until remote.length()) remote.optJSONObject(i)?.let { item ->
            val phone = item.optString("phone")
            if (phone.isNotBlank()) map[phone] = item
        }
        for (i in 0 until local.length()) local.optJSONObject(i)?.let { item ->
            val phone = item.optString("phone")
            if (phone.isNotBlank()) map[phone] = item
        }
        val out = JSONArray()
        map.values.forEach { out.put(it) }
        return out
    }

    private fun offerKey(item: JSONObject): String {
        val phone = item.optString("phone").ifBlank { item.optString("owner") }
        return phone + "|" + item.optInt("id", 0) + "|" + item.optLong("createdAt", 0L) + "|" + item.optString("name")
    }

    private fun mergeOffers(remote: JSONArray, local: JSONArray): JSONArray {
        val map = linkedMapOf<String, JSONObject>()
        for (i in 0 until remote.length()) remote.optJSONObject(i)?.let { map[offerKey(it)] = it }
        for (i in 0 until local.length()) local.optJSONObject(i)?.let { map[offerKey(it)] = it }
        val out = JSONArray()
        map.values.forEach { out.put(it) }
        return out
    }

    suspend fun push(prefs: android.content.SharedPreferences): Boolean {
        if (!enabled()) return false
        val remote = request("GET", SNAPSHOT)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val mergedUsers = mergeUsers(array(remote?.optString("users", "[]")), array(prefs.getString("users", "[]")))
        val mergedOffers = mergeOffers(array(remote?.optString("offers", "[]")), array(prefs.getString("offers", "[]")))
        val payload = JSONObject().apply {
            put("users", mergedUsers.toString())
            put("offers", mergedOffers.toString())
            put("updatedAt", System.currentTimeMillis())
        }
        val ok = request("PUT", SNAPSHOT, payload.toString()) != null
        if (ok) prefs.edit().putString("users", mergedUsers.toString()).putString("offers", mergedOffers.toString()).apply()
        return ok
    }

    suspend fun pull(prefs: android.content.SharedPreferences): Boolean {
        val raw = request("GET", SNAPSHOT) ?: return false
        val payload = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        val mergedUsers = mergeUsers(array(payload.optString("users", "[]")), array(prefs.getString("users", "[]")))
        val mergedOffers = mergeOffers(array(payload.optString("offers", "[]")), array(prefs.getString("offers", "[]")))
        prefs.edit().putString("users", mergedUsers.toString()).putString("offers", mergedOffers.toString()).apply()
        return true
    }
}
'''
C.write_text(header + body, encoding='utf-8')
print('PRODUCTION PERSISTENCE + ADMIN OTP PATCH OK')
