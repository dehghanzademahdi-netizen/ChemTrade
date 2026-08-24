package com.dehghanzadeh.chemtrade

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private enum class OfferStatus(val title: String) {
    Pending("در انتظار بررسی"),
    Approved("تأیید شده"),
    Rejected("رد شده")
}

private enum class PriceType(val title: String) {
    Official("رسمی"),
    Market("غیررسمی / بازار آزاد"),
    Both("هر دو قیمت")
}

private data class ChemicalOffer(
    val id: Int,
    val name: String,
    val category: String,
    val officialPrice: String?,
    val marketPrice: String?,
    val unit: String,
    val supplier: String,
    val stock: String,
    val deliveryPlace: String,
    val deliveryTime: String,
    val phone: String,
    val status: OfferStatus,
    val isChemTrade: Boolean = false,
    val marginPercent: Int = 0,
    val badge: String = ""
)

private val ChemBlue = Color(0xFF123B5D)
private val ChemBlueLight = Color(0xFFEAF3F8)
private val ChemGold = Color(0xFFD89A1D)
private val ChemGreen = Color(0xFF1F7A5A)
private val ChemRed = Color(0xFFB3261E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChemTradeApp() }
    }
}

@Composable
private fun ChemTradeApp() {
    val scheme = lightColorScheme(
        primary = ChemBlue,
        secondary = ChemGold,
        tertiary = ChemGreen,
        primaryContainer = ChemBlueLight,
        surface = Color(0xFFFCFCFC)
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme) { ChemTradeHome() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChemTradeHome() {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedOffer by remember { mutableStateOf<ChemicalOffer?>(null) }
    var showOfferForm by remember { mutableStateOf(false) }
    var showAdminLogin by remember { mutableStateOf(false) }
    var adminLoggedIn by remember { mutableStateOf(false) }
    var adminPin by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }
    var requestSent by remember { mutableStateOf(false) }
    var supportPhone by remember { mutableStateOf("02100000000") }
    var defaultMargin by remember { mutableIntStateOf(3) }

    var offers by remember {
        mutableStateOf(
            listOf(
                ChemicalOffer(1, "مونو اتانول آمین (MEA)", "آمین‌ها و افزودنی‌ها", "۱۲۵,۰۰۰ تومان", "۱۲۸,۵۰۰ تومان", "کیلوگرم", "بازرگانی دهقان‌زاده", "بشکه ۲۰۰ کیلویی", "تهران، شورآباد یا حواله از درب پتروشیمی", "هماهنگی تلفنی", "02100000000", OfferStatus.Approved, true, 3, "ویژه ChemTrade"),
                ChemicalOffer(2, "اسید استیک", "اسیدها", "۷۷,۰۰۰ تومان", "۷۹,۰۰۰ تومان", "کیلوگرم", "تأمین‌کننده تأییدشده", "موجود", "انبار تهران", "۱ تا ۲ روز کاری", "02100000000", OfferStatus.Approved, false, 2, "تأییدشده"),
                ChemicalOffer(3, "متانول", "حلال‌ها", "۴۲,۰۰۰ تومان", "۴۳,۵۰۰ تومان", "کیلوگرم", "تأمین‌کننده تأییدشده", "موجود", "عسلویه / تحویل توافقی", "تحویل فوری", "02100000000", OfferStatus.Approved, false, 2, "فوری"),
                ChemicalOffer(4, "اوره صنعتی", "مواد اولیه کود", "۲۵,۰۰۰ تومان", "۲۶,۰۰۰ تومان", "کیلوگرم", "تأمین‌کننده جدید", "حداقل سفارش ۱ تن", "انبار فروشنده", "پس از تأیید ادمین", "02100000000", OfferStatus.Pending, false, 0, "در انتظار")
            )
        )
    }

    val visibleOffers = offers.filter {
        it.status == OfferStatus.Approved &&
            (it.name.contains(query, true) || it.category.contains(query, true) || it.supplier.contains(query, true))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ChemTrade", fontWeight = FontWeight.Bold, color = ChemBlue)
                        Text("بازار حرفه‌ای مواد اولیه", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { selectedTab = 3 }) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("بازار") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Inventory2, null) }, label = { Text("ثبت آگهی") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("حساب من") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("تنظیمات") })
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> MarketScreen(Modifier.padding(padding), query, { query = it }, visibleOffers, { selectedOffer = it })
            1 -> SupplyScreen(Modifier.padding(padding), { showOfferForm = true })
            2 -> AccountScreen(Modifier.padding(padding), requestSent, { requestSent = true }, supportPhone)
            else -> SettingsScreen(
                Modifier.padding(padding),
                adminLoggedIn,
                { showAdminLogin = true },
                { adminLoggedIn = false },
                { selectedTab = 4 }
            )
        }

        if (selectedTab == 4 && adminLoggedIn) {
            AdminScreen(
                offers = offers,
                defaultMargin = defaultMargin,
                supportPhone = supportPhone,
                onUpdateOffer = { updated -> offers = offers.map { if (it.id == updated.id) updated else it } },
                onDelete = { id -> offers = offers.filterNot { it.id == id } },
                onDefaultMarginChange = { defaultMargin = it },
                onPhoneChange = { supportPhone = it },
                onAddOwnOffer = { showOfferForm = true }
            )
        }
    }

    selectedOffer?.let { offer ->
        OfferDetailsDialog(
            offer = offer,
            supportPhone = supportPhone,
            onDismiss = { selectedOffer = null },
            onRequest = { requestSent = true; selectedOffer = null }
        )
    }

    if (showOfferForm) {
        OfferFormDialog(
            adminMode = adminLoggedIn && selectedTab == 4,
            onDismiss = { showOfferForm = false },
            onSubmit = { newOffer ->
                offers = offers + newOffer.copy(
                    id = (offers.maxOfOrNull { it.id } ?: 0) + 1,
                    status = if (adminLoggedIn && selectedTab == 4) OfferStatus.Approved else OfferStatus.Pending,
                    isChemTrade = adminLoggedIn && selectedTab == 4,
                    phone = if (adminLoggedIn && selectedTab == 4) supportPhone else newOffer.phone,
                    marginPercent = if (adminLoggedIn && selectedTab == 4) defaultMargin else 0,
                    badge = if (adminLoggedIn && selectedTab == 4) "ویژه ChemTrade" else "در انتظار بررسی"
                )
                showOfferForm = false
            }
        )
    }

    if (showAdminLogin) {
        AlertDialog(
            onDismissRequest = { showAdminLogin = false },
            icon = { Icon(Icons.Default.AdminPanelSettings, null, tint = ChemBlue) },
            title = { Text("ورود به پنل مدیریت") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("این ورود فعلاً برای نسخه آزمایشی است. رمز پیش‌فرض: 1234")
                    OutlinedTextField(
                        value = adminPin,
                        onValueChange = { adminPin = it; loginError = false },
                        label = { Text("رمز مدیریت") },
                        singleLine = true,
                        isError = loginError
                    )
                    if (loginError) Text("رمز صحیح نیست.", color = ChemRed)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (adminPin == "1234") {
                        adminLoggedIn = true
                        adminPin = ""
                        showAdminLogin = false
                        selectedTab = 4
                    } else loginError = true
                }) { Text("ورود") }
            },
            dismissButton = { TextButton(onClick = { showAdminLogin = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun MarketScreen(
    modifier: Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    offers: List<ChemicalOffer>,
    onSelect: (ChemicalOffer) -> Unit
) {
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ChemBlue)) {
                Column(Modifier.padding(18.dp)) {
                    Text("بازار تخصصی مواد اولیه", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("قیمت رسمی و قیمت بازار، زمان و مکان تحویل و ارتباط مستقیم تلفنی", color = Color.White.copy(alpha = .9f))
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("جستجوی ماده، دسته یا فروشنده") })
            Spacer(Modifier.height(12.dp))
            Text("⭐ آگهی‌های مستقیم ChemTrade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(offers.filter { it.isChemTrade }) { OfferCard(it, onClick = { onSelect(it) }) }
        item { Text("جدیدترین آگهی‌های تأییدشده", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(offers.filterNot { it.isChemTrade }) { OfferCard(it, onClick = { onSelect(it) }) }
        if (offers.isEmpty()) item { Text("آگهی تأییدشده‌ای پیدا نشد.") }
    }
}

@Composable
private fun OfferCard(offer: ChemicalOffer, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(offer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(offer.category, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(onClick = onClick, label = { Text(offer.badge.ifBlank { offer.status.title }) }, leadingIcon = { Icon(if (offer.isChemTrade) Icons.Default.Verified else Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp)) })
            }
            PriceBlock(offer)
            InfoLine(Icons.Default.LocationOn, "تحویل: ${offer.deliveryPlace}")
            InfoLine(Icons.Default.Schedule, "زمان تحویل: ${offer.deliveryTime}")
            Text("${offer.stock} • ${offer.supplier}", style = MaterialTheme.typography.bodySmall)
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("مشاهده جزئیات و تماس") }
        }
    }
}

@Composable
private fun PriceBlock(offer: ChemicalOffer) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        offer.officialPrice?.let { Text("قیمت رسمی: $it / ${offer.unit}", color = ChemGreen, fontWeight = FontWeight.Bold) }
        offer.marketPrice?.let { Text("قیمت بازار: $it / ${offer.unit}", color = ChemBlue, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(17.dp), tint = ChemGold)
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SupplyScreen(modifier: Modifier, onAddOffer: () -> Unit) {
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("ثبت آگهی تأمین", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("هر آگهی ابتدا در وضعیت «در انتظار بررسی» قرار می‌گیرد و بدون تأیید مدیر منتشر نمی‌شود.")
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ChemBlueLight)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("اطلاعات قابل ثبت", fontWeight = FontWeight.Bold)
                    Text("• نام ماده و دسته‌بندی\n• قیمت رسمی، غیررسمی یا هر دو\n• مقدار و موجودی\n• مکان تحویل\n• زمان تحویل\n• شماره تماس")
                }
            }
        }
        item {
            Button(onClick = onAddOffer, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("ثبت آگهی برای بررسی")
            }
        }
    }
}

