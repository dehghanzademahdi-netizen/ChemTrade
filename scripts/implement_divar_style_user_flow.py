from pathlib import Path
import re

main = Path('app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt')
s = main.read_text(encoding='utf-8')

if 'import android.Manifest' not in s:
    s = s.replace('package com.dehghanzadeh.chemtrade\n\n', 'package com.dehghanzadeh.chemtrade\n\nimport android.Manifest\nimport android.content.pm.PackageManager\nimport android.telephony.SmsManager\n', 1)
if 'import androidx.core.content.ContextCompat' not in s:
    s = s.replace('import androidx.compose.ui.unit.dp\n', 'import androidx.compose.ui.unit.dp\nimport androidx.core.content.ContextCompat\n', 1)
if 'import java.security.SecureRandom' not in s:
    s = s.replace('import java.security.MessageDigest\n', 'import java.security.MessageDigest\nimport java.security.SecureRandom\n', 1)

s = s.replace('var admin by remember { mutableStateOf(prefs.getBoolean("adminSession", false)) }', 'var admin by remember { mutableStateOf(false) }')

old_nav = '''NavigationBarItem(selected = page == 2, onClick = { page = 2 }, icon = { Text("●") }, label = { Text("حساب") })\n                NavigationBarItem(selected = page == 3, onClick = { page = 3 }, icon = { Text("⚙") }, label = { Text("تنظیمات") })'''
new_nav = '''NavigationBarItem(selected = page == 2, onClick = { page = 2 }, icon = { Text("●") }, label = { Text("حساب") })\n                NavigationBarItem(selected = page == 4, onClick = { page = 4 }, icon = { Text("▤") }, label = { Text("آگهی‌های من") })\n                NavigationBarItem(selected = page == 3, onClick = { page = 3 }, icon = { Text("⚙") }, label = { Text("تنظیمات") })'''
if old_nav in s and 'label = { Text("آگهی‌های من") }' not in s:
    s = s.replace(old_nav, new_nav, 1)

old_route = '''page == 2 -> AccountPage(Modifier.padding(padding), phone, { page = 0 }, ::logout) {\n                editOffer = null\n                showOfferForm = true\n            }\n            else -> SettingsPage(Modifier.padding(padding)) { showAdminLogin = true }'''
new_route = '''page == 2 -> AccountPage(Modifier.padding(padding), phone, { page = 0 }, ::logout) {\n                editOffer = null\n                showOfferForm = true\n            }\n            page == 4 -> MyOffersPage(Modifier.padding(padding), offers, phone,\n                open = { detailsOffer = it },\n                requestCorrection = { offer, message -> saveOffer(offer.copy(status = Status.PENDING, reason = message)) },\n                edit = { editOffer = it; showOfferForm = true }\n            )\n            else -> SettingsPage(Modifier.padding(padding)) { showAdminLogin = true }'''
if old_route not in s:
    raise SystemExit('Account/settings routing target not found')
s = s.replace(old_route, new_route, 1)

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
    var phone by remember { mutableStateOf(context.getSharedPreferences("chemlink", Context.MODE_PRIVATE).getString("adminPhone", "").orEmpty()) }
    var code by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var permissionRequested by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionRequested = false
        if (granted) sendAdminOtp(context, phone, { value -> expected = value; sent = true; error = "کد مخصوص مدیریت ارسال شد." }, { error = it })
        else error = "برای ورود مدیریت باید مجوز ارسال پیامک را تأیید کنید."
    }
    fun requestSend() {
        val target = digits(phone).filter(Char::isDigit)
        if (target.length != 11 || !target.startsWith("09")) { error = "شماره خط مدیریت را درست وارد کنید."; return }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            if (!permissionRequested) { permissionRequested = true; permissionLauncher.launch(Manifest.permission.SEND_SMS) }
        } else sendAdminOtp(context, target, { value -> expected = value; sent = true; error = "کد مخصوص مدیریت ارسال شد." }, { error = it })
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text("ورود مخصوص مدیریت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ورود مدیریت فقط با کد پیامکی انجام می‌شود و رمز ثابت ندارد.")
                OutlinedTextField(value = phone, onValueChange = { phone = digits(it).filter(Char::isDigit).take(11) }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("شماره خط مدیریت") })
                if (sent) OutlinedTextField(value = code, onValueChange = { code = digits(it).filter(Char::isDigit).take(6) }, singleLine = true, modifier = Modifier.fillMaxWidth(), label = { Text("کد تأیید مدیریت") })
                if (error.isNotBlank()) Text(error, color = if (sent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(onClick = { if (!sent) requestSend() else if (code == expected && expected.isNotBlank()) { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE).edit().putString("adminPhone", digits(phone).filter(Char::isDigit)).apply(); success() } else error = "کد مدیریت صحیح نیست." }) { Text(if (sent) "تأیید و ورود به مدیریت" else "ارسال کد مدیریت") } },
        dismissButton = { TextButton(onClick = close) { Text("انصراف") } }
    )
}

private fun sendAdminOtp(context: Context, phone: String, ok: (String) -> Unit, fail: (String) -> Unit) {
    val target = digits(phone).filter(Char::isDigit)
    val otp = SecureRandom().nextInt(900000).plus(100000).toString()
    val message = "ChemLink | مدیریت\nکد تأیید ورود پنل مدیریت: $otp\nاین کد مخصوص مدیریت است."
    try {
        @Suppress("DEPRECATION")
        val manager = SmsManager.getDefault()
        manager.sendTextMessage(target, null, message, null, null)
        ok(otp)
    } catch (e: Exception) { fail("ارسال کد مدیریت ناموفق بود: ${e.message ?: "خطای پیامک"}") }
}

'''
s = s[:start.start()] + admin_dialog + s[end_pos:]

idx = s.find('@Composable\nprivate fun AdminDashboard')
if idx < 0:
    raise SystemExit('AdminDashboard marker not found')
my_ads = r'''@Composable
private fun MyOffersPage(modifier: Modifier, offers: List<Offer>, phone: String, open: (Offer) -> Unit, requestCorrection: (Offer, String) -> Unit, edit: (Offer) -> Unit) {
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
        AlertDialog(onDismissRequest = { correctionTarget = null }, title = { Text("درخواست اصلاح آگهی") }, text = { OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("توضیح اصلاح موردنظر") }) }, confirmButton = { Button(enabled = message.isNotBlank(), onClick = { requestCorrection(offer, "درخواست اصلاح مشتری: $message"); correctionTarget = null }) { Text("ارسال برای بررسی مدیریت") } }, dismissButton = { TextButton(onClick = { correctionTarget = null }) { Text("انصراف") } })
    }
}

'''
s = s[:idx] + my_ads + s[idx:]

main.write_text(s, encoding='utf-8')
print('Divar-style My Ads + SMS-only admin flow applied')
