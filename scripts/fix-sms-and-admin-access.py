from pathlib import Path
import re

ROOT = Path("app/src/main")
entry = ROOT / "java/com/dehghanzadeh/chemtrade/EntryActivity.kt"
main = ROOT / "java/com/dehghanzadeh/chemtrade/MainActivity.kt"
manifest = ROOT / "AndroidManifest.xml"

# The app is currently distributed as a direct APK, so use the phone's SMS
# capability instead of opening a composer. Keep the runtime permission flow.
e = entry.read_text(encoding="utf-8")
if "import android.Manifest\n" not in e:
    e = e.replace("package com.dehghanzadeh.chemtrade\n", "package com.dehghanzadeh.chemtrade\n\nimport android.Manifest\n", 1)
if "import android.content.pm.PackageManager\n" not in e:
    e = e.replace("import android.content.Context\n", "import android.content.Context\nimport android.content.pm.PackageManager\n", 1)
if "import android.telephony.SmsManager\n" not in e:
    e = e.replace("import android.os.Bundle\n", "import android.os.Bundle\nimport android.telephony.SmsManager\n", 1)
if "import androidx.activity.compose.rememberLauncherForActivityResult\n" not in e:
    e = e.replace("import androidx.activity.compose.setContent\n", "import androidx.activity.compose.setContent\nimport androidx.activity.compose.rememberLauncherForActivityResult\n", 1)
if "import androidx.activity.result.contract.ActivityResultContracts\n" not in e:
    e = e.replace("import androidx.activity.result.contract.ActivityResultContracts\n", "import androidx.activity.result.contract.ActivityResultContracts\n", 1)
if "import androidx.core.content.ContextCompat\n" not in e:
    e = e.replace("import androidx.compose.ui.unit.dp\n", "import androidx.compose.ui.unit.dp\nimport androidx.core.content.ContextCompat\n", 1)

# Restore the direct SMS sender if an earlier patch replaced it with a composer.
start = e.find("    fun prepareVerificationSms(")
if start >= 0:
    end = e.find("\n    fun requestOtp()", start)
    if end >= 0:
        direct = '''    fun sendVerificationSms(targetPhone: String, verificationCode: String) {
        val message = "ChemLink\\nکد تأیید ورود به حساب: $verificationCode\\nاین پیامک برای تأیید حساب کاربری ChemLink است. کد را در اختیار دیگران قرار ندهید."
        @Suppress("DEPRECATION")
        val manager = SmsManager.getDefault()
        val parts = manager.divideMessage(message)
        if (parts.size == 1) manager.sendTextMessage(targetPhone, null, message, null, null)
        else manager.sendMultipartTextMessage(targetPhone, null, parts, null, null)
    }
'''
        e = e[:start] + direct + e[end:]

# If the original sender is already present, leave it intact. Ensure requestOtp
# calls it after the runtime permission is granted.
e = e.replace("prepareVerificationSms(phone, expectedCode)", "sendVerificationSms(phone, expectedCode)")
e = e.replace("prepareVerificationSms(phone, otp)", "sendVerificationSms(phone, otp)")
entry.write_text(e, encoding="utf-8")

m = main.read_text(encoding="utf-8")
for imp, marker in [
    ("import android.Manifest\n", "import android.content.Context\n"),
    ("import android.content.pm.PackageManager\n", "import android.content.Context\n"),
    ("import android.telephony.SmsManager\n", "import android.os.Bundle\n"),
    ("import androidx.core.content.ContextCompat\n", "import androidx.compose.ui.unit.dp\n"),
]:
    if imp not in m:
        m = m.replace(marker, marker + imp, 1)

# Admin login: password first, then send a fresh OTP automatically from the
# management phone, then verify the six-digit code.
admin_sender = '''    fun sendAdminOtp() {
        val otp = SecureRandom().nextInt(900000).plus(100000).toString()
        expected = otp
        val sms = "ChemLink | کد ورود مدیریت: $otp\\nاین کد را در اختیار دیگران قرار ندهید."
        try {
            @Suppress("DEPRECATION")
            val manager = SmsManager.getDefault()
            val parts = manager.divideMessage(sms)
            if (parts.size == 1) manager.sendTextMessage(ADMIN_PHONE, null, sms, null, null)
            else manager.sendMultipartTextMessage(ADMIN_PHONE, null, parts, null, null)
            step = 2
            message = "کد ورود به شماره مدیریت ارسال شد."
            error = ""
        } catch (e: Exception) {
            error = "ارسال پیامک ناموفق بود: ${e.message ?: "خطای سیم‌کارت یا مجوز SMS"}"
        }
        sending = false
    }
'''
start = m.find("    fun sendAdminOtp()")
if start >= 0:
    end = m.find("\n    val permissionLauncher", start)
    if end >= 0:
        m = m[:start] + admin_sender + m[end:]

# Ensure admin's permission callback really calls the direct sender.
m = m.replace("val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) sendAdminOtp() else { sending = false; error = \"مجوز ارسال پیامک داده نشد.\" } }", "val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) sendAdminOtp() else { sending = false; error = \"مجوز ارسال پیامک داده نشد.\" } }")

# Keep the hidden long-press admin entry point (five seconds) on the brand logo.
if "combinedClickable" not in m:
    m = m.replace("import androidx.compose.foundation.clickable\n", "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.combinedClickable\n", 1)
if "ExperimentalFoundationApi" not in m:
    m = m.replace("import androidx.compose.foundation.Image\n", "import androidx.compose.foundation.Image\nimport androidx.compose.foundation.ExperimentalFoundationApi\n", 1)

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

# Header logo is also a hidden admin entry point without showing a management label.
m = m.replace('Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(34.dp))', 'Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(44.dp).combinedClickable(onClick = {}, onLongClick = { showAdminLogin = true }))', 1)
if 'combinedClickable(onClick = {}, onLongClick = { showAdminLogin = true })' in m and '@OptIn(ExperimentalFoundationApi::class)' not in m[:m.find('@Composable\nprivate fun MainScreen')]:
    marker = '@Composable\nprivate fun MainScreen'
    m = m.replace(marker, '@OptIn(ExperimentalFoundationApi::class)\n' + marker, 1)

main.write_text(m, encoding="utf-8")

# Direct SMS requires SEND_SMS to be declared. This is intentionally kept for
# the direct APK distribution/testing flow; no SMS is sent until the user taps
# the OTP action and grants the Android permission.
a = manifest.read_text(encoding="utf-8")
if 'android.permission.SEND_SMS' not in a:
    a = a.replace('<uses-permission android:name="android.permission.INTERNET" />', '<uses-permission android:name="android.permission.INTERNET" />\n    <uses-permission android:name="android.permission.SEND_SMS" />', 1)
manifest.write_text(a, encoding="utf-8")
print("FINAL SMS/ADMIN PATCH OK")
