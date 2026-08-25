package com.dehghanzadeh.chemtrade

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import java.security.MessageDigest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private enum class OfferStatus(val title: String) {
    Pending("در انتظار بررسی"),
    Approved("تأیید شده"),
    Rejected("رد شده")
}

private enum class AccountType(val title: String) {
    Supplier("تأمین‌کننده"),
    Consumer("مصرف‌کننده")
}

private data class UserProfile(
    val phone: String,
    val fullName: String,
    val email: String,
    val address: String,
    val company: String,
    val type: AccountType
)

private data class ChemicalOffer(
    val id: Int,
    val name: String,
    val officialPrice: String,
    val marketPrice: String,
    val deliveryPlace: String,
    val deliveryTime: String,
    val supplier: String,
    val phone: String,
    var status: OfferStatus,
    val ownerPhone: String = "",
    val isChemTrade: Boolean = false,
    val marginPercent: Int = 0,
    var rejectionReason: String = ""
)

private val ChemOlive = Color(0xFF566B3F)
private val ChemOliveDark = Color(0xFF34452A)
private val ChemCream = Color(0xFFF4EBDD)
private val ChemCreamLight = Color(0xFFFFFBF5)
private val ChemGold = Color(0xFFC7A55A)
private val ChemGreen = Color(0xFF567A4A)
private val ChemRed = Color(0xFFB3261E)
private val ChemInk = Color(0xFF263126)
private const val ADMIN_HASH = "fda58e44e118473f2e49b6a49f3c83b1ba4a4627dc4abb2b74e90e6a844df374"
private const val DEFAULT_PHONE = "02100000000"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChemTradeApp() }
    }
}

