package com.dehghanzadeh.chemtrade

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

class EntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("chemlink", Context.MODE_PRIVATE)
        PersistentBackup.restore(this, prefs)
        if (CloudStore.enabled()) runBlocking { CloudStore.pull(prefs) }
        if (prefs.getString("phone", "").orEmpty().isNotBlank()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContent { ChemLinkEntryApp() }
    }
}

private val EntryNavy = Color(0xFF0B1F33)
private val EntryBlue = Color(0xFF1565A8)
private val EntryGold = Color(0xFFC89618)
private val EntryCream = Color(0xFFFBF8F2)
private val EntryGreen = Color(0xFF2E7D5B)
private val EntryRed = Color(0xFFB3261E)

private fun normalizeDigits(value: String): String = value.map { c ->
    when (c) {
        in '۰'..'۹' -> ('0'.code + (c.code - '۰'.code)).toChar()
        in '٠'..'٩' -> ('0'.code + (c.code - '٠'.code)).toChar()
        else -> c
    }
}.joinToString("")

private data class EntryUser(
    val phone: String,
    val type: String,
    val name: String = "",
    val company: String = "",
    val address: String = "",
    val city: String = "",
    val landline: String = ""
)

private fun loadRegisteredUser(context: Context, phone: String): EntryUser? {
    val prefs = context.getSharedPreferences("chemlink", Context.MODE_PRIVATE)
    val raw = prefs.getString("users", "[]") ?: "[]"
    val old = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
    for (i in 0 until old.length()) {
        val item = old.optJSONObject(i) ?: continue
        if (item.optString("phone") == phone) {
            return EntryUser(phone, item.optString("type", "Consumer"), item.optString("name"), item.optString("company"), item.optString("address"), item.optString("city"), item.optString("landline"))
        }
    }
    return null
}

private fun saveRegisteredUser(context: Context, user: EntryUser) {
    val prefs = context.getSharedPreferences("chemlink", Context.MODE_PRIVATE)
    val old = runCatching { JSONArray(prefs.getString("users", "[]") ?: "[]") }.getOrDefault(JSONArray())
    val users = JSONArray(); var found = false
    for (i in 0 until old.length()) {
        val item = old.optJSONObject(i) ?: continue
        if (item.optString("phone") == user.phone) {
            users.put(JSONObject().apply { put("phone", user.phone); put("type", user.type); put("name", user.name); put("company", user.company); put("address", user.address); put("city", user.city); put("landline", user.landline) })
            found = true
        } else users.put(item)
    }
    if (!found) users.put(JSONObject().apply { put("phone", user.phone); put("type", user.type); put("name", user.name); put("company", user.company); put("address", user.address); put("city", user.city); put("landline", user.landline) })
    prefs.edit().putString("users", users.toString()).commit()
    if (CloudStore.enabled()) runBlocking { CloudStore.push(prefs) }
    PersistentBackup.backup(context, prefs)
}

@Composable
private fun ChemLinkEntryApp() {
    val colors = lightColorScheme(primary = EntryNavy, secondary = EntryGold, background = EntryCream, surface = Color.White, onPrimary = Color.White, onBackground = EntryNavy)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { MaterialTheme(colorScheme = colors) { EntryFlow() } }
}

