from pathlib import Path
import re

ROOT = Path("app/src/main")
entry = ROOT / "java/com/dehghanzadeh/chemtrade/EntryActivity.kt"
main = ROOT / "java/com/dehghanzadeh/chemtrade/MainActivity.kt"
manifest = ROOT / "AndroidManifest.xml"

# Remove the dangerous SEND_SMS permission and use the system SMS composer.
e = entry.read_text(encoding="utf-8")
for imp in ["import android.Manifest\n", "import android.content.pm.PackageManager\n", "import android.telephony.SmsManager\n", "import androidx.activity.compose.rememberLauncherForActivityResult\n", "import androidx.activity.result.contract.ActivityResultContracts\n", "import androidx.core.content.ContextCompat\n"]:
    e = e.replace(imp, "")
if "import android.net.Uri\n" not in e:
    e = e.replace("import android.content.Intent\n", "import android.content.Intent\nimport android.net.Uri\n", 1)

start = e.find("    fun sendVerificationSms(")
if start < 0:
    start = e.find("    fun prepareVerificationSms(")
surface = e.find("    Surface(modifier = Modifier.fillMaxSize(), color = EntryCream) {", start)
if start < 0 or surface < 0:
    raise SystemExit("Entry auth block markers not found")

auth_block = '''    fun prepareVerificationSms(targetPhone: String, verificationCode: String) {
        val message = "ChemLink\\nکد تأیید ورود به حساب: $verificationCode\\nاین پیامک برای تأیید حساب کاربری ChemLink است. کد را در اختیار دیگران قرار ندهید."
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$targetPhone")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
            sending = false
            step = 2
            error = "پیامک آماده شد؛ دکمه ارسال را در برنامه پیامک بزنید."
        } catch (_: Exception) {
            sending = false
            error = "برنامه پیامک روی گوشی پیدا نشد."
        }
    }

    fun requestOtp() {
        phone = normalizeDigits(phone).filter(Char::isDigit).take(11)
        if (phone.length != 11 || !phone.startsWith("09")) {
            error = "لطفاً شماره موبایل ۱۱ رقمی را درست وارد کنید."
            return
        }
        error = ""
        val existing = loadRegisteredUser(context, phone)
        if (existing != null) accountType = existing.type
        val otp = SecureRandom().nextInt(900000).plus(100000).toString()
        expectedCode = otp
        sending = true
        prepareVerificationSms(phone, otp)
    }

'''
e = e[:start] + auth_block + e[surface:]
e = e.replace('''                    try { sendVerificationSms(phone, otp) }
                    catch (e: Exception) { error = e.message ?: "ارسال مجدد ناموفق بود." }''', '                    prepareVerificationSms(phone, otp)')
entry.write_text(e, encoding="utf-8")

m = main.read_text(encoding="utf-8")
for imp in ["import android.Manifest\n", "import android.content.pm.PackageManager\n", "import android.telephony.SmsManager\n", "import androidx.activity.compose.rememberLauncherForActivityResult\n", "import androidx.activity.result.contract.ActivityResultContracts\n", "import androidx.core.content.ContextCompat\n"]:
    m = m.replace(imp, "")
if "import androidx.compose.foundation.ExperimentalFoundationApi\n" not in m:
    m = m.replace("import androidx.compose.foundation.Image\n", "import androidx.compose.foundation.Image\nimport androidx.compose.foundation.ExperimentalFoundationApi\n", 1)
if "import androidx.compose.foundation.combinedClickable\n" not in m:
    m = m.replace("import androidx.compose.foundation.clickable\n", "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.combinedClickable\n", 1)

hs = m.find("@Composable\nprivate fun HiddenAdminBrand")
he = m.find("@Composable\nprivate fun AdminDashboard", hs)
if hs >= 0 and he >= 0:
    hidden = '''@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HiddenAdminBrand(onUnlock: () -> Unit) {
    Row(Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onUnlock).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(62.dp))
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ChemLink", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Navy)
            Text("بازار حرفه‌ای مواد اولیه", color = Gold, fontWeight = FontWeight.Bold)
        }
    }
}

'''
    m = m[:hs] + hidden + m[he:]

old_header = 'Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(34.dp))'
new_header = '''Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(44.dp).combinedClickable(onClick = {}, onLongClick = { showAdminLogin = true }))'''
m = m.replace(old_header, new_header, 1)

as_ = m.find("@Composable\nprivate fun AdminLoginDialog")
ae = m.find("private fun loadOffers", as_)
if as_ < 0 or ae < 0:
    raise SystemExit("Admin login block markers not found")
admin = '''@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdminLoginDialog(close: () -> Unit, success: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    fun sendAdminOtp() {
        val otp = SecureRandom().nextInt(900000).plus(100000).toString()
        expected = otp
        val sms = "ChemLink | کد ورود مدیریت: $otp\\nاین کد را در اختیار دیگران قرار ندهید."
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$ADMIN_PHONE")
                putExtra("sms_body", sms)
            }
            context.startActivity(intent)
            step = 2
            message = "پیامک آماده شد؛ دکمه ارسال را بزنید و سپس به ChemLink برگردید."
            error = ""
        } catch (_: Exception) {
            error = "برنامه پیامک روی گوشی پیدا نشد."
        }
        sending = false
    }

    AlertDialog(onDismissRequest = close, title = { Text(if (step == 0) "ورود مدیریت" else "تأیید دو مرحله‌ای مدیریت") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (step == 0) {
                Text("دسترسی مدیریت محافظت شده است.")
                OutlinedTextField(password, { password = digits(it).take(12) }, Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("رمز مدیریت") })
            } else if (step == 1) {
                Text("رمز صحیح است. برای مرحله دوم پیامک را آماده کنید.")
            } else {
                Text("کد ۶ رقمی ارسال‌شده به شماره مدیریت را وارد کنید.")
                OutlinedTextField(code, { code = digits(it).filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("کد پیامکی") })
            }
            if (message.isNotBlank()) Text(message, color = Green)
            if (error.isNotBlank()) Text(error, color = Red)
        }
    }, confirmButton = {
        Button(enabled = !sending, onClick = {
            when (step) {
                0 -> if (hash(password) == ADMIN_HASH) { step = 1; error = "" } else error = "رمز مدیریت صحیح نیست."
                1 -> { sending = true; sendAdminOtp() }
                else -> if (code == expected && expected.isNotBlank()) success() else error = "کد پیامکی صحیح نیست."
            }
        }) { Text(when (step) { 0 -> "ادامه"; 1 -> "آماده‌سازی پیامک"; else -> "تأیید و ورود" }) }
    }, dismissButton = { TextButton(close) { Text("انصراف") } })
}

'''
m = m[:as_] + admin + m[ae:]
main.write_text(m, encoding="utf-8")

manifest_text = manifest.read_text(encoding="utf-8")
manifest_text = re.sub(r'\n\s*<uses-permission android:name="android\.permission\.SEND_SMS"\s*/>', '', manifest_text)
manifest.write_text(manifest_text, encoding="utf-8")
print("SMS permission removed; consumer/admin SMS use system composer; hidden admin long-press fixed.")
