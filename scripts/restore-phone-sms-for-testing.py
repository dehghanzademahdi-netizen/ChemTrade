from pathlib import Path

ROOT = Path("app/src/main")
entry = ROOT / "java/com/dehghanzadeh/chemtrade/EntryActivity.kt"
main = ROOT / "java/com/dehghanzadeh/chemtrade/MainActivity.kt"
manifest = ROOT / "AndroidManifest.xml"

# Both customer and admin OTP now use the phone's own SMS composer.
# This avoids SEND_SMS runtime permission and therefore avoids the Android
# permission failure the user was seeing.
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
start = e.find("    fun sendVerificationSms(")
if start >= 0:
    end = e.find("\n    fun requestOtp()", start)
    if end >= 0:
        e = e[:start] + send_fn + e[end:]

start = e.find("    fun requestOtp()")
if start >= 0:
    end = e.find("\n    Surface(modifier = Modifier.fillMaxSize(), color = EntryCream) {", start)
    if end >= 0:
        request_fn = '''    fun requestOtp() {
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
        e = e[:start] + request_fn + e[end:]

e = e.replace("sendVerificationSms(phone, otp)", "prepareVerificationSms(phone, otp)")
e = e.replace("sendVerificationSms(phone, expectedCode)", "prepareVerificationSms(phone, expectedCode)")
entry.write_text(e, encoding="utf-8")

# Admin login must follow the exact same no-permission SMS-composer path.
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
end = m.find("\n@Composable", start + 10)
if end < 0:
    end = len(m)

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

    fun prepareAdminSms(code: String) {
        val sms = "ChemLink - کد ورود مدیریت: $code - این کد را در اختیار دیگران قرار ندهید."
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$ADMIN_PHONE")
                putExtra("sms_body", sms)
            }
            context.startActivity(intent)
            sending = false
            step = 1
            message = "پیامک ورود مدیریت آماده شد؛ دکمه ارسال را در برنامه پیامک بزنید."
            error = ""
        } catch (_: Exception) {
            sending = false
            error = "برنامه پیامک روی گوشی پیدا نشد."
        }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (step == 0) "ورود مدیریت" else "تأیید ورود مدیریت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step == 0) {
                    Text("رمز مدیریت را وارد کنید.")
                    OutlinedTextField(value = password, onValueChange = { password = it }, singleLine = true, label = { Text("رمز مدیریت") }, visualTransformation = PasswordVisualTransformation())
                } else {
                    Text("کد تأیید به شماره مدیریت ارسال شد.")
                    OutlinedTextField(value = otp, onValueChange = { otp = digits(it).filter(Char::isDigit).take(6) }, singleLine = true, label = { Text("کد پیامکی") })
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
                    val code = SecureRandom().nextInt(900000).plus(100000).toString()
                    expected = code
                    sending = true
                    prepareAdminSms(code)
                } else {
                    if (otp == expected && expected.isNotBlank()) onSuccess() else error = "کد واردشده صحیح نیست."
                }
            }) { Text(if (step == 0) "ارسال کد ورود" else "ورود به مدیریت") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("انصراف") } }
    )
}
'''
m = m[:start] + admin + m[end:]
main.write_text(m, encoding="utf-8")

# SEND_SMS is no longer needed anywhere in the app.
a = manifest.read_text(encoding="utf-8")
a = a.replace('    <uses-permission android:name="android.permission.SEND_SMS" />\n', '')
manifest.write_text(a, encoding="utf-8")

print("CUSTOMER + ADMIN SMS COMPOSER PATCH OK")