@Composable
private fun ChemTradeApp() {
    val colors = lightColorScheme(
        primary = ChemOlive,
        onPrimary = Color.White,
        secondary = ChemGold,
        onSecondary = ChemInk,
        tertiary = ChemGreen,
        background = ChemCreamLight,
        surface = ChemCreamLight,
        surfaceVariant = ChemCream,
        onSurface = ChemInk,
        primaryContainer = Color(0xFFDDE6D3),
        onPrimaryContainer = ChemOliveDark
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = colors) { ChemTradeHome() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChemTradeHome() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chemtrade", Context.MODE_PRIVATE) }
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var showAdminLogin by remember { mutableStateOf(false) }
    var showOfferForm by remember { mutableStateOf(false) }
    var showRegistration by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var selectedOffer by remember { mutableStateOf<ChemicalOffer?>(null) }
    var supportPhone by remember { mutableStateOf(prefs.getString("supportPhone", DEFAULT_PHONE) ?: DEFAULT_PHONE) }
    var defaultMargin by remember { mutableIntStateOf(prefs.getInt("defaultMargin", 3)) }
    var currentUser by remember { mutableStateOf(loadUser(prefs)) }

    val offers = remember {
        mutableStateListOf(
            ChemicalOffer(1, "مونو اتانول آمین (MEA)", "۱۲۵,۰۰۰ تومان", "۱۲۸,۵۰۰ تومان", "تهران، شورآباد / حواله از درب پتروشیمی", "هماهنگی تلفنی", "بازرگانی دهقان‌زاده", DEFAULT_PHONE, OfferStatus.Approved, isChemTrade = true, marginPercent = 3),
            ChemicalOffer(2, "اسید استیک", "۷۷,۰۰۰ تومان", "۷۹,۰۰۰ تومان", "انبار تهران", "۱ تا ۲ روز کاری", "تأمین‌کننده تأییدشده", DEFAULT_PHONE, OfferStatus.Approved),
            ChemicalOffer(3, "متانول", "۴۲,۰۰۰ تومان", "۴۳,۵۰۰ تومان", "عسلویه / تحویل توافقی", "تحویل فوری", "تأمین‌کننده تأییدشده", DEFAULT_PHONE, OfferStatus.Approved),
            ChemicalOffer(4, "اوره صنعتی", "۲۵,۰۰۰ تومان", "۲۶,۰۰۰ تومان", "انبار فروشنده", "پس از تأیید مدیر", "تأمین‌کننده جدید", DEFAULT_PHONE, OfferStatus.Pending, ownerPhone = "09000000000")
        )
    }

    val myOffers = offers.filter { currentUser?.phone?.isNotBlank() == true && it.ownerPhone == currentUser!!.phone }

    Scaffold(
        containerColor = ChemCreamLight,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ChemCreamLight),
                title = {
                    Column {
                        Text("ChemTrade", fontWeight = FontWeight.ExtraBold, color = ChemOliveDark)
                        Text("بازار تخصصی مواد اولیه", style = MaterialTheme.typography.labelSmall, color = ChemOlive)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { tab = 0 }) {
                        Icon(Icons.Default.Storefront, contentDescription = "بازار", tint = ChemOlive)
                    }
                },
                actions = {
                    IconButton(onClick = { tab = 3 }) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات", tint = ChemOliveDark)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = ChemCream) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Store, null) }, label = { Text("بازار") })
                NavigationBarItem(selected = tab == 1, onClick = { if (currentUser == null) showRegistration = true else showOfferForm = true }, icon = { Icon(Icons.Default.AddCircle, null) }, label = { Text("ثبت آگهی") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("حساب من") })
            }
        }
    ) { padding ->
        when (tab) {
            0 -> MarketScreen(Modifier.padding(padding), offers, query, currentUser?.phone ?: "", { query = it }) { selectedOffer = it }
            1 -> AddOfferScreen(Modifier.padding(padding), currentUser != null) { if (currentUser == null) showRegistration = true else showOfferForm = true }
            2 -> AccountScreen(Modifier.padding(padding), currentUser, myOffers, supportPhone, onRegister = { showRegistration = true }, onLogin = { showLogin = true }, onSelect = { selectedOffer = it })
            3 -> SettingsScreen(Modifier.padding(padding), currentUser, isAdmin, supportPhone, onAdminLogin = { showAdminLogin = true }, onLogoutAdmin = { isAdmin = false }, onLogoutUser = { currentUser = null; prefs.edit().clear().apply() }, onRegister = { showRegistration = true }, onLogin = { showLogin = true }, onCall = { dial(context, supportPhone) })
            4 -> if (isAdmin) AdminScreen(Modifier.padding(padding), offers, supportPhone, defaultMargin, onPhone = { supportPhone = it; prefs.edit().putString("supportPhone", it).apply() }, onMargin = { defaultMargin = it; prefs.edit().putInt("defaultMargin", it).apply() }, onAdd = { showOfferForm = true }, onBack = { tab = 0 }) else tab = 3
        }
    }

    selectedOffer?.let { offer -> OfferDialog(offer, supportPhone, onDismiss = { selectedOffer = null }, onResubmit = { selectedOffer = null; showOfferForm = true }) }

    if (showAdminLogin) AdminLoginDialog(onDismiss = { showAdminLogin = false }, onSuccess = { isAdmin = true; showAdminLogin = false; tab = 4 })
    if (showRegistration) RegistrationDialog(onDismiss = { showRegistration = false }, onSuccess = { user -> currentUser = user; saveUser(prefs, user); showRegistration = false; tab = 2 })
    if (showLogin) LoginDialog(onDismiss = { showLogin = false }, onSuccess = { user -> currentUser = user; saveUser(prefs, user); showLogin = false; tab = 2 })
    if (showOfferForm) OfferFormDialog(adminMode = isAdmin, ownerPhone = currentUser?.phone ?: "", supportPhone = supportPhone, defaultMargin = defaultMargin, onDismiss = { showOfferForm = false }) { offer ->
        val newId = (offers.maxOfOrNull { it.id } ?: 0) + 1
        offers.add(offer.copy(id = newId))
        showOfferForm = false
        tab = if (isAdmin) 4 else 2
    }
}

private fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

private fun saveUser(prefs: android.content.SharedPreferences, user: UserProfile) {
    prefs.edit().putString("phone", user.phone).putString("name", user.fullName).putString("email", user.email).putString("address", user.address).putString("company", user.company).putString("type", user.type.name).apply()
}

private fun loadUser(prefs: android.content.SharedPreferences): UserProfile? {
    val phone = prefs.getString("phone", null) ?: return null
    return UserProfile(phone, prefs.getString("name", "") ?: "", prefs.getString("email", "") ?: "", prefs.getString("address", "") ?: "", prefs.getString("company", "") ?: "", runCatching { AccountType.valueOf(prefs.getString("type", AccountType.Supplier.name)!!) }.getOrDefault(AccountType.Supplier))
}

