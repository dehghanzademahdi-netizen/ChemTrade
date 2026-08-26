package com.dehghanzadeh.chemtrade

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.security.SecureRandom

class SmsGatewayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SmsGatewayScreen() }
    }
}

private val GatewayNavy = Color(0xFF0B1F33)
private val GatewayGold = Color(0xFFC8A24A)
private val GatewayCream = Color(0xFFFBF6EE)
private val GatewayGreen = Color(0xFF2E7D5B)
private val GatewayRed = Color(0xFFB3261E)

@Composable
private fun SmsGatewayScreen() {
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("سیم‌کارت خط مدیریت را در همین گوشی قرار دهید.") }
    var statusColor by remember { mutableStateOf(GatewayNavy) }

    fun generateCode() {
        code = SecureRandom().nextInt(900000).plus(100000).toString()
    }

    fun sendOtp() {
        val normalized = phone.filter { it.isDigit() }
        if (normalized.length < 10) {
            status = "شماره موبایل را درست وارد کنید."
            statusColor = GatewayRed
            return
        }
        if (code.length != 6) generateCode()
        val message = "ChemLink\nکد تایید شما: $code\nاین کد را در اختیار دیگران قرار ندهید."
        try {
            @Suppress("DEPRECATION")
            val manager = SmsManager.getDefault()
            val parts = manager.divideMessage(message)
            if (parts.size == 1) manager.sendTextMessage(normalized, null, message, null, null)
            else manager.sendMultipartTextMessage(normalized, null, parts, null, null)
            status = "درخواست ارسال شد. کد $code به شماره $normalized ارسال شد."
            statusColor = GatewayGreen
        } catch (e: Exception) {
            status = "ارسال ناموفق بود: ${e.message ?: "خطای سیم‌کارت یا مجوز"}"
            statusColor = GatewayRed
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) sendOtp() else {
            status = "برای ارسال پیامک باید مجوز SMS را تایید کنید."
            statusColor = GatewayRed
        }
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = GatewayNavy, secondary = GatewayGold, background = GatewayCream, surface = Color.White)) {
        Surface(modifier = Modifier.fillMaxSize(), color = GatewayCream) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.height(16.dp))
                Text("ChemLink", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = GatewayNavy)
                Text("آزمایش درگاه پیامکی خط مدیریت", color = GatewayGold, fontWeight = FontWeight.Bold)

                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("این نسخه آزمایشی، پیامک را مستقیماً با سیم‌کارت همین گوشی ارسال می‌کند.", textAlign = TextAlign.Center)
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("شماره دریافت‌کننده") }, placeholder = { Text("0912...") })
                        OutlinedTextField(value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("کد تایید") }, placeholder = { Text("۶ رقمی") })
                        OutlinedButton(onClick = { generateCode() }, modifier = Modifier.fillMaxWidth()) { Text("ساخت کد آزمایشی") }
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) sendOtp()
                                else permissionLauncher.launch(Manifest.permission.SEND_SMS)
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GatewayNavy)
                        ) { Text("ارسال کد با سیم‌کارت این گوشی") }
                    }
                }

                Text(status, color = statusColor, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                Text("برای تست واقعی، این اپ را روی گوشی‌ای نصب کن که سیم‌کارت خط مدیریت داخل آن است و یک شماره دیگر را به‌عنوان دریافت‌کننده وارد کن.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = GatewayNavy)
            }
        }
    }
}
