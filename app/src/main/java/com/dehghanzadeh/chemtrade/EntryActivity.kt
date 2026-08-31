package com.dehghanzadeh.chemtrade

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import java.security.SecureRandom

class EntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChemLinkEntryApp() }
    }
}

private val EntryNavy = Color(0xFF0B1F33)
private val EntryBlue = Color(0xFF1565A8)
private val EntryGold = Color(0xFFC89618)
private val EntryCream = Color(0xFFFBF8F2)

@Composable
private fun ChemLinkEntryApp() {
    val colors = lightColorScheme(
        primary = EntryNavy,
        secondary = EntryGold,
        background = EntryCream,
        surface = Color.White,
        onPrimary = Color.White,
        onBackground = EntryNavy
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = colors) { EntryFlow() }
    }
}

@Composable
private fun EntryFlow() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var expectedCode by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }

    fun sendVerificationSms(targetPhone: String, verificationCode: String) {
        val message = "ChemLink\nکد تایید شما: $verificationCode\nاین کد را در اختیار دیگران قرار ندهید."
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$targetPhone")
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = EntryCream) {
        when (step) {
            0 -> WelcomeScreen { step = 1 }
            1 -> ServiceLoginScreen(
                phone = phone,
                onPhoneChange = { phone = it.filter(Char::isDigit).take(11) },
                onContinue = {
                    if (phone.length != 11 || !phone.startsWith("09")) {
                        error = "لطفاً شماره موبایل ۱۱ رقمی را درست وارد کنید."
                    } else {
                        error = ""
                        expectedCode = SecureRandom().nextInt(900000).plus(100000).toString()
                        sendVerificationSms(phone, expectedCode)
                        step = 2
                    }
                },
                error = error
            )
            else -> VerifyCodeScreen(
                phone = phone,
                code = code,
                onCodeChange = { code = it.filter(Char::isDigit).take(6) },
                onVerify = {
                    if (code == expectedCode && expectedCode.isNotBlank()) {
                        context.getSharedPreferences("chemlink", Context.MODE_PRIVATE).edit()
                            .putString("phone", phone)
                            .putString("name", "")
                            .putString("email", "")
                            .putString("address", "")
                            .putString("company", "")
                            .putString("type", "Consumer")
                            .apply()
                        context.startActivity(Intent(context, MainActivity::class.java))
                        (context as? ComponentActivity)?.finish()
                    } else {
                        error = "کد واردشده صحیح نیست."
                    }
                },
                onResend = {
                    expectedCode = SecureRandom().nextInt(900000).plus(100000).toString()
                    error = ""
                    sendVerificationSms(phone, expectedCode)
                },
                onBack = { code = ""; error = ""; step = 1 },
                error = error
            )
        }
    }
}

@Composable
private fun BrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_chemtrade_logo),
        contentDescription = "ChemLink",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandLogo(Modifier.size(116.dp))
        Spacer(Modifier.height(22.dp))
        Text("ChemLink", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = EntryNavy)
        Spacer(Modifier.height(8.dp))
        Text("پل هوشمند تأمین و فروش مواد اولیه", style = MaterialTheme.typography.titleMedium, color = EntryGold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text("بازار حرفه‌ای برای ارتباط مطمئن تأمین‌کنندگان، فروشندگان و مصرف‌کنندگان مواد اولیه", textAlign = TextAlign.Center, color = EntryNavy)
        Spacer(Modifier.height(36.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = EntryNavy)) {
            Text("ورود به ChemLink", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ServiceLoginScreen(phone: String, onPhoneChange: (String) -> Unit, onContinue: () -> Unit, error: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandLogo(Modifier.size(86.dp))
        Spacer(Modifier.height(24.dp))
        Text("دریافت خدمات تأمین و فروش", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = EntryNavy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("لطفاً وارد حساب کاربری شوید.", color = EntryNavy, textAlign = TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("شماره موبایل") },
            placeholder = { Text("0912xxxxxxx") },
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = EntryBlue) }
        )
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(18.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = EntryNavy)) {
            Text("دریافت کد تأیید")
        }
    }
}

@Composable
private fun VerifyCodeScreen(phone: String, code: String, onCodeChange: (String) -> Unit, onVerify: () -> Unit, onResend: () -> Unit, onBack: () -> Unit, error: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        BrandLogo(Modifier.size(86.dp))
        Spacer(Modifier.height(24.dp))
        Text("تأیید شماره موبایل", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = EntryNavy)
        Spacer(Modifier.height(10.dp))
        Text("پیامک آماده ارسال شد. در برنامه پیامک روی ارسال بزنید، سپس کد را اینجا وارد کنید.", textAlign = TextAlign.Center, color = EntryNavy)
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(value = code, onValueChange = onCodeChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("کد ۶ رقمی") }, placeholder = { Text("------") })
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Spacer(Modifier.height(18.dp))
        Button(onClick = onVerify, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = EntryNavy)) { Text("تأیید و ورود") }
        TextButton(onClick = onResend) { Text("ارسال مجدد کد", color = EntryGold) }
        TextButton(onClick = onBack) { Text("ویرایش شماره موبایل", color = EntryGold) }
    }
}
