package com.dehghanzadeh.chemtrade

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private enum class OfferStatus(val title: String) {
    Pending("در انتظار بررسی"), Approved("تأیید شده"), Rejected("رد شده")
}

private data class ChemicalOffer(
    val id: Int,
    val name: String,
    val category: String,
    val officialPrice: String,
    val marketPrice: String,
    val unit: String,
    val supplier: String,
    val stock: String,
    val deliveryPlace: String,
    val deliveryTime: String,
    val phone: String,
    var status: OfferStatus,
    val isChemTrade: Boolean = false,
    val marginPercent: Int = 0
)

private val ChemBlue = Color(0xFF123B5D)
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
    val colors = lightColorScheme(
        primary = ChemBlue,
        secondary = ChemGold,
        tertiary = ChemGreen,
        primaryContainer = Color(0xFFE7F1F7),
        surface = Color(0xFFFCFCFC)
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = colors) { ChemTradeHome() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChemTradeHome() {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var admin by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ChemicalOffer?>(null) }
    var supportPhone by remember { mutableStateOf("02100000000") }
    var defaultMargin by remember { mutableIntStateOf(3) }
    val offers = remember {
        mutableStateListOf(
            ChemicalOffer(1, "مونو اتانول آمین (MEA)", "آمین‌ها و افزودنی‌ها", "۱۲۵,۰۰۰ تومان", "۱۲۸,۵۰۰ تومان", "کیلوگرم", "بازرگانی دهقان‌زاده", "بشکه ۲۰۰ کیلویی", "تهران، شورآباد یا حواله از درب پتروشیمی", "هماهنگی تلفنی", "02100000000", OfferStatus.Approved, true, 3),
            ChemicalOffer(2, "اسید استیک", "اسیدها", "۷۷,۰۰۰ تومان", "۷۹,۰۰۰ تومان", "کیلوگرم", "تأمین‌کننده تأییدشده", "موجود", "انبار تهران", "۱ تا ۲ روز کاری", "02100000000", OfferStatus.Approved),
            ChemicalOffer(3, "متانول", "حلال‌ها", "۴۲,۰۰۰ تومان", "۴۳,۵۰۰ تومان", "کیلوگرم", "تأمین‌کننده تأییدشده", "موجود", "عسلویه / تحویل توافقی", "تحویل فوری", "02100000000", OfferStatus.Approved),
            ChemicalOffer(4, "اوره صنعتی", "مواد اولیه کود", "۲۵,۰۰۰ تومان", "۲۶,۰۰۰ تومان", "کیلوگرم", "تأمین‌کننده جدید", "حداقل سفارش ۱ تن", "انبار فروشنده", "پس از تأیید ادمین", "02100000000", OfferStatus.Pending)
        )
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
                    IconButton(onClick = { tab = 3 }) { Icon(Icons.Default.Settings, "تنظیمات") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, { Icon(Icons.Default.Store, null) }, { Text("بازار") })
                NavigationBarItem(tab == 1, { tab = 1 }, { Icon(Icons.Default.Add, null) }, { Text("ثبت آگهی") })
                NavigationBarItem(tab == 2, { tab = 2 }, { Icon(Icons.Default.AccountCircle, null) }, { Text("حساب") })
                NavigationBarItem(tab == 3, { tab = 3 }, { Icon(Icons.Default.Settings, null) }, { Text("تنظیمات") })
            }
        }
    ) { padding ->
        when (tab) {
            0 -> MarketScreen(Modifier.padding(padding), offers, query, { query = it }, { selected = it })
            1 -> SupplyScreen(Modifier.padding(padding), { showForm = true })
            2 -> AccountScreen(Modifier.padding(padding), supportPhone)
            3 -> SettingsScreen(Modifier.padding(padding), admin, { showLogin = true }, { admin = false }, { tab = 4 })
            4 -> if (admin) AdminScreen(
                Modifier.padding(padding), offers, supportPhone, defaultMargin,
                { supportPhone = it }, { defaultMargin = it }, { showForm = true }, { tab = 0 }
            )
        }
    }

    selected?.let { OfferDialog(it, supportPhone, { selected = null }) }

    if (showLogin) {
        AdminLoginDialog(
            onDismiss = { showLogin = false },
            onSuccess = { admin = true; showLogin = false; tab = 4 }
        )
    }

    if (showForm) {
        OfferFormDialog(
            adminMode = admin && tab == 4,
            supportPhone = supportPhone,
            defaultMargin = defaultMargin,
            onDismiss = { showForm = false },
            onSubmit = { offer ->
                offers.add(offer.copy(id = (offers.maxOfOrNull { it.id } ?: 0) + 1))
                showForm = false
                tab = if (admin) 4 else 0
            }
        )
    }
}

