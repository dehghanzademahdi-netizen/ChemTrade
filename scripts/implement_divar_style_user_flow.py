from pathlib import Path
import re

MAIN = Path('app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt')
s = MAIN.read_text(encoding='utf-8')

# Imports needed by the dedicated management OTP flow.
if 'import android.Manifest' not in s:
    s = s.replace('import android.content.Context\n', 'import android.Manifest\nimport android.content.Context\n', 1)
if 'import android.content.pm.PackageManager' not in s:
    s = s.replace('import android.content.Intent\n', 'import android.content.Intent\nimport android.content.pm.PackageManager\n', 1)
if 'import android.telephony.SmsManager' not in s:
    s = s.replace('import android.os.Bundle\n', 'import android.os.Bundle\nimport android.telephony.SmsManager\n', 1)
if 'import androidx.core.content.ContextCompat' not in s:
    s = s.replace('import androidx.compose.ui.unit.dp\n', 'import androidx.compose.ui.unit.dp\nimport androidx.core.content.ContextCompat\n', 1)
if 'import java.security.SecureRandom' not in s:
    s = s.replace('import java.security.MessageDigest\n', 'import java.security.MessageDigest\nimport java.security.SecureRandom\n', 1)

# Management is never restored from SharedPreferences; only a successful OTP
# unlock creates an in-memory admin session.
s = s.replace('var admin by remember { mutableStateOf(prefs.getBoolean("adminSession", false)) }', 'var admin by remember { mutableStateOf(false) }')
s = s.replace('prefs.edit().putBoolean("adminSession", true).apply()', 'prefs.edit().remove("adminSession").apply()')

# Add My Offers to the normal user navigation.
old_nav = '''NavigationBarItem(selected = page == 2, onClick = { page = 2 }, icon = { Text("●") }, label = { Text("حساب") })
                NavigationBarItem(selected = page == 3, onClick = { page = 3 }, icon = { Text("⚙") }, label = { Text("تنظیمات") })'''
new_nav = '''NavigationBarItem(selected = page == 2, onClick = { page = 2 }, icon = { Text("●") }, label = { Text("حساب") })
                NavigationBarItem(selected = page == 4, onClick = { page = 4 }, icon = { Text("▤") }, label = { Text("آگهی‌های من") })
                NavigationBarItem(selected = page == 3, onClick = { page = 3 }, icon = { Text("⚙") }, label = { Text("تنظیمات") })'''
if 'label = { Text("آگهی‌های من") }' not in s:
    if old_nav not in s:
        raise SystemExit('Navigation target not found')
    s = s.replace(old_nav, new_nav, 1)

old_route = '''page == 2 -> AccountPage(Modifier.padding(padding), phone, { page = 0 }, ::logout) {
                editOffer = null
                showOfferForm = true
            }
            else -> SettingsPage(Modifier.padding(padding)) { showAdminLogin = true }'''
new_route = '''page == 2 -> AccountPage(Modifier.padding(padding), phone, { page = 0 }, ::logout) {
                editOffer = null
                showOfferForm = true
            }
            page == 4 -> MyOffersPage(
                modifier = Modifier.padding(padding),
                offers = offers,
                phone = phone,
                open = { detailsOffer = it },
                requestCorrection = { offer, message -> saveOffer(offer.copy(status = Status.PENDING, reason = message)) },
                edit = { editOffer = it; showOfferForm = true }
            )
            else -> SettingsPage(Modifier.padding(padding)) { showAdminLogin = true }'''
if 'page == 4 -> MyOffersPage(' not in s:
    if old_route not in s:
        raise SystemExit('Account/settings routing target not found')
    s = s.replace(old_route, new_route, 1)

# Replace the old password dialog with SMS OTP restricted to the fixed admin line.
start = s.find('@Composable\nprivate fun AdminLoginDialog(')
if start < 0:
    raise SystemExit('AdminLoginDialog not found')
end = s.find('\nprivate fun loadOffers(', start)
if end < 0:
    raise SystemExit('loadOffers marker not found')
