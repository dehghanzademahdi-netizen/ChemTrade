from pathlib import Path

M = Path('app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt')
m = M.read_text(encoding='utf-8')
start = m.find('@Composable\nprivate fun AdminLoginDialog')
if start < 0:
    raise SystemExit('AdminLoginDialog not found')
next_pos = m.find('\n@Composable', start + 10)
end = len(m) if next_pos < 0 else next_pos
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

    fun sendAdminOtp() {
        val code = SecureRandom().nextInt(900000).plus(100000).toString()
        expected = code
        val sms = "ChemLink | کد ورود مدیریت: $code\nاین کد را در اختیار دیگران قرار ندهید."
        try {
            @Suppress("DEPRECATION")
            val manager = SmsManager.getDefault()
            val parts = manager.divideMessage(sms)
            if (parts.size == 1) manager.sendTextMessage(ADMIN_PHONE, null, sms, null, null)
            else manager.sendMultipartTextMessage(ADMIN_PHONE, null, parts, null, null)
            step = 1
            message = "کد ورود مدیریت ارسال شد."
            error = ""
        } catch (_: Exception) {
            error = "ارسال پیامک ناموفق بود. مجوز SMS یا سیم‌کارت را بررسی کنید."
        }
        sending = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendAdminOtp()
        else {
            sending = false
            error = "برای ورود مدیریت باید مجوز ارسال SMS را فعال کنید."
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
                    error = ""; message = ""; sending = true
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) sendAdminOtp()
                    else permissionLauncher.launch(Manifest.permission.SEND_SMS)
                } else {
                    if (otp == expected && expected.isNotBlank()) onSuccess() else error = "کد واردشده صحیح نیست."
                }
            }) { Text(if (step == 0) "ادامه" else "ورود به مدیریت") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("انصراف") } }
    )
}
'''
M.write_text(m[:start] + admin + m[end:], encoding='utf-8')
print('ADMIN OTP PERMISSION PATCH OK')