@Composable
private fun AccountScreen(modifier: Modifier, requestSent: Boolean, onRequest: () -> Unit, supportPhone: String) {
    val context = LocalContext.current
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("حساب من", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("نسخه آزمایشی ChemTrade")
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ارتباط سریع با ChemTrade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("برای قیمت لحظه‌ای، موجودی و شرایط معامله با تیم ما تماس بگیرید.")
                    Button(onClick = { dial(context, supportPhone) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Phone, null); Spacer(Modifier.width(8.dp)); Text("تماس تلفنی")
                    }
                }
            }
        }
        item {
            if (requestSent) Text("درخواست شما در نسخه آزمایشی ثبت شد.")
            else OutlinedButton(onClick = onRequest, modifier = Modifier.fillMaxWidth()) { Text("ثبت درخواست آزمایشی") }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    adminLoggedIn: Boolean,
    onAdminLogin: () -> Unit,
    onAdminLogout: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("تنظیمات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = ChemBlue)
                    Text("پنل مدیریت", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(if (adminLoggedIn) "شما با دسترسی مدیر وارد شده‌اید." else "برای تأیید آگهی‌ها، تعیین سود و مدیریت بازار وارد شوید.")
                    if (adminLoggedIn) {
                        Button(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth()) { Text("ورود به داشبورد مدیریت") }
                        TextButton(onClick = onAdminLogout, modifier = Modifier.fillMaxWidth()) { Text("خروج از مدیریت") }
                    } else Button(onClick = onAdminLogin, modifier = Modifier.fillMaxWidth()) { Text("ورود مدیر") }
                }
            }
        }
    }
}

