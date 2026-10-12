package com.dehghanzadeh.chemtrade

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class EntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("chemlink", Context.MODE_PRIVATE)
        val adminToken = prefs.getString("adminAuthToken", "").orEmpty()
        val userToken = prefs.getString("authToken", "").orEmpty()
        val savedToken = adminToken.ifBlank { userToken }
        if (savedToken.isBlank()) {
            setContent { ChemLinkEntryApp() }
            return
        }
        setContent {
            var checkingSavedSession by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) {
                try {
                    val session = ChemLinkAuthApi.me(savedToken)
                    val purpose = session.optString("purpose")
                    val expectedPurpose = if (adminToken.isNotBlank()) "admin" else "user"
                    if (purpose == expectedPurpose) {
                        startActivity(Intent(this@EntryActivity, MainActivity::class.java))
                        finish()
                    } else {
                        prefs.edit().remove("authToken").remove("adminAuthToken").remove("chemlinkSession").apply()
                        checkingSavedSession = false
                    }
                } catch (_: Exception) {
                    prefs.edit().remove("authToken").remove("adminAuthToken").remove("chemlinkSession").apply()
                    checkingSavedSession = false
                }
            }
            if (checkingSavedSession) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                ChemLinkEntryApp()
            }
        }
    }
}

private val EntryNavy = Color(0xFF0B1F33)
private val EntryGold = Color(0xFFC89618)
private val EntryCream = Color(0xFFFBF8F2)
private val EntryGreen = Color(0xFF2E7D5B)
private val EntryRed = Color(0xFFB3261E)

private fun normalizeDigits(value: String): String = value.map { c ->
    when (c) {
        in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar()
        in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar()
        else -> c
    }
}.joinToString("")

private fun normalizeIranPhone(value: String): String {
    val p = normalizeDigits(value).filter(Char::isDigit)
    return when {
        p.startsWith("0098") -> "0" + p.drop(4).removePrefix("0")
        p.startsWith("98") && p.length == 12 -> "0" + p.drop(2)
        p.length == 10 && p.startsWith("9") -> "0$p"
        else -> p
    }
}

private data class EntryUser(val phone: String, val type: String, val name: String = "", val company: String = "", val address: String = "", val city: String = "", val landline: String = "")

private fun loadRegisteredUser(context: Context, phone: String): EntryUser? {
    val prefs = context.getSharedPreferences("chemlink", Context.MODE_PRIVATE)
    val old = runCatching { JSONArray(prefs.getString("users", "[]") ?: "[]") }.getOrDefault(JSONArray())
    for (i in 0 until old.length()) {
        val item = old.optJSONObject(i) ?: continue
        if (normalizeIranPhone(item.optString("phone")) == phone) return EntryUser(phone, item.optString("type", "Consumer"), item.optString("name"), item.optString("company"), item.optString("address"), item.optString("city"), item.optString("landline"))
    }
    return null
}

private fun saveRegisteredUser(context: Context, user: EntryUser) {
    val prefs = context.getSharedPreferences("chemlink", Context.MODE_PRIVATE)
    val old = runCatching { JSONArray(prefs.getString("users", "[]") ?: "[]") }.getOrDefault(JSONArray())
    val users = JSONArray(); var found = false
    for (i in 0 until old.length()) {
        val item = old.optJSONObject(i) ?: continue
        if (normalizeIranPhone(item.optString("phone")) == user.phone) { users.put(user.toJson()); found = true } else users.put(item)
    }
    if (!found) users.put(user.toJson())
    prefs.edit().putString("users", users.toString()).apply()
}
private fun EntryUser.toJson() = JSONObject().apply {
    put("phone", phone); put("type", type); put("name", name); put("company", company)
    put("address", address); put("city", city); put("landline", landline)
}

@Composable private fun ChemLinkEntryApp() {
    val colors = lightColorScheme(primary = EntryNavy, secondary = EntryGold, background = EntryCream, surface = Color.White, onPrimary = Color.White, onBackground = EntryNavy)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { MaterialTheme(colorScheme = colors) { EntryFlow() } }
}

