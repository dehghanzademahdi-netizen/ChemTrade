from pathlib import Path
import re

ROOT = Path("app/src/main")
entry = ROOT / "java/com/dehghanzadeh/chemtrade/EntryActivity.kt"
main = ROOT / "java/com/dehghanzadeh/chemtrade/MainActivity.kt"
manifest = ROOT / "AndroidManifest.xml"

SMS_IMPORTS = ["import android.Manifest\n", "import android.content.pm.PackageManager\n", "import android.telephony.SmsManager\n", "import androidx.activity.compose.rememberLauncherForActivityResult\n", "import androidx.activity.result.contract.ActivityResultContracts\n", "import androidx.core.content.ContextCompat\n"]

def clean(text):
    for imp in SMS_IMPORTS:
        text = text.replace(imp, "")
    return text

# Consumer OTP: no SEND_SMS permission; use the system SMS composer.
e = clean(entry.read_text(encoding="utf-8"))
if "import android.net.Uri\n" not in e:
    e = e.replace("import android.content.Intent\n", "import android.content.Intent\nimport android.net.Uri\n", 1)
start = e.find("    fun sendVerificationSms(")
if start < 0:
    start = e.find("    fun prepareVerificationSms(")
surface = e.find("    Surface(modifier = Modifier.fillMaxSize(), color = EntryCream) {", start)
if start < 0 or surface < 0:
    raise SystemExit("Entry auth markers not found")
entry_auth = '''    fun prepareVerificationSms(targetPhone: String, verificationCode: String) {
        val message = "ChemLink - کد تأیید ورود: $verificationCode - این کد را در اختیار دیگران قرار ندهید."
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
e = e[:start] + entry_auth + e[surface:]
e = re.sub(r'try \{\s*sendVerificationSms\(phone, otp\)\s*\}\s*catch \([^)]*\)\s*\{[^}]*\}', 'prepareVerificationSms(phone, otp)', e, count=1, flags=re.S)
entry.write_text(e, encoding="utf-8")

# Admin OTP: no SEND_SMS permission; use the system SMS composer.
m = clean(main.read_text(encoding="utf-8"))
if "import android.net.Uri\n" not in m:
    m = m.replace("import android.content.Intent\n", "import android.content.Intent\nimport android.net.Uri\n", 1)
if "import androidx.compose.foundation.combinedClickable\n" not in m:
    m = m.replace("import androidx.compose.foundation.clickable\n", "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.combinedClickable\n", 1)
if "import androidx.compose.foundation.ExperimentalFoundationApi\n" not in m:
    m = m.replace("import androidx.compose.foundation.Image\n", "import androidx.compose.foundation.Image\nimport androidx.compose.foundation.ExperimentalFoundationApi\n", 1)

admin_sender = '''    fun sendAdminOtp() {
        val otp = SecureRandom().nextInt(900000).plus(100000).toString()
        expected = otp
        val sms = "ChemLink - کد ورود مدیریت: $otp - این کد را در اختیار دیگران قرار ندهید."
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
'''
m, n = re.subn(r'    fun sendAdminOtp\(\)\s*\{.*?^    \}\n', admin_sender, m, count=1, flags=re.S|re.M)
if n != 1:
    raise SystemExit("Admin OTP function not found")

# Remove old permission launcher and permission checks, even if previous patches formatted them differently.
m = re.sub(r'\n\s*val permissionLauncher\s*=\s*rememberLauncherForActivityResult\([\s\S]*?\)\s*(?=\n\s*AlertDialog)', '\n', m, count=1)
m = re.sub(r'\n\s*val permissionLauncher\s*=\s*rememberLauncherForActivityResult[^\n]*', '\n', m, count=1)
m = re.sub(r'if \(ContextCompat\.checkSelfPermission\(context, Manifest\.permission\.SEND_SMS\) == PackageManager\.PERMISSION_GRANTED\)\s*sendAdminOtp\(\)\s*else\s*permissionLauncher\.launch\(Manifest\.permission\.SEND_SMS\)', 'sendAdminOtp()', m, count=1)
m = m.replace('permissionLauncher.launch(Manifest.permission.SEND_SMS)', 'sendAdminOtp()')

# Reliable hidden admin entry on the actual logo.
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

header_old = 'Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(34.dp))'
header_new = 'Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(44.dp).combinedClickable(onClick = {}, onLongClick = { showAdminLogin = true }))'
m = m.replace(header_old, header_new, 1)
if 'combinedClickable(onClick = {}, onLongClick = { showAdminLogin = true })' in m:
    m = re.sub(r'(?m)^(@Composable\n(?:private )?fun MainScreen)', r'@OptIn(ExperimentalFoundationApi::class)\n\1', m, count=1)

# Hard cleanup of any residual symbols that could trigger the permission dialog or compile failure.
m = re.sub(r'\n\s*.*rememberLauncherForActivityResult.*', '', m)
m = re.sub(r'\n\s*.*ActivityResultContracts.*', '', m)
m = re.sub(r'\n\s*.*ContextCompat\.checkSelfPermission.*', '', m)
m = re.sub(r'\n\s*.*SmsManager\.getDefault.*', '', m)
main.write_text(m, encoding="utf-8")

a = manifest.read_text(encoding="utf-8")
a = re.sub(r'\n\s*<uses-permission android:name="android\.permission\.SEND_SMS"\s*/>', '', a)
manifest.write_text(a, encoding="utf-8")
print("FINAL SMS/ADMIN PATCH OK")