@Composable
private fun AdminScreen(
    offers: List<ChemicalOffer>,
    defaultMargin: Int,
    supportPhone: String,
    onUpdateOffer: (ChemicalOffer) -> Unit,
    onDelete: (Int) -> Unit,
    onDefaultMarginChange: (Int) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddOwnOffer: () -> Unit
) {
    var phoneDraft by remember(supportPhone) { mutableStateOf(supportPhone) }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ChemBlue)) {
                Column(Modifier.padding(18.dp)) {
                    Text("پنل مدیریت ChemTrade", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("کنترل کامل انتشار آگهی، قیمت و سود", color = Color.White.copy(alpha = .9f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("در انتظار", offers.count { it.status == OfferStatus.Pending }.toString(), Modifier.weight(1f))
                MetricCard("فعال", offers.count { it.status == OfferStatus.Approved }.toString(), Modifier.weight(1f))
                MetricCard("آگهی خودم", offers.count { it.isChemTrade }.toString(), Modifier.weight(1f))
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("تنظیمات کنترل بازار", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("سود پیش‌فرض ChemTrade: $defaultMargin٪")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onDefaultMarginChange((defaultMargin - 1).coerceAtLeast(0)) }) { Text("−") }
                        OutlinedButton(onClick = { onDefaultMarginChange((defaultMargin + 1).coerceAtMost(30)) }) { Text("+") }
                    }
                    OutlinedTextField(value = phoneDraft, onValueChange = { phoneDraft = it }, label = { Text("شماره تماس ChemTrade") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { onPhoneChange(phoneDraft) }) { Text("ذخیره شماره تماس") }
                }
            }
        }
        item { Text("آگهی‌های در انتظار بررسی", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(offers.filter { it.status == OfferStatus.Pending }) { offer ->
            AdminOfferCard(offer, onApprove = { onUpdateOffer(offer.copy(status = OfferStatus.Approved, marginPercent = defaultMargin, badge = "تأییدشده")) }, onReject = { onUpdateOffer(offer.copy(status = OfferStatus.Rejected, badge = "رد شده")) }, onDelete = { onDelete(offer.id) })
        }
        if (offers.none { it.status == OfferStatus.Pending }) item { Text("آگهی در انتظاری وجود ندارد.") }
        item { Text("آگهی‌های فعال", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(offers.filter { it.status == OfferStatus.Approved }) { offer ->
            AdminOfferCard(offer, onApprove = { onUpdateOffer(offer.copy(marginPercent = (offer.marginPercent + 1).coerceAtMost(30))) }, onReject = { onUpdateOffer(offer.copy(status = OfferStatus.Rejected, badge = "متوقف شده")) }, onDelete = { onDelete(offer.id) })
        }
        item {
            Button(onClick = onAddOwnOffer, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("ثبت آگهی مستقیم ChemTrade") }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ChemBlue); Text(title, style = MaterialTheme.typography.labelSmall) } }
}

@Composable
private fun AdminOfferCard(offer: ChemicalOffer, onApprove: () -> Unit, onReject: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(offer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("فروشنده: ${offer.supplier}")
            PriceBlock(offer)
            Text("مکان: ${offer.deliveryPlace}")
            Text("زمان: ${offer.deliveryTime}")
            Text("سود ChemTrade: ${offer.marginPercent}٪", color = ChemGold, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text(if (offer.status == OfferStatus.Pending) "تأیید" else "+۱٪ سود") }
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("رد / توقف") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = ChemRed) }
            }
        }
    }
}