@Composable private fun EntryFlow() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("buyer") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var companyName by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var businessType by rememberSaveable { mutableStateOf("") }
    var neededMaterials by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var landline by rememberSaveable { mutableStateOf("") }
    var busy by rememberSaveable { mutableStateOf(false) }

    fun finishLogin(profile: JSONObject?) {
        val p = profile ?: JSONObject()
        val savedRole = if (p.optString("role") == "supplier") "Supplier" else "Consumer"
        saveRegisteredUser(context, EntryUser(phone, savedRole, p.optString("fullName"), p.optString("companyName"), p.optString("address"), p.optString("city"), p.optString("landline")))
        context.getSharedPreferences("chemlink", Context.MODE_PRIVATE).edit()
            .putString("phone", phone).putString("type", savedRole).putString("authToken", token).putString("chemlinkSession", token).apply()
        context.startActivity(Intent(context, MainActivity::class.java))
        (context as? ComponentActivity)?.finish()
    }

    fun requestOtp() {
        phone = normalizeIranPhone(phone)
        if (!Regex("^09[0-9]{9}$").matches(phone)) { error = "لطفاً شماره موبایل معتبر ۱۱ رقمی وارد کنید."; return }
        scope.launch {
            busy = true; error = ""; message = "در حال ارسال کد تأیید..."
            try { ChemLinkAuthApi.sendOtp(phone, "user"); step = 2; code = ""; message = "کد تأیید به شماره شما پیامک شد." }
            catch (e: Exception) { error = e.message ?: "ارسال کد انجام نشد." }
            finally { busy = false }
        }
    }

    fun verifyOtp() {
        val normalizedCode = normalizeDigits(code).filter(Char::isDigit).take(6)
        if (normalizedCode.length != 6) { error = "کد ۶ رقمی را وارد کنید."; return }
        scope.launch {
            busy = true; error = ""; message = "در حال تأیید کد..."
            try {
                val auth = ChemLinkAuthApi.verifyOtp(phone, normalizedCode, "user")
                token = auth.getString("token")
                context.getSharedPreferences("chemlink", Context.MODE_PRIVATE).edit().putString("authToken", token).putString("chemlinkSession", token).apply()
                val response = ChemLinkAuthApi.profile(token)
                val profile = response.optJSONObject("profile")
                if (response.optBoolean("registered") && profile != null) finishLogin(profile)
                else {
                    val legacy = loadRegisteredUser(context, phone)
                    // Migrate complete legacy phone-local profiles to the cloud automatically,
                    // so the website can reuse them without asking the user to type everything again.
                    if (profile == null && legacy != null &&
                        legacy.name.trim().length >= 2 &&
                        legacy.company.trim().length >= 2 &&
                        legacy.city.trim().length >= 2
                    ) {
                        val legacyRole = if (legacy.type == "Supplier") "supplier" else "buyer"
                        val migrationPayload = JSONObject()
                            .put("fullName", legacy.name.trim())
                            .put("companyName", legacy.company.trim())
                            .put("role", legacyRole)
                            .put("city", legacy.city.trim())
                            .put("businessType", "فعالیت بازرگانی")
                            .put("address", legacy.address)
                            .put("landline", legacy.landline)
                            .put("neededMaterials", "")
                        val migrated = ChemLinkAuthApi.saveProfile(token, migrationPayload)
                        val migratedProfile = migrated.optJSONObject("profile")
                        if (migrated.optBoolean("registered") && migratedProfile != null) {
                            finishLogin(migratedProfile)
                            return@launch
                        }
                    }
                    fullName = profile?.optString("fullName").orEmpty().ifBlank { legacy?.name.orEmpty() }
                    companyName = profile?.optString("companyName").orEmpty().ifBlank { legacy?.company.orEmpty() }
                    city = profile?.optString("city").orEmpty().ifBlank { legacy?.city.orEmpty() }
                    role = profile?.optString("role").orEmpty().ifBlank { if (legacy?.type == "Supplier") "supplier" else "buyer" }
                    businessType = profile?.optString("businessType").orEmpty().ifBlank { if (legacy != null) "فعالیت بازرگانی" else "" }
                    neededMaterials = profile?.optString("neededMaterials").orEmpty()
                    address = profile?.optString("address").orEmpty().ifBlank { legacy?.address.orEmpty() }
                    landline = profile?.optString("landline").orEmpty().ifBlank { legacy?.landline.orEmpty() }
                    step = 3; message = "شماره تأیید شد. اطلاعات حساب را تکمیل کنید."
                }
            } catch (e: Exception) { error = e.message ?: "تأیید کد انجام نشد." }
            finally { busy = false }
        }
    }

    fun saveProfile() {
        if (fullName.trim().length < 2 || companyName.trim().length < 2 || city.trim().length < 2 || businessType.trim().length < 2 || role !in listOf("buyer", "supplier")) {
            error = "نام، نام شرکت، نوع حساب، شهر و زمینه فعالیت را کامل کنید."; return
        }
        scope.launch {
            busy = true; error = ""; message = "در حال ذخیره اطلاعات..."
            try {
                val payload = JSONObject().put("fullName", fullName.trim()).put("companyName", companyName.trim())
                    .put("role", role).put("city", city.trim()).put("businessType", businessType.trim())
                    .put("neededMaterials", neededMaterials.trim()).put("address", address.trim()).put("landline", landline.trim())
                val result = ChemLinkAuthApi.saveProfile(token, payload)
                finishLogin(result.optJSONObject("profile"))
            } catch (e: Exception) { error = e.message ?: "ذخیره اطلاعات انجام نشد." }
            finally { busy = false }
        }
    }

    Surface(Modifier.fillMaxSize(), color = EntryCream) {
        when (step) {
            0 -> WelcomeScreen { step = 1 }
            1 -> ServiceLoginScreen(phone, { phone = it }, { requestOtp() }, error, message, busy)
            2 -> VerifyCodeScreen(phone, code, { code = normalizeDigits(it).filter(Char::isDigit).take(6) }, { verifyOtp() }, { requestOtp() }, { step = 1; error = ""; message = "" }, error, message, busy)
            else -> ProfileScreen(fullName, companyName, role, city, businessType, neededMaterials, address, landline,
                { fullName = it }, { companyName = it }, { role = it }, { city = it }, { businessType = it }, { neededMaterials = it }, { address = it }, { landline = normalizeDigits(it).filter(Char::isDigit).take(11) },
                { saveProfile() }, error, message, busy)
        }
    }
}

