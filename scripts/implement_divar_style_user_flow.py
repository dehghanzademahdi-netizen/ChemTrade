from pathlib import Path
import re

main = Path('app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt')
s = main.read_text(encoding='utf-8')

# Imports for direct SMS OTP.
if 'import android.Manifest' not in s:
    s = s.replace('package com.dehghanzadeh.chemtrade\n\n', 'package com.dehghanzadeh.chemtrade\n\nimport android.Manifest\nimport android.content.pm.PackageManager\nimport android.telephony.SmsManager\n', 1)
if 'import androidx.core.content.ContextCompat' not in s:
    s = s.replace('import androidx.compose.ui.unit.dp\n', 'import androidx.compose.ui.unit.dp\nimport androidx.core.content.ContextCompat\n', 1)
if 'import java.security.SecureRandom' not in s:
    s = s.replace('import java.security.MessageDigest\n', 'import java.security.MessageDigest\nimport java.security.SecureRandom\n', 1)

# Normal users stay logged in until explicit logout. Admin sessions must never be restored.
s = s.replace('var admin by remember { mutableStateOf(prefs.getBoolean("adminSession", false)) }', 'var admin by remember { mutableStateOf(false) }')

# Add a dedicated My Ads tab while keeping the existing account/settings tabs.
old_nav = '''NavigationBarItem(selected = page == 2, onClick = { page = 2 }, icon = { Text("●") }, label = { Text("حساب") })\n                NavigationBarItem(selected = page == 3, onClick = { page = 3 }, icon = { Text("⚙") }, label = { Text("تنظیمات") })'''
new_nav = '''NavigationBarItem(selected = page == 2, onClick = { page = 2 }, icon = { Text("●") }, label = { Text("حساب") })\n                NavigationBarItem(selected = page == 4, onClick = { page = 4 }, icon = { Text("▤") }, label = { Text("آگهی‌های من") })\n                NavigationBarItem(selected = page == 3, onClick = { page = 3 }, icon = { Text("⚙") }, label = { Text("تنظیمات") })'''
if old_nav in s and 'label = { Text("آگهی‌های من") }' not in s:
    s = s.replace(old_nav, new_nav, 1)

# Route page 4 to the persistent owner-specific ads list.
old_route = '''page == 2 -> AccountPage(Modifier.padding(padding), phone, { page = 0 }, ::logout) {\n                editOffer = null\n                showOfferForm = true\n            }\n            else -> SettingsPage(Modifier.padding(padding)) { showAdminLogin = true }'''
new_route = '''page == 2 -> AccountPage(Modifier.padding(padding), phone, { page = 0 }, ::logout) {\n                editOffer = null\n                showOfferForm = true\n            }\n            page == 4 -> MyOffersPage(Modifier.padding(padding), offers, phone,\n                open = { detailsOffer = it },\n                requestCorrection = { offer, message ->\n                    saveOffer(offer.copy(status = Status.PENDING, reason = message))\n                },\n                edit = { editOffer = it; showOfferForm = true }\n            )\n            else -> SettingsPage(Modifier.padding(padding)) { showAdminLogin = true }'''
if old_route not in s:
    raise SystemExit('Account/settings routing target not found')
s = s.replace(old_route, new_route, 1)

# Replace password-style admin dialog with SMS OTP. It deliberately does not create an admin session in prefs.
start = re.search(r'@Composable\s+private fun AdminLoginDialog\(', s)
if not start:
    raise SystemExit('AdminLoginDialog not found')
end = re.search(r'\n@Composable\s+private fun ', s[start.start()+1:])
if not end:
    raise SystemExit('Next composable after AdminLoginDialog not found')