@Composable
private fun MarketScreen(
    modifier: Modifier,
    offers: List<ChemicalOffer>,
    query: String,
    onQuery: (String) -> Unit,
    onSelect: (ChemicalOffer) -> Unit
) {
    val visible = offers.filter {
        it.status == OfferStatus.Approved && (it.name.contains(query, true) || it.category.contains(query, true) || it.supplier.contains(query, true))
    }
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ChemBlue)) {
                Column(Modifier.padding(18.dp)) {
                    Text("بازار تخصصی مواد اولیه", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("قیمت رسمی و غیررسمی، زمان و مکان تحویل و ارتباط مستقیم", color = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("جستجوی ماده یا فروشنده") }
            )
            Spacer(Modifier.height(10.dp))
        }
        if (visible.any { it.isChemTrade }) {
            item { Text("⭐ آگهی‌های مستقیم ChemTrade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(visible.filter { it.isChemTrade }) { OfferCard(it, { onSelect(it) }) }
        }
        item { Text("جدیدترین آگهی‌های تأییدشده", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(visible.filterNot { it.isChemTrade }) { OfferCard(it, { onSelect(it) }) }
        if (visible.isEmpty()) item { Text("آگهی تأییدشده‌ای پیدا نشد.") }
    }
}

@Composable
private fun OfferCard(offer: ChemicalOffer, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(offer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(offer.category, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(onClick = onClick, label = { Text(if (offer.isChemTrade) "ویژه ChemTrade" else "تأییدشده") })
            }
            if (offer.officialPrice.isNotBlank()) Text("قیمت رسمی: ${offer.officialPrice} / ${offer.unit}", color = ChemGreen, fontWeight = FontWeight.Bold)
            if (offer.marketPrice.isNotBlank()) Text("قیمت بازار: ${offer.marketPrice} / ${offer.unit}", color = ChemBlue, fontWeight = FontWeight.Bold)
            InfoLine(Icons.Default.LocationOn, "تحویل: ${offer.deliveryPlace}")
            InfoLine(Icons.Default.Schedule, "زمان تحویل: ${offer.deliveryTime}")
            Text("${offer.stock} • ${offer.supplier}", style = MaterialTheme.typography.bodySmall)
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("جزئیات و تماس") }
        }
    }
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = ChemGold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SupplyScreen(modifier: Modifier, onAdd: () -> Unit) {
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Default.Inventory2, null, tint = ChemBlue, modifier = Modifier.size(48.dp))
        Text("ثبت آگهی فروش", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("آگهی شما ابتدا برای بررسی مدیر ارسال می‌شود و فقط بعد از تأیید منتشر خواهد شد.")
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("ثبت آگهی جدید") }
    }
}

@Composable
private fun AccountScreen(modifier: Modifier, phone: String) {
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("حساب کاربری", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("برای ثبت و مدیریت آگهی‌ها، حساب کاربری کامل در نسخه آنلاین فعال خواهد شد.")
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("درخواست همکاری") }
        OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("تماس با ChemTrade: $phone") }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    admin: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onAdmin: () -> Unit
) {
    Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("پنل مدیریت", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (admin) {
                    Button(onClick = onAdmin, modifier = Modifier.fillMaxWidth()) { Text("ورود به داشبورد مدیریت") }
                    TextButton(onClick = onLogout) { Text("خروج از مدیریت") }
                } else {
                    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) { Text("ورود مدیر") }
                }
            }
        }
    }
}

@Composable
private fun AdminScreen(
    modifier: Modifier,
    offers: MutableList<ChemicalOffer>,
    phone: String,
    margin: Int,
    onPhone: (String) -> Unit,
    onMargin: (Int) -> Unit,
    onAdd: () -> Unit,
    onBack: () -> Unit
) {
    var phoneEdit by remember(phone) { mutableStateOf(phone) }
    var marginEdit by remember(margin) { mutableStateOf(margin.toString()) }
    LazyColumn(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("پنل مدیریت ChemTrade", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onBack) { Icon(Icons.Default.Close, null) }
            }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3F8))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تنظیمات مدیریت", fontWeight = FontWeight.Bold)
                    OutlinedTextField(phoneEdit, { phoneEdit = it }, label = { Text("شماره تماس ChemTrade") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(marginEdit, { marginEdit = it.filter { c -> c.isDigit() } }, label = { Text("سود پیش‌فرض درصدی") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        onPhone(phoneEdit)
                        onMargin(marginEdit.toIntOrNull() ?: 0)
                    }) { Text("ذخیره تنظیمات") }
                }
            }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("ثبت آگهی مستقیم ChemTrade") }
            Spacer(Modifier.height(4.dp))
            Text("آگهی‌های در انتظار بررسی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(offers.filter { it.status == OfferStatus.Pending }, key = { it.id }) { offer ->
            AdminOfferCard(offer, offers)
        }
        item { Text("سایر آگهی‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        items(offers.filter { it.status != OfferStatus.Pending }, key = { it.id }) { offer ->
            AdminOfferCard(offer, offers)
        }
    }
}

