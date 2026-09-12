from pathlib import Path
import re

ROOT = Path("app/src/main")
entry = ROOT / "java/com/dehghanzadeh/chemtrade/EntryActivity.kt"
main = ROOT / "java/com/dehghanzadeh/chemtrade/MainActivity.kt"
manifest = ROOT / "AndroidManifest.xml"

# Temporary testing mode for EntryActivity: let the phone's own SMS app compose the OTP.
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

# Replace requestOtp with a no-permission phone-SMS composer flow.
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

# Admin OTP remains a direct SMS flow. Keep the imports because MainActivity
# still requests SEND_SMS permission before sending the admin OTP.
m = main.read_text(encoding="utf-8")
required_main_imports = [
    ("import android.Manifest\n", "import android.content.Context\n"),
    ("import android.content.pm.PackageManager\n", "import android.content.Context\n"),
    ("import android.telephony.SmsManager\n", "import android.os.Bundle\n"),
    ("import androidx.core.content.ContextCompat\n", "import androidx.compose.ui.unit.dp\n"),
]
for imp, marker in required_main_imports:
    if imp not in m and marker in m:
        m = m.replace(marker, marker + imp, 1)

# Keep the existing AdminLoginDialog permission launcher intact. The previous
# temporary testing patch removed it, which caused the admin flow to call
# SmsManager directly without first requesting SEND_SMS at runtime.
main.write_text(m, encoding="utf-8")

# Keep SEND_SMS declared for the admin direct-SMS testing flow.
a = manifest.read_text(encoding="utf-8")
if 'android.permission.SEND_SMS' not in a:
    a = a.replace('<uses-permission android:name="android.permission.INTERNET" />', '<uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.SEND_SMS" />', 1)
manifest.write_text(a, encoding="utf-8")

print("SMS FLOW PATCH OK")