@Composable
private fun EntryFlow() {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var expectedCode by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var accountType by rememberSaveable { mutableStateOf("Consumer") }
    var profileName by rememberSaveable { mutableStateOf("") }
    var company by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var landline by rememberSaveable { mutableStateOf("") }
    var sending by rememberSaveable { mutableStateOf(false) }

    fun finishLogin(type: String) {
        context.getSharedPreferences("chemlink", Context.MODE_PRIVATE).edit().putString("phone", phone).putString("type", type).commit()
        context.startActivity(Intent(context, MainActivity::class.java)); (context as? ComponentActivity)?.finish()
    }

    fun sendVerificationSms(targetPhone: String, verificationCode: String) {
        val message = "ChemLink\nکد تأیید ورود به حساب: $verificationCode\nاین پیامک برای تأیید حساب کاربری ChemLink است. کد را در اختیار دیگران قرار ندهید."
        try {
            @Suppress("DEPRECATION") val manager = SmsManager.getDefault(); val parts = manager.divideMessage(message)
            if (parts.size == 1) manager.sendTextMessage(targetPhone, null, message, null, null) else manager.sendMultipartTextMessage(targetPhone, null, parts, null, null)
        } catch (_: Exception) { throw IllegalStateException("ارسال پیامک ناموفق بود. مجوز SMS یا سیم‌کارت را بررسی کنید.") }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { try { sendVerificationSms(phone, expectedCode); sending = false; step = 2; error = "" } catch (e: Exception) { sending = false; error = e.message ?: "ارسال پیامک ناموفق بود." } }
        else { sending = false; error = "برای ورود پیامکی باید مجوز ارسال SMS را فعال کنید." }
    }

    fun requestOtp() {
        phone = normalizeDigits(phone).filter(Char::isDigit).take(11)
        if (phone.length != 11 || !phone.startsWith("09")) { error = "لطفاً شماره موبایل ۱۱ رقمی را درست وارد کنید."; return }
        error = ""; loadRegisteredUser(context, phone)?.let { accountType = it.type }
        val otp = SecureRandom().nextInt(900000).plus(100000).toString(); expectedCode = otp; sending = true
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) { sendVerificationSms(phone, otp); sending = false; step = 2 }
            else permissionLauncher.launch(Manifest.permission.SEND_SMS)
        } catch (e: Exception) { sending = false; error = e.message ?: "ارسال پیامک ناموفق بود." }
    }

    Surface(Modifier.fillMaxSize(), color = EntryCream) {
        when (step) {
            0 -> WelcomeScreen { step = 1 }
            1 -> ServiceLoginScreen(phone, accountType, { accountType = it }, { phone = normalizeDigits(it).filter(Char::isDigit).take(11) }, { requestOtp() }, error, sending)
            2 -> VerifyCodeScreen(phone, code, { code = normalizeDigits(it).filter(Char::isDigit).take(6) }, {
                code = normalizeDigits(code).filter(Char::isDigit).take(6)
                if (code == expectedCode && expectedCode.isNotBlank()) {
                    val existing = loadRegisteredUser(context, phone)
                    if (existing != null && existing.name.isNotBlank() && existing.company.isNotBlank() && existing.address.isNotBlank()) finishLogin(existing.type)
                    else { profileName = existing?.name.orEmpty(); company = existing?.company.orEmpty(); address = existing?.address.orEmpty(); city = existing?.city.orEmpty(); landline = existing?.landline.orEmpty(); if (existing != null) accountType = existing.type; step = 3; error = "" }
                } else error = "کد واردشده صحیح نیست."
            }, {
                val otp = SecureRandom().nextInt(900000).plus(100000).toString(); expectedCode = otp; error = ""
                try { sendVerificationSms(phone, otp) } catch (e: Exception) { error = e.message ?: "ارسال مجدد ناموفق بود." }
            }, { code = ""; error = ""; step = 1 }, error)
            3 -> ProfileScreen(profileName, company, address, city, landline, { profileName = it }, { company = it }, { address = it }, { city = it }, { landline = normalizeDigits(it).filter(Char::isDigit).take(11) }, {
                if (profileName.isBlank() || company.isBlank() || address.isBlank()) error = "نام، نام شرکت و آدرس را کامل کنید."
                else { saveRegisteredUser(context, EntryUser(phone, accountType, profileName.trim(), company.trim(), address.trim(), city.trim(), landline)); error = ""; finishLogin(accountType) }
            }, error)
        }
    }
}

@Composable private fun BrandLogo(modifier: Modifier = Modifier) { Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", modifier, contentScale = ContentScale.Fit) }

@Composable private fun WelcomeScreen(onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { BrandLogo(Modifier.size(116.dp)); Spacer(Modifier.height(22.dp)); Text("ChemLink", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = EntryNavy); Spacer(Modifier.height(8.dp)); Text("پل هوشمند تأمین و فروش مواد اولیه", style = MaterialTheme.typography.titleMedium, color = EntryGold, textAlign = TextAlign.Center); Spacer(Modifier.height(18.dp)); Text("بازار حرفه‌ای برای ارتباط مطمئن تأمین‌کنندگان، فروشندگان و مصرف‌کنندگان مواد اولیه", textAlign = TextAlign.Center, color = EntryNavy); Spacer(Modifier.height(36.dp)); Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = EntryNavy)) { Text("ورود به ChemLink", fontWeight = FontWeight.Bold) } }
}

@Composable private fun ServiceLoginScreen(phone: String, type: String, onTypeChange: (String) -> Unit, onPhoneChange: (String) -> Unit, onContinue: () -> Unit, error: String, sending: Boolean) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { BrandLogo(Modifier.size(86.dp)); Spacer(Modifier.height(18.dp)); Text("ورود / ثبت‌نام", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = EntryNavy, textAlign = TextAlign.Center); Spacer(Modifier.height(8.dp)); Text("شماره موبایل را وارد کنید. کد تأیید پیامکی برای ورود ارسال می‌شود.", color = EntryNavy, textAlign = TextAlign.Center); Spacer(Modifier.height(18.dp));
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = type == "Supplier", onClick = { onTypeChange("Supplier") }, label = { Text("تأمین‌کننده") }); FilterChip(selected = type == "Consumer", onClick = { onTypeChange("Consumer") }, label = { Text("مصرف‌کننده") }) }
        Spacer(Modifier.height(12.dp)); OutlinedTextField(value = phone, onValueChange = onPhoneChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("شماره موبایل") }, leadingIcon = { Icon(Icons.Filled.Phone, null) }); Spacer(Modifier.height(18.dp)); Button(onClick = onContinue, enabled = !sending, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = EntryNavy)) { Text(if (sending) "در حال ارسال..." else "دریافت کد پیامکی") }; if (error.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(error, color = EntryRed, textAlign = TextAlign.Center) }
    }
}