@Composable
private fun AdminOfferCard(offer: ChemicalOffer, offers: MutableList<ChemicalOffer>) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(offer.name, fontWeight = FontWeight.Bold)
            Text("وضعیت: ${offer.status.title}")
            Text("فروشنده: ${offer.supplier}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (offer.status == OfferStatus.Pending) {
                    Button(onClick = {
                        val i = offers.indexOfFirst { it.id == offer.id }
                        if (i >= 0) offers[i] = offer.copy(status = OfferStatus.Approved)
                    }) { Text("تأیید") }
                    OutlinedButton(onClick = {
                        val i = offers.indexOfFirst { it.id == offer.id }
                        if (i >= 0) offers[i] = offer.copy(status = OfferStatus.Rejected)
                    }) { Text("رد") }
                }
                IconButton(onClick = { offers.removeAll { it.id == offer.id } }) { Icon(Icons.Default.Delete, "حذف", tint = ChemRed) }
            }
        }
    }
}

@Composable
private fun AdminLoginDialog(onDismiss: () -> Unit, onSuccess: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ورود به پنل مدیریت") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نسخه آزمایشی - رمز پیش‌فرض: 1234")
                OutlinedTextField(pin, { pin = it; error = false }, label = { Text("رمز مدیریت") }, singleLine = true, isError = error)
                if (error) Text("رمز صحیح نیست.", color = ChemRed)
            }
        },
        confirmButton = {
            Button(onClick = { if (pin == "1234") onSuccess() else error = true }) { Text("ورود") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun OfferDialog(offer: ChemicalOffer, supportPhone: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(offer.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (offer.officialPrice.isNotBlank()) Text("قیمت رسمی: ${offer.officialPrice}")
                if (offer.marketPrice.isNotBlank()) Text("قیمت بازار: ${offer.marketPrice}")
                Text("واحد: ${offer.unit}")
                Text("موجودی: ${offer.stock}")
                Text("مکان تحویل: ${offer.deliveryPlace}")
                Text("زمان تحویل: ${offer.deliveryTime}")
                Text("فروشنده: ${offer.supplier}")
            }
        },
        confirmButton = {
            Button(onClick = {
                val number = if (offer.isChemTrade) supportPhone else offer.phone
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            }) { Icon(Icons.Default.Phone, null); Spacer(Modifier.width(6.dp)); Text("تماس") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
private fun OfferFormDialog(
    adminMode: Boolean,
    supportPhone: String,
    defaultMargin: Int,
    onDismiss: () -> Unit,
    onSubmit: (ChemicalOffer) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var official by remember { mutableStateOf("") }
    var market by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("کیلوگرم") }
    var supplier by remember { mutableStateOf(if (adminMode) "ChemTrade" else "") }
    var stock by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(if (adminMode) supportPhone else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (adminMode) "ثبت آگهی مستقیم ChemTrade" else "ثبت آگهی فروش") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("نام ماده") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(category, { category = it }, label = { Text("دسته‌بندی") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(official, { official = it }, label = { Text("قیمت رسمی (اختیاری)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(market, { market = it }, label = { Text("قیمت غیررسمی / بازار (اختیاری)") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(unit, { unit = it }, label = { Text("واحد") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(supplier, { supplier = it }, label = { Text("نام فروشنده") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(stock, { stock = it }, label = { Text("موجودی / حداقل سفارش") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(place, { place = it }, label = { Text("مکان تحویل") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(time, { time = it }, label = { Text("زمان تحویل") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(phone, { phone = it }, label = { Text("شماره تماس") }, modifier = Modifier.fillMaxWidth()) }
                if (adminMode) item { Text("سود پیش‌فرض ChemTrade: $defaultMargin٪") }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && (official.isNotBlank() || market.isNotBlank()) && place.isNotBlank()) {
                    onSubmit(
                        ChemicalOffer(0, name, category.ifBlank { "سایر" }, official, market, unit, supplier.ifBlank { "فروشنده" }, stock.ifBlank { "نامشخص" }, place, time.ifBlank { "توافقی" }, phone, if (adminMode) OfferStatus.Approved else OfferStatus.Pending, adminMode, if (adminMode) defaultMargin else 0)
                    )
                }
            }) { Text("ثبت") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