@Composable
private fun OfferDetailsDialog(offer: ChemicalOffer, supportPhone: String, onDismiss: () -> Unit, onRequest: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(offer.name) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text(offer.category) }
                item { PriceBlock(offer) }
                item { InfoLine(Icons.Default.LocationOn, "مکان تحویل: ${offer.deliveryPlace}") }
                item { InfoLine(Icons.Default.Schedule, "زمان تحویل: ${offer.deliveryTime}") }
                item { Text("موجودی: ${offer.stock}") }
                item { Text("فروشنده: ${offer.supplier}") }
                item { if (offer.isChemTrade) Text("این آگهی مستقیم ChemTrade است.", color = ChemGreen) }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { dial(context, if (offer.isChemTrade) supportPhone else offer.phone) }) { Icon(Icons.Default.Phone, null); Spacer(Modifier.width(4.dp)); Text("تماس") }
                Button(onClick = onRequest) { Text("درخواست خرید") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
private fun OfferFormDialog(adminMode: Boolean, onDismiss: () -> Unit, onSubmit: (ChemicalOffer) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("مواد شیمیایی") }
    var priceType by remember { mutableStateOf(PriceType.Both) }
    var officialPrice by remember { mutableStateOf("") }
    var marketPrice by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("کیلوگرم") }
    var supplier by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (adminMode) "ثبت آگهی مستقیم ChemTrade" else "ثبت آگهی برای بررسی") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("نام ماده") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(category, { category = it }, label = { Text("دسته‌بندی") }, modifier = Modifier.fillMaxWidth()) }
                item { Text("نوع قیمت") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PriceType.entries.forEach { type -> FilterChip(selected = priceType == type, onClick = { priceType = type }, label = { Text(type.title) }) }
                    }
                }
                if (priceType == PriceType.Official || priceType == PriceType.Both) item { OutlinedTextField(officialPrice, { officialPrice = it }, label = { Text("قیمت رسمی") }, modifier = Modifier.fillMaxWidth()) }
                if (priceType == PriceType.Market || priceType == PriceType.Both) item { OutlinedTextField(marketPrice, { marketPrice = it }, label = { Text("قیمت غیررسمی / بازار") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(unit, { unit = it }, label = { Text("واحد") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(supplier, { supplier = it }, label = { Text("نام فروشنده / شرکت") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(stock, { stock = it }, label = { Text("موجودی / حداقل سفارش") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(place, { place = it }, label = { Text("مکان تحویل") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(time, { time = it }, label = { Text("زمان تحویل") }, modifier = Modifier.fillMaxWidth()) }
                if (!adminMode) item { OutlinedTextField(phone, { phone = it }, label = { Text("شماره تماس") }, modifier = Modifier.fillMaxWidth()) }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && place.isNotBlank() && time.isNotBlank()) {
                    onSubmit(ChemicalOffer(0, name, category, officialPrice.takeIf { it.isNotBlank() }, marketPrice.takeIf { it.isNotBlank() }, unit, supplier.ifBlank { "تأمین‌کننده" }, stock.ifBlank { "توافقی" }, place, time, phone, OfferStatus.Pending))
                }
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

private fun dial(context: android.content.Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
    context.startActivity(intent)
}