@Composable
private fun MarketScreen(modifier: Modifier, offers: List<ChemicalOffer>, query: String, userPhone: String, onQuery: (String) -> Unit, onSelect: (ChemicalOffer) -> Unit) {
    val visible = offers.filter { it.status == OfferStatus.Approved && (it.name.contains(query, true) || it.supplier.contains(query, true)) }
    val ordered = visible.sortedWith(compareByDescending<ChemicalOffer> { userPhone.isNotBlank() && it.ownerPhone == userPhone }.thenByDescending { it.isChemTrade }.thenByDescending { it.id })
    LazyColumn(modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = ChemOliveDark)) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, null, tint = ChemGold, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(10.dp))
                        Column { Text("ChemTrade", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall); Text("بازار حرفه‌ای مواد اولیه", color = ChemCream) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("قیمت رسمی و بازار، اطلاعات تحویل و تماس مستقیم با فروشنده", color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("جستجوی ماده یا فروشنده") })
        }
        if (userPhone.isNotBlank() && ordered.any { it.ownerPhone == userPhone }) {
            item { SectionTitle("آگهی‌های من") }
            items(ordered.filter { it.ownerPhone == userPhone }, key = { it.id }) { OfferCard(it, onSelect, mine = true) }
        }
        if (ordered.any { it.isChemTrade && it.ownerPhone != userPhone }) {
            item { SectionTitle("آگهی‌های مستقیم ChemTrade") }
            items(ordered.filter { it.isChemTrade && it.ownerPhone != userPhone }, key = { it.id }) { OfferCard(it, onSelect, mine = false) }
        }
        item { SectionTitle("سایر آگهی‌های تأییدشده") }
        items(ordered.filter { it.ownerPhone != userPhone && !it.isChemTrade }, key = { it.id }) { OfferCard(it, onSelect, mine = false) }
        if (ordered.isEmpty()) item { Text("آگهی تأییدشده‌ای پیدا نشد.") }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = ChemOliveDark) }

@Composable
private fun OfferCard(offer: ChemicalOffer, onClick: (ChemicalOffer) -> Unit, mine: Boolean) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (mine) Color(0xFFE7EFD9) else Color.White), onClick = { onClick(offer) }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) { Text(offer.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium); Text(offer.supplier, style = MaterialTheme.typography.bodySmall) }
                AssistChip(onClick = { onClick(offer) }, label = { Text(if (mine) "آگهی من" else if (offer.isChemTrade) "ChemTrade" else "تأییدشده") })
            }
            if (offer.officialPrice.isNotBlank()) Text("قیمت رسمی: ${offer.officialPrice}", color = ChemGreen, fontWeight = FontWeight.Bold)
            if (offer.marketPrice.isNotBlank()) Text("قیمت بازار: ${offer.marketPrice}", color = ChemOliveDark, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            InfoLine(Icons.Default.LocationOn, "مکان تحویل: ${offer.deliveryPlace}")
            InfoLine(Icons.Default.Schedule, "زمان تحویل: ${offer.deliveryTime}")
            Button(onClick = { onClick(offer) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Phone, null); Spacer(Modifier.width(6.dp)); Text("جزئیات و تماس مستقیم") }
        }
    }
}

@Composable private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = ChemGold, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(text, style = MaterialTheme.typography.bodySmall) } }

@Composable
private fun AddOfferScreen(modifier: Modifier, loggedIn: Boolean, onAdd: () -> Unit) {
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Default.Inventory2, null, tint = ChemOlive, modifier = Modifier.size(52.dp))
        Text("ثبت آگهی فروش", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(if (loggedIn) "آگهی شما ابتدا برای بررسی مدیر ارسال می‌شود. در صورت رد شدن، دلیل رد را می‌بینید و می‌توانید اصلاح و دوباره ارسال کنید." else "برای ثبت آگهی ابتدا به عنوان تأمین‌کننده ثبت‌نام کنید.")
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text(if (loggedIn) "ثبت آگهی جدید" else "ثبت‌نام تأمین‌کننده") }
    }
}