end_pos = start.start() + 1 + end.start()
admin_dialog = r'''@Composable
private fun AdminLoginDialog(close: () -> Unit, success: () -> Unit) {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    fun sendCode() {
        val target = digits(phone).filter(Char::isDigit)
        if (target.length != 11 || !target.startsWith("09")) { error = "شماره خط مدیریت را درست وارد کنید."; return }
        expected = SecureRandom().nextInt(900000).plus(100000).toString()
        val message = "ChemLink | مدیریت\nکد تأیید ورود پنل مدیریت: $expected\nاین کد مخصوص مدیریت است."
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                error = "برای ارسال کد مدیریت، مجوز پیامک را فعال کنید."
                return
            }
            @Suppress("DEPRECATION")
            SmsManager.getDefault().sendTextMessage(target, null, message, null, null)
            sent = true
            error = "کد مدیریت ارسال شد."
        } catch (e: Exception) {
            error = "ارسال کد مدیریت ناموفق بود."
        }
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("ورود مخصوص مدیریت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ورود مدیریت فقط با کد پیامکی انجام می‌شود؛ رمز ثابت حذف شده است.")
                OutlinedTextField(value = phone, onValueChange = { phone = digits(it).filter(Char::isDigit).take(11) }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("شماره خط مدیریت") })
                if (sent) OutlinedTextField(value = code, onValueChange = { code = digits(it).filter(Char::isDigit).take(6) }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("کد تأیید مدیریت") })
                if (error.isNotBlank()) Text(error, color = if (sent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = { if (!sent) sendCode() else if (code == expected && expected.isNotBlank()) success() else error = "کد مدیریت صحیح نیست." }) {
                Text(if (sent) "تأیید و ورود به مدیریت" else "ارسال کد مدیریت")
            }
        },
        dismissButton = { TextButton(onClick = close) { Text("انصراف") } }
    )
}

'''
s = s[:start.start()] + admin_dialog + s[end_pos:]

# Add a dedicated user-facing My Ads screen and correction request dialog before AdminDashboard.
marker = '@Composable\nprivate fun AdminDashboard'
if marker not in s:
    marker = '@Composable\nprivate fun AdminDashboard'
my_ads = r'''@Composable
private fun MyOffersPage(
    modifier: Modifier,
    offers: List<Offer>,
    phone: String,
    open: (Offer) -> Unit,
    requestCorrection: (Offer, String) -> Unit,
    edit: (Offer) -> Unit
) {
    var correctionTarget by remember { mutableStateOf<Offer?>(null) }
    val mine = offers.filter { it.owner == phone || (it.owner.isBlank() && it.phone == phone) }
    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("آگهی‌های من", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) }
        if (mine.isEmpty()) item { Text("هنوز آگهی‌ای ثبت نکرده‌اید.") }
        items(mine, key = { it.id }) { offer ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(offer.name, fontWeight = FontWeight.Bold)
                    Text("وضعیت: ${when (offer.status) { Status.PENDING -> "در انتظار بررسی"; Status.APPROVED -> "منتشر شده"; Status.REJECTED -> "نیازمند اصلاح" }}")
                    Text("تحویل: ${offer.place} • ${offer.time}")
                    if (offer.reason.isNotBlank()) Text("پیام مدیریت: ${offer.reason}", color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { open(offer) }) { Text("مشاهده") }
                        if (offer.status != Status.APPROVED) OutlinedButton(onClick = { edit(offer) }) { Text("ویرایش") }
                        if (offer.status == Status.APPROVED) Button(onClick = { correctionTarget = offer }) { Text("درخواست اصلاح") }
                    }
                }
            }
        }
    }
    correctionTarget?.let { offer ->
        var message by remember(offer.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { correctionTarget = null },
            title = { Text("درخواست اصلاح آگهی") },
            text = { OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("توضیح اصلاح موردنظر") }) },
            confirmButton = { Button(enabled = message.isNotBlank(), onClick = { requestCorrection(offer, "درخواست اصلاح مشتری: $message"); correctionTarget = null }) { Text("ارسال برای بررسی مدیریت") } },
            dismissButton = { TextButton(onClick = { correctionTarget = null }) { Text("انصراف") } }
        )
    }
}

'''
# Current file uses the same declaration style but may not have newline exactly.
idx = s.find('@Composable\nprivate fun AdminDashboard')
if idx < 0:
    raise SystemExit('AdminDashboard marker not found')
s = s[:idx] + my_ads + s[idx:]

main.write_text(s, encoding='utf-8')

# Manifest permission + activity for SMS gateway remain available.
manifest = Path('app/src/main/AndroidManifest.xml')
m = manifest.read_text(encoding='utf-8')
if 'android.permission.SEND_SMS' not in m:
    m = m.replace('<manifest', '<manifest\n    xmlns:android="http://schemas.android.com/apk/res/android"', 1) if 'xmlns:android=' not in m else m
    m = m.replace('<application', '    <uses-permission android:name="android.permission.SEND_SMS" />\n\n    <application', 1)
manifest.write_text(m, encoding='utf-8')
print('Divar-style My Ads + SMS-only admin flow applied')
