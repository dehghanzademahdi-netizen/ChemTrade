from pathlib import Path

ROOT = Path("app/src/main/java/com/dehghanzadeh/chemtrade")
E = ROOT / "EntryActivity.kt"
M = ROOT / "MainActivity.kt"

entry = r'''package com.dehghanzadeh.chemtrade

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.security.SecureRandom

class EntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("chemlink", Context.MODE_PRIVATE)
        if (prefs.getString("phone", "").orEmpty().isNotBlank()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContent { ChemLinkEntryApp() }
    }
}

private val Navy = Color(0xFF0B1F33)
private val Gold = Color(0xFFC89618)
private val Cream = Color(0xFFFBF8F2)
private val Red = Color(0xFFB3261E)
private val Green = Color(0xFF2E7D5B)

private fun digits(s: String): String = s.map { c ->
    when (c) {
        in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar()
        in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar()
        else -> c
    }
}.joinToString("")

private data class User(val phone: String, val type: String, val name: String = "", val company: String = "", val address: String = "", val city: String = "", val landline: String = "")
private fun User.json() = JSONObject().apply {
    put("phone", phone); put("type", type); put("name", name); put("company", company)
    put("address", address); put("city", city); put("landline", landline)
}

private fun localUser(context: Context, phone: String): User? {
    val prefs = context.getSharedPreferences("chemlink", Context.MODE_PRIVATE)
    val arr = runCatching { JSONArray(prefs.getString("users", "[]") ?: "[]") }.getOrDefault(JSONArray())
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        if (digits(o.optString("phone")) == digits(phone)) return User(phone, o.optString("type", "Consumer"), o.optString("name"), o.optString("company"), o.optString("address"), o.optString("city"), o.optString("landline"))
    }
    return null
}

private fun cacheUser(context: Context, user: User) {
    val prefs = context.getSharedPreferences("chemlink", Context.MODE_PRIVATE)
    val old = runCatching { JSONArray(prefs.getString("users", "[]") ?: "[]") }.getOrDefault(JSONArray())
    val out = JSONArray(); var found = false
    for (i in 0 until old.length()) {
        val o = old.optJSONObject(i) ?: continue
        if (digits(o.optString("phone")) == digits(user.phone)) { out.put(user.json()); found = true } else out.put(o)
    }
    if (!found) out.put(user.json())
    prefs.edit().putString("users", out.toString()).apply()
}

@Composable private fun ChemLinkEntryApp() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme(primary = Navy, secondary = Gold, background = Cream)) { EntryFlow() }
    }
}

@Composable private fun EntryFlow() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var expected by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Consumer") }
    var name by rememberSaveable { mutableStateOf("") }
    var company by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var landline by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }

    fun finish(userType: String) {
        prefs.edit().putString("phone", phone).putString("type", userType).apply()
        context.startActivity(Intent(context, MainActivity::class.java))
        (context as? ComponentActivity)?.finish()
    }

    fun prepareVerificationSms(targetPhone: String, verificationCode: String) {
        val sms = "ChemLink - کد تأیید ورود: $verificationCode - این کد را در اختیار دیگران قرار ندهید."
        try {
            context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$targetPhone")
                putExtra("sms_body", sms)
            })
            message = "پیامک آماده شد؛ دکمه ارسال را در برنامه پیامک بزنید."
            step = 2
        } catch (_: Exception) {
            error = "برنامه پیامک روی گوشی پیدا نشد."
        }
    }

    fun requestOtp() {
        phone = digits(phone).filter(Char::isDigit).take(11)
        if (phone.length != 11 || !phone.startsWith("09")) { error = "لطفاً شماره موبایل ۱۱ رقمی را درست وارد کنید."; return }
        error = ""; message = ""
        scope.launch {
            val remote = if (CloudStore.enabled()) CloudStore.loadUser(prefs, phone) else null
            remote?.let {
                val u = User(phone, it.optString("type", "Consumer"), it.optString("name"), it.optString("company"), it.optString("address"), it.optString("city"), it.optString("landline"))
                cacheUser(context, u); type = u.type
            } ?: localUser(context, phone)?.let { type = it.type }
            expected = SecureRandom().nextInt(900000).plus(100000).toString()
            prepareVerificationSms(phone, expected)
        }
    }

    fun verifyCode() {
        if (digits(code) != expected || expected.isBlank()) { error = "کد واردشده صحیح نیست."; return }
        error = ""; message = ""
        scope.launch {
            val remote = if (CloudStore.enabled()) CloudStore.loadUser(prefs, phone) else null
            val u = remote?.let { User(phone, it.optString("type", "Consumer"), it.optString("name"), it.optString("company"), it.optString("address"), it.optString("city"), it.optString("landline")) } ?: localUser(context, phone)
            if (u != null) {
                cacheUser(context, u); type = u.type
                name = u.name; company = u.company; address = u.address; city = u.city; landline = u.landline
            }
            if (u != null && u.name.isNotBlank() && u.company.isNotBlank() && u.address.isNotBlank()) finish(u.type) else step = 3
        }
    }

    Surface(Modifier.fillMaxSize(), color = Cream) {
        when (step) {
            0 -> Welcome { step = 1 }
            1 -> Login(phone, type, { type = it }, { phone = digits(it).filter(Char::isDigit).take(11) }, { requestOtp() }, error)
            2 -> Verify(phone, code, { code = digits(it).filter(Char::isDigit).take(6) }, { verifyCode() }, { expected = SecureRandom().nextInt(900000).plus(100000).toString(); prepareVerificationSms(phone, expected) }, { step = 1; error = "" }, error, message)
            3 -> Profile(name, company, address, city, landline, { name = it }, { company = it }, { address = it }, { city = it }, { landline = digits(it).filter(Char::isDigit).take(11) }, {
                if (name.isBlank() || company.isBlank() || address.isBlank()) error = "نام، نام شرکت و آدرس را کامل کنید." else {
                    val u = User(phone, type, name.trim(), company.trim(), address.trim(), city.trim(), landline)
                    cacheUser(context, u)
                    scope.launch {
                        if (CloudStore.enabled()) CloudStore.saveUser(prefs, u.json())
                        finish(u.type)
                    }
                }
            }, error)
        }
    }
}

@Composable private fun Logo(modifier: Modifier) { Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", modifier, contentScale = ContentScale.Fit) }

@Composable private fun Welcome(next: () -> Unit) = Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
    Logo(Modifier.size(110.dp)); Spacer(Modifier.height(22.dp)); Text("ChemLink", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = Navy); Spacer(Modifier.height(10.dp)); Text("پل هوشمند تأمین و فروش مواد اولیه", color = Gold, textAlign = TextAlign.Center); Spacer(Modifier.height(34.dp)); Button(next, Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(18.dp)) { Text("ورود به ChemLink", fontWeight = FontWeight.Bold) }
}

@Composable private fun Login(phone: String, type: String, onType: (String) -> Unit, onPhone: (String) -> Unit, next: () -> Unit, error: String) = Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
    Logo(Modifier.size(82.dp)); Spacer(Modifier.height(18.dp)); Text("ورود / ثبت‌نام", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Navy); Spacer(Modifier.height(12.dp)); Text("شماره موبایل را وارد کنید. کد تأیید برای شما آماده می‌شود.", textAlign = TextAlign.Center); Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(type == "Supplier", { onType("Supplier") }, label = { Text("تأمین‌کننده") }); FilterChip(type == "Consumer", { onType("Consumer") }, label = { Text("مصرف‌کننده") }) }; Spacer(Modifier.height(12.dp)); OutlinedTextField(phone, onPhone, Modifier.fillMaxWidth(), singleLine = true, label = { Text("شماره موبایل") }); Spacer(Modifier.height(18.dp)); Button(next, Modifier.fillMaxWidth().height(52.dp), RoundedCornerShape(16.dp)) { Text("دریافت کد پیامکی") }; if (error.isNotBlank()) Text(error, color = Red, textAlign = TextAlign.Center)
}

@Composable private fun Verify(phone: String, code: String, onCode: (String) -> Unit, verify: () -> Unit, resend: () -> Unit, back: () -> Unit, error: String, message: String) = Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
    Logo(Modifier.size(82.dp)); Spacer(Modifier.height(18.dp)); Text("تأیید کد پیامکی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Navy); Spacer(Modifier.height(10.dp)); Text("کد ارسال‌شده برای $phone را وارد کنید.", textAlign = TextAlign.Center); Spacer(Modifier.height(18.dp)); OutlinedTextField(code, onCode, Modifier.fillMaxWidth(), singleLine = true, label = { Text("کد ۶ رقمی") }); if (message.isNotBlank()) Text(message, color = Green, textAlign = TextAlign.Center); if (error.isNotBlank()) Text(error, color = Red, textAlign = TextAlign.Center); Spacer(Modifier.height(18.dp)); Button(verify, Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(18.dp)) { Text("تأیید و ادامه") }; TextButton(resend) { Text("ارسال مجدد کد") }; TextButton(back) { Text("ویرایش شماره") }
}

@Composable private fun Profile(name: String, company: String, address: String, city: String, landline: String, onName: (String) -> Unit, onCompany: (String) -> Unit, onAddress: (String) -> Unit, onCity: (String) -> Unit, onLandline: (String) -> Unit, save: () -> Unit, error: String) = Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Logo(Modifier.size(72.dp)); Text("تکمیل اطلاعات حساب", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Navy); Text("این اطلاعات فقط در اولین ثبت‌نام لازم است.", textAlign = TextAlign.Center); Field("نام و نام خانوادگی", name, onName); Field("نام شرکت / مجموعه", company, onCompany); Field("شهر", city, onCity); OutlinedTextField(address, onAddress, Modifier.fillMaxWidth(), minLines = 3, label = { Text("آدرس کامل") }); Field("تلفن ثابت (اختیاری)", landline, onLandline); if (error.isNotBlank()) Text(error, color = Red); Button(save, Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(18.dp)) { Text("ذخیره اطلاعات و ورود", fontWeight = FontWeight.Bold) }
}

@Composable private fun Field(label: String, value: String, change: (String) -> Unit) = OutlinedTextField(value, change, Modifier.fillMaxWidth(), singleLine = true, label = { Text(label) })
'''

E.write_text(entry, encoding="utf-8")

# Ensure the main screen pulls the cloud copy on launch and pushes offer changes.
m = M.read_text(encoding="utf-8")
if "CloudStore.pull(prefs)" not in m:
    marker = "var offers by remember { mutableStateOf(loadOffers(prefs)) }"
    if marker in m:
        m = m.replace(marker, marker + "\n    LaunchedEffect(Unit) { if (CloudStore.enabled()) { CloudStore.pull(prefs); offers = loadOffers(prefs) } }", 1)
if "CloudStore.push(prefs)" not in m:
    marker = "offers = list; persistOffers(prefs, list)"
    if marker in m:
        m = m.replace(marker, marker + "\n        if (CloudStore.enabled()) kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { CloudStore.push(prefs) }", 1)
M.write_text(m, encoding="utf-8")
print("VERIFIED CLOUD ACCOUNT FLOW PATCH OK")