admin_block = r'''@Composable
private fun AdminLoginDialog(close: () -> Unit, success: () -> Unit) {
    val context = LocalContext.current
    val adminPhone = "09357236476"
    var code by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    fun sendAdminOtp() {
        val otp = SecureRandom().nextInt(900000).plus(100000).toString()
        val message = "ChemLink | مدیریت\nکد تأیید ورود پنل مدیریت: $otp\nاین کد مخصوص مدیریت است."
        try {
            @Suppress("DEPRECATION")
            val manager = SmsManager.getDefault()
            val parts = manager.divideMessage(message)
            if (parts.size == 1) manager.sendTextMessage(adminPhone, null, message, null, null)
            else manager.sendMultipartTextMessage(adminPhone, null, parts, null, null)
            expected = otp
            sent = true
            error = "کد مخصوص مدیریت ارسال شد."
        } catch (e: Exception) {
            error = "ارسال کد مدیریت ناموفق بود. مجوز SMS و سیم‌کارت را بررسی کنید."
        } finally {
            sending = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendAdminOtp() else {
            sending = false
            error = "برای ورود مدیریت باید مجوز ارسال پیامک را تأیید کنید."
        }
    }

    AlertDialog(
        onDismissRequest = close,
        title = { Text("ورود مخصوص مدیریت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("این بخش فقط برای مدیریت است.")
                OutlinedTextField(
                    value = adminPhone,
                    onValueChange = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("شماره مدیریت") }
                )
                if (!sent) Text("کد یک‌بارمصرف به شماره مدیریت ارسال می‌شود.")
                if (sent) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = digits(it).filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("کد تأیید مدیریت") }
                    )
                }
                if (error.isNotBlank()) Text(error, color = if (sent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                enabled = !sending,
                onClick = {
                    if (!sent) {
                        sending = true
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) sendAdminOtp()
                        else permissionLauncher.launch(Manifest.permission.SEND_SMS)
                    } else if (code == expected && expected.isNotBlank()) {
                        success()
                    } else error = "کد مدیریت صحیح نیست."
                }
            ) { Text(if (sent) "تأیید و ورود" else "ارسال کد مدیریت") }
        },
        dismissButton = { TextButton(onClick = close) { Text("انصراف") } }
    )
}

@Composable
private fun MyOffersPage(
    modifier: Modifier,
    offers: List<Offer>,
    phone: String,
    open: (Offer) -> Unit,
    requestCorrection: (Offer, String) -> Unit,
    edit: (Offer) -> Unit
) {
    var correctionTarget by remember { mutableStateOf<Offer?>(null) }
    var correctionText by remember { mutableStateOf("") }
    val mine = offers.filter { it.owner == phone || (it.owner.isBlank() && it.phone == phone) }

    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("آگهی‌های من", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("وضعیت آگهی‌ها، اصلاحات و آگهی‌های منتشرشده را اینجا ببینید.")
        }
        if (mine.isEmpty()) item { Text("هنوز آگهی‌ای ثبت نکرده‌اید.") }
        items(mine, key = { it.id }) { offer ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(offer.name, fontWeight = FontWeight.Bold)
                    Text(
                        when (offer.status) {
                            Status.PENDING -> "در انتظار بررسی مدیریت"
                            Status.REJECTED -> "نیازمند اصلاح"
                            Status.APPROVED -> "منتشر شده"
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (offer.status == Status.REJECTED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    if (offer.status == Status.APPROVED) {
                        Text("قیمت نهایی رسمی: ${offer.publishedOfficial}")
                        Text("قیمت نهایی غیررسمی: ${offer.publishedMarket}")
                    }
                    Text("تحویل: ${offer.place} • ${offer.time}")
                    if (offer.reason.isNotBlank()) Text("پیام مدیریت: ${offer.reason}", color = MaterialTheme.colorScheme.error)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { open(offer) }) { Text("مشاهده") }
                        if (offer.status != Status.APPROVED) OutlinedButton(onClick = { edit(offer) }) { Text("ویرایش") }
                        if (offer.status == Status.APPROVED) Button(onClick = { correctionText = ""; correctionTarget = offer }) { Text("درخواست اصلاح") }
                    }
                }
            }
        }
    }

    correctionTarget?.let { offer ->
        AlertDialog(
            onDismissRequest = { correctionTarget = null },
            title = { Text("درخواست اصلاح آگهی") },
            text = {
                OutlinedTextField(
                    value = correctionText,
                    onValueChange = { correctionText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("توضیح اصلاح موردنظر") }
                )
            },
            confirmButton = {
                Button(
                    enabled = correctionText.isNotBlank(),
                    onClick = {
                        requestCorrection(offer, correctionText)
                        correctionTarget = null
                    }
                ) { Text("ارسال برای بررسی") }
            },
            dismissButton = { TextButton(onClick = { correctionTarget = null }) { Text("انصراف") } }
        )
    }
}

'''
s = s[:start] + admin_block + s[end:]

MAIN.write_text(s, encoding='utf-8')
print('ChemLink user/admin flow patch applied successfully')
