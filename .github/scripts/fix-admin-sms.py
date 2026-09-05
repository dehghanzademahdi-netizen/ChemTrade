from pathlib import Path
import re

path = Path("app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt")
s = path.read_text(encoding="utf-8")

# The user login already uses ACTION_SENDTO. Make the hidden admin login use
# the exact same approach so Android opens the SMS app instead of requesting
# SEND_SMS permission or using SmsManager directly.
for imp in (
    "import android.Manifest\n",
    "import android.content.pm.PackageManager\n",
    "import android.telephony.SmsManager\n",
    "import androidx.activity.result.contract.ActivityResultContracts\n",
    "import androidx.activity.compose.rememberLauncherForActivityResult\n",
    "import androidx.core.content.ContextCompat\n",
):
    s = s.replace(imp, "")

start = s.find("@Composable\nprivate fun AdminLoginDialog")
end = s.find("\nprivate fun loadOffers", start)
if start < 0 or end < 0:
    raise SystemExit("AdminLoginDialog boundaries were not found")

new_function = '''@Composable
private fun AdminLoginDialog(close: () -> Unit, success: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    fun sendAdminOtp() {
        val otp = SecureRandom().nextInt(900000).plus(100000).toString()
        val sms = "ChemLink\\nکد تأیید ورود به مدیریت: $otp\\nاین کد را در اختیار دیگران قرار ندهید."
        expected = otp
        step = 2
        message = "پیامک آماده ارسال شد. در برنامه پیامک روی ارسال بزنید، سپس کد را اینجا وارد کنید."
        error = ""
        sending = false
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$ADMIN_PHONE")
                putExtra("sms_body", sms)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            error = "برنامه پیامک روی گوشی پیدا نشد."
        }
    }

    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (step == 0) "ورود مدیریت" else "تأیید دو مرحله‌ای مدیریت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (step) {
                    0 -> {
                        Text("دسترسی مدیریت محافظت شده است.")
                        OutlinedTextField(
                            password,
                            { password = digits(it).take(12) },
                            Modifier.fillMaxWidth(),
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            label = { Text("رمز مدیریت") }
                        )
                    }
                    1 -> Text("رمز صحیح است. برای مرحله دوم کد پیامکی را ارسال کنید.")
                    else -> {
                        Text("کد ۶ رقمی ارسال‌شده به شماره مدیریت را وارد کنید.")
                        OutlinedTextField(
                            code,
                            { code = digits(it).filter(Char::isDigit).take(6) },
                            Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("کد پیامکی") }
                        )
                    }
                }
                if (message.isNotBlank()) Text(message, color = Green)
                if (error.isNotBlank()) Text(error, color = Red)
            }
        },
        confirmButton = {
            Button(
                enabled = !sending,
                onClick = {
                    when (step) {
                        0 -> if (hash(password) == ADMIN_HASH) {
                            step = 1
                            error = ""
                        } else {
                            error = "رمز مدیریت صحیح نیست."
                        }
                        1 -> {
                            sending = true
                            sendAdminOtp()
                        }
                        else -> if (code == expected && expected.isNotBlank()) {
                            success()
                        } else {
                            error = "کد پیامکی صحیح نیست."
                        }
                    }
                }
            ) { Text(when (step) { 0 -> "ادامه"; 1 -> "ارسال کد پیامکی"; else -> "تأیید و ورود" }) }
        },
        dismissButton = { TextButton(close) { Text("انصراف") } }
    )
}
'''

s = s[:start] + new_function + s[end:]
path.write_text(s, encoding="utf-8")
print("Admin SMS flow patched successfully")
