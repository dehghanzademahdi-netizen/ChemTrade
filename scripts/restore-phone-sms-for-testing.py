from pathlib import Path
import re

ROOT = Path("app/src/main")
entry = ROOT / "java/com/dehghanzadeh/chemtrade/EntryActivity.kt"
main = ROOT / "java/com/dehghanzadeh/chemtrade/MainActivity.kt"
manifest = ROOT / "AndroidManifest.xml"

# Temporary testing mode: let the phone's own SMS app compose the OTP.
# This avoids SEND_SMS runtime permission and keeps the SMS gateway for later.

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

# Replace requestOtp with a no-permission phone-SMS flow.
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

# Resend must use the same SMS composer and not call the removed direct sender.
e = e.replace("sendVerificationSms(phone, otp)", "prepareVerificationSms(phone, otp)")
e = e.replace("sendVerificationSms(phone, expectedCode)", "prepareVerificationSms(phone, expectedCode)")
entry.write_text(e, encoding="utf-8")

# Admin OTP: use the same phone SMS composer temporarily.
m = main.read_text(encoding="utf-8")
for imp in [
    "import android.Manifest\n",
    "import android.content.pm.PackageManager\n",
    "import android.telephony.SmsManager\n",
    "import androidx.core.content.ContextCompat\n",
]:
    m = m.replace(imp, "")

start = m.find("    fun sendAdminOtp()")
if start >= 0:
    end = m.find("\n    val permissionLauncher", start)
    if end >= 0:
        admin_fn = '''    fun sendAdminOtp() {
        val otp = SecureRandom().nextInt(900000).plus(100000).toString()
        expected = otp
        val sms = "ChemLink | کد ورود مدیریت: $otp\\nاین کد را در اختیار دیگران قرار ندهید."
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = android.net.Uri.parse("smsto:$ADMIN_PHONE")
                putExtra("sms_body", sms)
            }
            context.startActivity(intent)
            step = 2
            message = "پیامک آماده شد؛ دکمه ارسال را در برنامه پیامک بزنید."
            error = ""
        } catch (e: Exception) {
            error = "برنامه پیامک روی گوشی پیدا نشد."
        }
        sending = false
    }
'''
        m = m[:start] + admin_fn + m[end:]

# Remove any admin permission launcher left by previous patches.
m = re.sub(r'\n    val permissionLauncher = rememberLauncherForActivityResult\(ActivityResultContracts\.RequestPermission\(\)\) \{.*?\n    \}\n', '\n', m, flags=re.S)
main.write_text(m, encoding="utf-8")

# No SEND_SMS permission is needed for ACTION_SENDTO.
a = manifest.read_text(encoding="utf-8")
a = re.sub(r'\s*<uses-permission android:name="android.permission.SEND_SMS"\s*/>', '', a)
manifest.write_text(a, encoding="utf-8")

print("RESTORED PHONE SMS COMPOSER MODE")