@Composable
private fun AccountScreen(modifier: Modifier, user: UserProfile?, myOffers: List<ChemicalOffer>, phone: String, onRegister: () -> Unit, onLogin: () -> Unit, onSelect: (ChemicalOffer) -> Unit) {
    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("حساب کاربری", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            if (user == null) {
                Text("برای ثبت آگهی، درخواست همکاری و مدیریت اطلاعات خود وارد حساب شوید.")
                Button(onClick = onRegister, modifier = Modifier.fillMaxWidth()) { Text("ثبت‌نام تأمین‌کننده / مصرف‌کننده") }
                OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("ورود به حساب") }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = ChemCream), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(user.fullName.ifBlank { "کاربر ChemTrade" }, fontWeight = FontWeight.ExtraBold)
                        Text("${user.type.title} • ${user.company}")
                        Text(user.phone)
                        Text(user.email)
                        Text(user.address)
                    }
                }
                Spacer(Modifier.height(8.dp))
                SectionTitle("آگهی‌های من")
            }
        }
        if (user != null) {
            items(myOffers, key = { it.id }) { offer ->
                Card(onClick = { onSelect(offer) }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(offer.name, fontWeight = FontWeight.Bold); Text("وضعیت: ${offer.status.title}"); if (offer.status == OfferStatus.Rejected) Text("دلیل رد: ${offer.rejectionReason}", color = ChemRed) } }
            }
            item { CallButton(phone, "تماس با ChemTrade") }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier, user: UserProfile?, isAdmin: Boolean, phone: String, onAdminLogin: () -> Unit, onLogoutAdmin: () -> Unit, onLogoutUser: () -> Unit, onRegister: () -> Unit, onLogin: () -> Unit, onCall: () -> Unit) {
    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("تنظیمات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) }
        item { Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Text("تماس با ما", fontWeight = FontWeight.Bold); Text("برای استعلام، همکاری یا پشتیبانی مستقیم با ChemTrade در ارتباط باشید."); Button(onClick = onCall, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Phone, null); Spacer(Modifier.width(6.dp)); Text("تماس با ما") } } } }
        item { Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("حساب کاربری", fontWeight = FontWeight.Bold); if (user == null) { Button(onClick = onRegister, modifier = Modifier.fillMaxWidth()) { Text("ثبت‌نام") }; OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("ورود") } } else { Text("وارد شده با ${user.phone}"); TextButton(onClick = onLogoutUser) { Text("خروج از حساب") } } } } }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = ChemCream)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("نسخه 1.0.0", style = MaterialTheme.typography.labelSmall, color = ChemOlive)
                    Text("دسترسی مدیر", fontWeight = FontWeight.Bold)
                    if (isAdmin) { Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("پنل مدیریت فعال است") }; TextButton(onClick = onLogoutAdmin) { Text("خروج مدیر") } }
                    else TextButton(onClick = onAdminLogin) { Text("ورود مدیر") }
                }
            }
        }
    }
}

@Composable
private fun AdminScreen(modifier: Modifier, offers: MutableList<ChemicalOffer>, phone: String, margin: Int, onPhone: (String) -> Unit, onMargin: (Int) -> Unit, onAdd: () -> Unit, onBack: () -> Unit) {
    var phoneEdit by remember(phone) { mutableStateOf(phone) }
    var marginEdit by remember(margin) { mutableStateOf(margin.toString()) }
    var rejectTarget by remember { mutableStateOf<ChemicalOffer?>(null) }
    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("داشبورد مدیریت", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold); Text("کنترل آگهی، سود و انتشار", style = MaterialTheme.typography.bodySmall) }; IconButton(onClick = onBack) { Icon(Icons.Default.Close, null) } } }
        item { Card(colors = CardDefaults.cardColors(containerColor = ChemCream), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("تنظیمات مدیریت", fontWeight = FontWeight.Bold); OutlinedTextField(phoneEdit, { phoneEdit = it }, label = { Text("شماره تماس ChemTrade") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(marginEdit, { marginEdit = it.filter(Char::isDigit) }, label = { Text("سود پیش‌فرض درصدی") }, modifier = Modifier.fillMaxWidth()); Button(onClick = { onPhone(phoneEdit); onMargin(marginEdit.toIntOrNull() ?: 0) }, modifier = Modifier.fillMaxWidth()) { Text("ذخیره") }; OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Verified, null); Spacer(Modifier.width(6.dp)); Text("ثبت آگهی مستقیم ChemTrade") } } } }
        item { SectionTitle("درخواست‌های در انتظار تأیید") }
        items(offers.filter { it.status == OfferStatus.Pending }, key = { it.id }) { offer -> PendingOfferCard(offer, onApprove = { offer.status = OfferStatus.Approved }, onReject = { rejectTarget = offer }, onDelete = { offers.remove(offer) }) }
        item { SectionTitle("همه آگهی‌ها") }
        items(offers, key = { it.id }) { offer -> AdminOfferCard(offer) { offers.remove(offer) } }
    }
    rejectTarget?.let { target -> RejectionDialog(onDismiss = { rejectTarget = null }) { reason -> target.status = OfferStatus.Rejected; target.rejectionReason = reason; rejectTarget = null } }
}