@Composable private fun BrandLogo(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.ic_chemtrade_logo), contentDescription = "ChemLink", modifier = modifier, contentScale = ContentScale.Fit)
}
@Composable private fun WelcomeScreen(onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BrandLogo(Modifier.size(116.dp)); Spacer(Modifier.height(22.dp))
        Text("ChemLink", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = EntryNavy)
        Spacer(Modifier.height(8.dp)); Text("پل هوشمند تأمین و فروش مواد اولیه", style = MaterialTheme.typography.titleMedium, color = EntryGold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp)); Text("بازار حرفه‌ای برای ارتباط مطمئن تأمین‌کنندگان، فروشندگان و مصرف‌کنندگان مواد اولیه", textAlign = TextAlign.Center, color = EntryNavy)
        Spacer(Modifier.height(36.dp)); Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) { Text("ورود به ChemLink", fontWeight = FontWeight.Bold) }
    }
}
@Composable private fun ServiceLoginScreen(phone: String, onPhoneChange: (String) -> Unit, onContinue: () -> Unit, error: String, message: String, busy: Boolean) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BrandLogo(Modifier.size(86.dp)); Spacer(Modifier.height(18.dp))
        Text("ورود / ثبت‌نام", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = EntryNavy)
        Spacer(Modifier.height(8.dp)); Text("شماره موبایل را وارد کنید. کد از طریق پیامک رسمی ChemLink ارسال می‌شود.", color = EntryNavy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp)); OutlinedTextField(phone, onPhoneChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("شماره موبایل") }, leadingIcon = { Icon(Icons.Filled.Phone, null) })
        Spacer(Modifier.height(18.dp)); Button(onClick = onContinue, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text(if (busy) "لطفاً صبر کنید..." else "دریافت کد پیامکی") }
        if (message.isNotBlank()) Text(message, color = EntryGreen, textAlign = TextAlign.Center)
        if (error.isNotBlank()) Text(error, color = EntryRed, textAlign = TextAlign.Center)
    }
}
@Composable private fun VerifyCodeScreen(phone: String, code: String, onCodeChange: (String) -> Unit, onVerify: () -> Unit, onResend: () -> Unit, onBack: () -> Unit, error: String, message: String, busy: Boolean) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BrandLogo(Modifier.size(86.dp)); Spacer(Modifier.height(24.dp))
        Text("تأیید کد پیامکی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = EntryNavy)
        Spacer(Modifier.height(10.dp)); Text("کد ارسال‌شده برای $phone را وارد کنید.", textAlign = TextAlign.Center, color = EntryNavy)
        Spacer(Modifier.height(22.dp)); OutlinedTextField(code, onCodeChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("کد ۶ رقمی") })
        if (message.isNotBlank()) Text(message, color = EntryGreen, textAlign = TextAlign.Center)
        if (error.isNotBlank()) Text(error, color = EntryRed, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp)); Button(onClick = onVerify, enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) { Text(if (busy) "در حال بررسی..." else "تأیید و ادامه") }
        TextButton(onClick = onResend, enabled = !busy) { Text("ارسال مجدد کد") }
        TextButton(onClick = onBack) { Text("ویرایش شماره") }
    }
}
@Composable private fun ProfileScreen(
    name: String, company: String, role: String, city: String, businessType: String, neededMaterials: String, address: String, landline: String,
    onName: (String) -> Unit, onCompany: (String) -> Unit, onRole: (String) -> Unit, onCity: (String) -> Unit,
    onBusinessType: (String) -> Unit, onNeededMaterials: (String) -> Unit, onAddress: (String) -> Unit, onLandline: (String) -> Unit,
    onSave: () -> Unit, error: String, message: String, busy: Boolean
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BrandLogo(Modifier.size(76.dp)); Text("تکمیل اطلاعات حساب", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = EntryNavy)
        Text("این اطلاعات در سایت و اپلیکیشن مشترک ذخیره می‌شود.", color = EntryNavy, textAlign = TextAlign.Center)
        OutlinedTextField(name, onName, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("نام و نام خانوادگی") })
        OutlinedTextField(company, onCompany, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("نام شرکت / مجموعه") })
        Text("نوع حساب", modifier = Modifier.fillMaxWidth(), color = EntryNavy)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = role == "buyer", onClick = { onRole("buyer") }, label = { Text("خریدار / مصرف‌کننده") })
            FilterChip(selected = role == "supplier", onClick = { onRole("supplier") }, label = { Text("تأمین‌کننده / فروشنده") })
        }
        OutlinedTextField(city, onCity, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("شهر") })
        OutlinedTextField(businessType, onBusinessType, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("زمینه فعالیت") })
        OutlinedTextField(neededMaterials, onNeededMaterials, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("مواد اولیه موردنیاز یا قابل‌عرضه (اختیاری)") })
        OutlinedTextField(address, onAddress, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("آدرس (اختیاری)") })
        OutlinedTextField(landline, onLandline, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("تلفن ثابت (اختیاری)") })
        if (message.isNotBlank()) Text(message, color = EntryGreen, modifier = Modifier.fillMaxWidth())
        if (error.isNotBlank()) Text(error, color = EntryRed, modifier = Modifier.fillMaxWidth())
        Button(onClick = onSave, enabled = !busy, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) { Text(if (busy) "در حال ذخیره..." else "ذخیره اطلاعات و ورود", fontWeight = FontWeight.Bold) }
    }
}
