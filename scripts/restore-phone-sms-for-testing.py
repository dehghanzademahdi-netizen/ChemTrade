from pathlib import Path
import re

ROOT = Path("app/src/main")
entry = ROOT / "java/com/dehghanzadeh/chemtrade/EntryActivity.kt"
main = ROOT / "java/com/dehghanzadeh/chemtrade/MainActivity.kt"
manifest = ROOT / "AndroidManifest.xml"

# Both customer and admin OTP use the phone's own SMS composer: no SEND_SMS runtime permission.
e = entry.read_text(encoding="utf-8")
for imp in [
    "import android.Manifest\n",
    "import android.content.pm.PackageManager\n",
    "import android.telephony.SmsManager\n",
    "import androidx.activity.compose.rememberLauncherForActivityResult\n",
    "import androidx.activity.result.contract.ActivityResultContracts\n",
    "import androidx.core.content.ContextCompat\n",
]:
    e = e.replace(imp, "")

send_fn = '''    fun prepareVerificationSms(targetPhone: String, verificationCode: String) {
        val message = "ChemLink - کد تأیید ورود: $verificationCode - این کد را در اختیار دیگران قرار ندهید."
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("smsto:$targetPhone")
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
'''
# Replace the old direct-SMS function if present.
start = e.find("    fun sendVerificationSms(")
if start >= 0:
    end = e.find("\n    fun requestOtp()", start)
    if end >= 0:
        e = e[:start] + send_fn + e[end:]
else:
    start = e.find("    fun prepareVerificationSms(")
    if start < 0:
        raise SystemExit("customer SMS helper not found")

# Remove any runtime-permission launcher and replace requestOtp with composer flow.
e = re.sub(r'\n\s*val permissionLauncher = rememberLauncherForActivityResult\(.*?\n\s*\}\n\s*fun requestOtp\(\)', '\n    fun requestOtp()', e, count=1, flags=re.S)

request_start = e.find("    fun requestOtp()")
if request_start < 0:
    raise SystemExit("requestOtp not found")
# Find next top-level composable Surface marker after requestOtp.
next_marker = e.find("\n    Surface(", request_start)
if next_marker < 0:
    raise SystemExit("EntryFlow Surface marker not found")
request_fn = '''    fun requestOtp() {
        phone = normalizeDigits(phone).filter(Char::isDigit).take(11)
        if (phone.length != 11 || !phone.startsWith("09")) {
            error = "لطفاً شماره موبایل ۱۱ رقمی را درست وارد کنید."
            return
        }
        error = ""
        loadRegisteredUser(context, phone)?.let { accountType = it.type }
        val otp = SecureRandom().nextInt(900000).plus(100000).toString()
        expectedCode = otp
        sending = true
        prepareVerificationSms(phone, otp)
    }
'''
e = e[:request_start] + request_fn + e[next_marker:]
e = e.replace('sendVerificationSms(', 'prepareVerificationSms(')
entry.write_text(e, encoding="utf-8")

# Keep admin login on the exact same no-permission SMS-composer path.
m = main.read_text(encoding="utf-8")
for imp in [
    "import android.Manifest\n",
    "import android.content.pm.PackageManager\n",
    "import android.telephony.SmsManager\n",
    "import androidx.activity.compose.rememberLauncherForActivityResult\n",
    "import androidx.activity.result.contract.ActivityResultContracts\n",
    "import androidx.core.content.ContextCompat\n",
]:
    m = m.replace(imp, "")

start = m.find("@Composable\nprivate fun AdminLoginDialog")
if start < 0:
    raise SystemExit("AdminLoginDialog not found")
end = m.find("\nprivate fun loadOffers", start)
if end < 0:
    end = m.find("\nfun loadOffers", start)
if end < 0:
    raise SystemExit("MainActivity helper boundary not found")

admin = r'''@Composable
private fun AdminLoginDialog(close: () -> Unit, success: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    fun prepareAdminSms(otp: String) {
        val sms = "ChemLink - کد ورود مدیریت: $otp - این کد را در اختیار دیگران قرار ندهید."
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$ADMIN_PHONE")
                putExtra("sms_body", sms)
            }
            context.startActivity(intent)
            sending = false
            step = 2
            message = "پیامک آماده شد؛ دکمه ارسال را در برنامه پیامک بزنید."
            error = ""
        } catch (_: Exception) {
            sending = false
            error = "برنامه پیامک روی گوشی پیدا نشد."
        }
    }

    AlertDialog(onDismissRequest = close, title = { Text(if (step == 0) "ورود مدیریت" else "تأیید دو مرحله‌ای مدیریت") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (step == 0) { Text("دسترسی مدیریت محافظت شده است."); OutlinedTextField(password, { password = digits(it).take(12) }, Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("رمز مدیریت") }) }
        else { Text("کد ۶ رقمی آماده ارسال به شماره مدیریت است."); OutlinedTextField(code, { code = digits(it).filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("کد پیامکی") }) }
        if (message.isNotBlank()) Text(message, color = Green); if (error.isNotBlank()) Text(error, color = Red)
    } }, confirmButton = { Button(enabled = !sending, onClick = {
        if (step == 0) {
            if (hash(password) == ADMIN_HASH) { val otp = SecureRandom().nextInt(900000).plus(100000).toString(); expected = otp; code = ""; sending = true; prepareAdminSms(otp) }
            else error = "رمز مدیریت صحیح نیست."
        } else if (code == expected && expected.isNotBlank()) success() else error = "کد پیامکی صحیح نیست."
    }) { Text(if (step == 0) "ارسال کد ورود" else "تأیید و ورود") } }, dismissButton = { TextButton(close) { Text("انصراف") } })
}
'''
m = m[:start] + admin + m[end:]
main.write_text(m, encoding="utf-8")

# No SEND_SMS permission is needed when using the phone's SMS composer.
a = manifest.read_text(encoding="utf-8")
a = a.replace('    <uses-permission android:name="android.permission.SEND_SMS" />\n', '')
manifest.write_text(a, encoding="utf-8")

print("CUSTOMER + ADMIN SMS COMPOSER PATCH OK")