@Composable private fun PendingOfferCard(offer: ChemicalOffer, onApprove: () -> Unit, onReject: () -> Unit, onDelete: () -> Unit) { Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(offer.name, fontWeight = FontWeight.Bold); Text("فروشنده: ${offer.supplier}"); Text("مکان: ${offer.deliveryPlace}"); Text("زمان: ${offer.deliveryTime}"); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { Button(onClick = onApprove) { Text("تأیید") }; OutlinedButton(onClick = onReject) { Text("رد + پیام") }; TextButton(onClick = onDelete) { Text("حذف", color = ChemRed) } } } } }
@Composable private fun AdminOfferCard(offer: ChemicalOffer, onDelete: () -> Unit) { Card { Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(offer.name, fontWeight = FontWeight.Bold); Text("${offer.status.title} • سود ${offer.marginPercent}%", style = MaterialTheme.typography.bodySmall); if (offer.rejectionReason.isNotBlank()) Text("پیام رد: ${offer.rejectionReason}", color = ChemRed, style = MaterialTheme.typography.bodySmall) }; IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف", tint = ChemRed) } } } }

@Composable
private fun OfferDialog(offer: ChemicalOffer, fallbackPhone: String, onDismiss: () -> Unit, onResubmit: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(offer.name) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("فروشنده: ${offer.supplier}"); if (offer.officialPrice.isNotBlank()) Text("قیمت رسمی: ${offer.officialPrice}"); if (offer.marketPrice.isNotBlank()) Text("قیمت بازار: ${offer.marketPrice}"); Text("مکان تحویل: ${offer.deliveryPlace}"); Text("زمان تحویل: ${offer.deliveryTime}"); if (offer.status == OfferStatus.Rejected) Text("دلیل رد: ${offer.rejectionReason}", color = ChemRed, fontWeight = FontWeight.Bold); CallButton(if (offer.phone.isBlank()) fallbackPhone else offer.phone, "تماس مستقیم") } }, confirmButton = { if (offer.status == OfferStatus.Rejected) Button(onClick = onResubmit) { Text("اصلاح و ارسال مجدد") } else TextButton(onClick = onDismiss) { Text("بستن") } }, dismissButton = { if (offer.status == OfferStatus.Rejected) TextButton(onClick = onDismiss) { Text("بستن") } })
}

@Composable private fun CallButton(phone: String, title: String) { val context = LocalContext.current; Button(onClick = { dial(context, phone) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Phone, null); Spacer(Modifier.width(8.dp)); Text(title) } }
private fun dial(context: Context, phone: String) { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }

@Composable
private fun AdminLoginDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var password by remember { mutableStateOf("") }; var error by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ورود مدیر") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("دسترسی مدیریت محافظت شده است."); OutlinedTextField(password, { password = it; error = false }, singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("رمز مدیریت") }, isError = error); if (error) Text("رمز صحیح نیست.", color = ChemRed) } }, confirmButton = { Button(onClick = { if (hash(password) == ADMIN_HASH) onSuccess() else error = true }) { Text("ورود") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun RegistrationDialog(onDismiss: () -> Unit, onSuccess: (UserProfile) -> Unit) {
    var type by remember { mutableStateOf(AccountType.Supplier) }; var phone by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var address by remember { mutableStateOf("") }; var company by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ثبت‌نام در ChemTrade") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 430.dp)) { item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = type == AccountType.Supplier, onClick = { type = AccountType.Supplier }, label = { Text("تأمین‌کننده") }); FilterChip(selected = type == AccountType.Consumer, onClick = { type = AccountType.Consumer }, label = { Text("مصرف‌کننده") }) } }; item { OutlinedTextField(phone, { phone = it }, label = { Text("شماره موبایل") }, modifier = Modifier.fillMaxWidth()) }; item { OutlinedTextField(name, { name = it }, label = { Text("نام و نام خانوادگی") }, modifier = Modifier.fillMaxWidth()) }; item { OutlinedTextField(company, { company = it }, label = { Text("نام شرکت") }, modifier = Modifier.fillMaxWidth()) }; item { OutlinedTextField(email, { email = it }, label = { Text("ایمیل") }, modifier = Modifier.fillMaxWidth()) }; item { OutlinedTextField(address, { address = it }, label = { Text("آدرس کامل") }, modifier = Modifier.fillMaxWidth()) } } }, confirmButton = { Button(onClick = { if (phone.length >= 10 && name.isNotBlank() && email.isNotBlank() && address.isNotBlank()) onSuccess(UserProfile(phone, name, email, address, company, type)) }) { Text("ثبت حساب") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun LoginDialog(onDismiss: () -> Unit, onSuccess: (UserProfile) -> Unit) {
    var phone by remember { mutableStateOf("") }; var error by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ورود به حساب") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("شماره موبایل ثبت‌شده را وارد کنید."); OutlinedTextField(phone, { phone = it; error = false }, label = { Text("شماره موبایل") }, isError = error); if (error) Text("حسابی با این شماره روی این دستگاه پیدا نشد.", color = ChemRed) } }, confirmButton = { Button(onClick = { if (phone.isNotBlank()) onSuccess(UserProfile(phone, "کاربر ChemTrade", "", "", "", AccountType.Supplier)) else error = true }) { Text("ورود") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun RejectionDialog(onDismiss: () -> Unit, onReject: (String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("دلیل رد آگهی") }, text = { OutlinedTextField(reason, { reason = it }, label = { Text("پیام برای تأمین‌کننده") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }, confirmButton = { Button(onClick = { if (reason.isNotBlank()) onReject(reason) }) { Text("رد و ارسال پیام") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun OfferFormDialog(adminMode: Boolean, ownerPhone: String, supportPhone: String, defaultMargin: Int, onDismiss: () -> Unit, onSubmit: (ChemicalOffer) -> Unit) {
    var name by remember { mutableStateOf("") }; var official by remember { mutableStateOf("") }; var market by remember { mutableStateOf("") }; var place by remember { mutableStateOf("") }; var time by remember { mutableStateOf("") }; var supplier by remember { mutableStateOf(if (adminMode) "ChemTrade" else "") }; var phone by remember { mutableStateOf(if (adminMode) supportPhone else ownerPhone) }; var margin by remember { mutableStateOf(defaultMargin.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (adminMode) "ثبت آگهی مستقیم ChemTrade" else "ثبت آگهی جدید") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 430.dp)) { item { OutlinedTextField(name, { name = it }, label = { Text("نام ماده") }, modifier = Modifier.fillMaxWidth()) }; item { OutlinedTextField(official, { official = it }, label = { Text("قیمت رسمی") }, modifier = Modifier.fillMaxWidth()) }; item { OutlinedTextField(market, { market = it }, label = { Text("قیمت غیررسمی / بازار") }, modifier = Modifier.fillMaxWidth()) }; item { OutlinedTextField(place, { place = it }, label = { Text("مکان تحویل") }, modifier = Modifier.fillMaxWidth()) }; item { OutlinedTextField(time, { time = it }, label = { Text("زمان تحویل") }, modifier = Modifier.fillMaxWidth()) }; item { OutlinedTextField(supplier, { supplier = it }, label = { Text("نام فروشنده") }, modifier = Modifier.fillMaxWidth()) }; item { OutlinedTextField(phone, { phone = it }, label = { Text("شماره تماس") }, modifier = Modifier.fillMaxWidth()) }; if (adminMode) item { OutlinedTextField(margin, { margin = it.filter(Char::isDigit) }, label = { Text("سود این آگهی درصدی") }, modifier = Modifier.fillMaxWidth()) } } }, confirmButton = { Button(onClick = { if (name.isNotBlank() && place.isNotBlank() && time.isNotBlank()) onSubmit(ChemicalOffer(0, name, official, market, place, time, supplier.ifBlank { "فروشنده" }, phone.ifBlank { supportPhone }, if (adminMode) OfferStatus.Approved else OfferStatus.Pending, ownerPhone = if (adminMode) "" else ownerPhone, isChemTrade = adminMode, marginPercent = margin.toIntOrNull() ?: 0)) }) { Text("ثبت") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}
