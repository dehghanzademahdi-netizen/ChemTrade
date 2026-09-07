from pathlib import Path
import re

ROOT = Path("app/src/main")
entry = ROOT / "java/com/dehghanzadeh/chemtrade/EntryActivity.kt"
main = ROOT / "java/com/dehghanzadeh/chemtrade/MainActivity.kt"
manifest = ROOT / "AndroidManifest.xml"

# The app must never request SEND_SMS from ordinary users. Android does not
# allow silent outbound SMS without that dangerous permission. Use the user's
# installed SMS app instead; it requires no SEND_SMS permission and lets the
# user explicitly press Send.

e = entry.read_text(encoding="utf-8")
e = re.sub(r'^import android\.Manifest\n', '', e, flags=re.M)
e = re.sub(r'^import android\.content\.pm\.PackageManager\n', '', e, flags=re.M)
e = re.sub(r'^import android\.telephony\.SmsManager\n', '', e, flags=re.M)
e = re.sub(r'^import androidx\.activity\.compose\.rememberLauncherForActivityResult\n', '', e, flags=re.M)
e = re.sub(r'^import androidx\.activity\.result\.contract\.ActivityResultContracts\n', '', e, flags=re.M)
e = re.sub(r'^import androidx\.core\.content\.ContextCompat\n', '', e, flags=re.M)
if 'import android.net.Uri' not in e:
    e = e.replace('import android.content.Intent\n', 'import android.content.Intent\nimport android.net.Uri\n', 1)

# Remove the old direct-SmsManager sender.
e = re.sub(
    r'    fun sendVerificationSms\(targetPhone: String, verificationCode: String\) \{.*?\n    \}\n\n    fun requestOtp\(\) \{',
    '''    fun prepareVerificationSms(targetPhone: String, verificationCode: String) {
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

    fun requestOtp() {''',
    e,
    flags=re.S,
)

# Replace the requestOtp body with permission-free SMS composer flow.
e = re.sub(
    r'    fun requestOtp\(\) \{.*?\n    \}\n\n    val permissionLauncher = rememberLauncherForActivityResult.*?\n    }\n\n    Surface',
    '''    fun requestOtp() {
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

    Surface''',
    e,
    flags=re.S,
)

# Replace resend callback's direct SMS call.
e = e.replace(
    '''                    try { sendVerificationSms(phone, otp) }
                    catch (e: Exception) { error = e.message ?: "ارسال مجدد ناموفق بود." }''',
    '''                    prepareVerificationSms(phone, otp)''',
    1,
)
entry.write_text(e, encoding="utf-8")

m = main.read_text(encoding="utf-8")
# Remove direct SMS permission machinery from the admin flow.
m = re.sub(r'^import android\.Manifest\n', '', m, flags=re.M)
m = re.sub(r'^import android\.content\.pm\.PackageManager\n', '', m, flags=re.M)
m = re.sub(r'^import android\.telephony\.SmsManager\n', '', m, flags=re.M)
m = re.sub(r'^import androidx\.activity\.compose\.rememberLauncherForActivityResult\n', '', m, flags=re.M)
m = re.sub(r'^import androidx\.activity\.result\.contract\.ActivityResultContracts\n', '', m, flags=re.M)
m = re.sub(r'^import androidx\.core\.content\.ContextCompat\n', '', m, flags=re.M)
if 'import android.net.Uri' not in m:
    m = m.replace('import android.content.Intent\n', 'import android.content.Intent\nimport android.net.Uri\n', 1)

# Make the visible header logo itself the reliable hidden admin entry point.
m = re.sub(r'\n@Composable\nprivate fun HiddenAdminBrand\(onUnlock: \(\) -> Unit\) \{.*?\n\}\n\n@Composable\nprivate fun AdminDashboard', '''
@Composable
private fun HiddenAdminBrand(onUnlock: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onUnlock)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(62.dp))
        Spacer(Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ChemLink", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Navy)
            Text("بازار حرفه‌ای مواد اولیه", color = Gold, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminDashboard''', m, flags=re.S)

# Add combinedClickable import if absent.
if 'import androidx.compose.foundation.combinedClickable' not in m:
    m = m.replace('import androidx.compose.foundation.clickable\n', 'import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.combinedClickable\n', 1)

# Replace admin OTP sender and remove its permission launcher.
m = re.sub(
    r'    fun sendAdminOtp\(\) \{.*?\n    }\n    val permissionLauncher = rememberLauncherForActivityResult.*?\n    AlertDialog',
    '''    fun sendAdminOtp() {
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
    AlertDialog''',
    m,
    flags=re.S,
)

m = re.sub(
    r'1 -> \{ sending = true; if \(ContextCompat\.checkSelfPermission\(context, Manifest\.permission\.SEND_SMS\) == PackageManager\.PERMISSION_GRANTED\) sendAdminOtp\(\) else permissionLauncher\.launch\(Manifest\.permission\.SEND_SMS\) \}',
    '1 -> { sending = true; sendAdminOtp() }',
    m,
)

# If the previous workflow patch left the header logo without a gesture,
# attach the same long-press handler to the logo.
old = 'Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(44.dp))'
new = '''Image(
                    painterResource(R.drawable.ic_chemtrade_logo),
                    "ChemLink",
                    Modifier.size(44.dp).combinedClickable(onClick = {}, onLongClick = { showAdminLogin = true })
                )'''
m = m.replace(old, new, 1)
main.write_text(m, encoding="utf-8")

# No SEND_SMS permission is needed anywhere in this build.
a = manifest.read_text(encoding="utf-8")
a = re.sub(r'\n\s*<uses-permission android:name="android\.permission\.SEND_SMS"\s*/>', '', a)
manifest.write_text(a, encoding="utf-8")

print("SMS permission removed; consumer/admin SMS now use the system SMS composer; hidden admin long-press strengthened.")
