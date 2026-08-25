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
    Pending("در انتظار بررسی"),
    Approved("تأیید شده"),
    Rejected("رد شده")
}

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
        primaryContainer = Color(0xFFE8F1F7),
        surface = Color(0xFFFCFCFC)
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = colors) {
            ChemTradeHome()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChemTradeHome() {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var selectedOffer by remember { mutableStateOf<ChemicalOffer?>(null) }
    var supportPhone by remember { mutableStateOf("02100000000") }
    var defaultMargin by remember { mutableIntStateOf(3) }

    val offers = remember {
        mutableStateListOf(
            ChemicalOffer(
                1,
                "مونو اتانول آمین (MEA)",
                "۱۲۵,۰۰۰ تومان",
                "۱۲۸,۵۰۰ تومان",
                "تهران، شورآباد یا حواله از درب پتروشیمی",
                "هماهنگی تلفنی",
                "بازرگانی دهقان‌زاده",
                "02100000000",
                OfferStatus.Approved,
                true,
                3
            ),
            ChemicalOffer(
                2,
                "اسید استیک",
                "۷۷,۰۰۰ تومان",
                "۷۹,۰۰۰ تومان",
                "انبار تهران",
                "۱ تا ۲ روز کاری",
                "تأمین‌کننده تأییدشده",
                "02100000000",
                OfferStatus.Approved
            ),
            ChemicalOffer(
                3,
                "متانول",
                "۴۲,۰۰۰ تومان",
                "۴۳,۵۰۰ تومان",
                "عسلویه / تحویل توافقی",
                "تحویل فوری",
                "تأمین‌کننده تأییدشده",
                "02100000000",
                OfferStatus.Approved
            ),
            ChemicalOffer(
                4,
                "اوره صنعتی",
                "۲۵,۰۰۰ تومان",
                "۲۶,۰۰۰ تومان",
                "انبار فروشنده",
                "پس از تأیید ادمین",
                "تأمین‌کننده جدید",
                "02100000000",
                OfferStatus.Pending
            )
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
                    IconButton(onClick = { tab = 3 }) {
                        Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Store, contentDescription = null) },
                    label = { Text("بازار") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("ثبت آگهی") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    label = { Text("حساب") }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("مدیریت") }
                )
            }
        }
    ) { padding ->
        when (tab) {
            0 -> MarketScreen(
                modifier = Modifier.padding(padding),
                offers = offers,
                query = query,
                onQuery = { query = it },
                onSelect = { selectedOffer = it }
            )
            1 -> AddOfferScreen(
                modifier = Modifier.padding(padding),
                onAdd = { showForm = true }
            )
            2 -> AccountScreen(
                modifier = Modifier.padding(padding),
                phone = supportPhone
            )
            3 -> SettingsScreen(
                modifier = Modifier.padding(padding),
                isAdmin = isAdmin,
                onLogin = { showLogin = true },
                onLogout = { isAdmin = false },
                onAdmin = { tab = 4 }
            )
            4 -> if (isAdmin) {
                AdminScreen(
                    modifier = Modifier.padding(padding),
                    offers = offers,
                    phone = supportPhone,
                    margin = defaultMargin,
                    onPhone = { supportPhone = it },
                    onMargin = { defaultMargin = it },
                    onAdd = { showForm = true },
                    onBack = { tab = 0 }
                )
            } else {
                tab = 3
            }
        }
    }

    selectedOffer?.let { offer ->
        OfferDialog(
            offer = offer,
            fallbackPhone = supportPhone,
            onDismiss = { selectedOffer = null }
        )
    }

    if (showLogin) {
        AdminLoginDialog(
            onDismiss = { showLogin = false },
            onSuccess = {
                isAdmin = true
                showLogin = false
                tab = 4
            }
        )
    }

    if (showForm) {
        OfferFormDialog(
            adminMode = isAdmin && tab == 4,
            supportPhone = supportPhone,
            defaultMargin = defaultMargin,
            onDismiss = { showForm = false },
            onSubmit = { offer ->
                val newId = (offers.maxOfOrNull { it.id } ?: 0) + 1
                offers.add(offer.copy(id = newId))
                showForm = false
                tab = if (isAdmin) 4 else 0
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
        it.status == OfferStatus.Approved &&
            (it.name.contains(query, ignoreCase = true) ||
                it.supplier.contains(query, ignoreCase = true))
    }

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = ChemBlue)) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "بازار تخصصی مواد اولیه",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "قیمت رسمی و غیررسمی، زمان و مکان تحویل و ارتباط مستقیم",
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("جستجوی ماده یا فروشنده") }
            )
        }

        if (visible.any { it.isChemTrade }) {
            item {
                Text(
                    "⭐ آگهی‌های مستقیم ChemTrade",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(visible.filter { it.isChemTrade }, key = { it.id }) { offer ->
                OfferCard(offer = offer, onClick = { onSelect(offer) })
            }
        }

        item {
            Text(
                "جدیدترین آگهی‌های تأییدشده",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(visible.filterNot { it.isChemTrade }, key = { it.id }) { offer ->
            OfferCard(offer = offer, onClick = { onSelect(offer) })
        }

        if (visible.isEmpty()) {
            item { Text("آگهی تأییدشده‌ای پیدا نشد.") }
        }
    }
}

@Composable
private fun OfferCard(offer: ChemicalOffer, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        offer.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(offer.supplier, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(
                    onClick = onClick,
                    label = { Text(if (offer.isChemTrade) "ویژه ChemTrade" else "تأییدشده") }
                )
            }
            if (offer.officialPrice.isNotBlank()) {
                Text("قیمت رسمی: ${offer.officialPrice}", color = ChemGreen, fontWeight = FontWeight.Bold)
            }
            if (offer.marketPrice.isNotBlank()) {
                Text("قیمت بازار: ${offer.marketPrice}", color = ChemBlue, fontWeight = FontWeight.Bold)
            }
            InfoLine(Icons.Default.LocationOn, "تحویل: ${offer.deliveryPlace}")
            InfoLine(Icons.Default.Schedule, "زمان تحویل: ${offer.deliveryTime}")
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text("جزئیات و تماس")
            }
        }
    }
}

@Composable
private fun InfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ChemGold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AddOfferScreen(modifier: Modifier, onAdd: () -> Unit) {
    Column(
        modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.Default.Inventory2, contentDescription = null, tint = ChemBlue, modifier = Modifier.size(48.dp))
        Text("ثبت آگهی فروش", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("آگهی شما ابتدا برای بررسی مدیر ارسال می‌شود و فقط بعد از تأیید منتشر خواهد شد.")
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text("ثبت آگهی جدید")
        }
    }
}

@Composable
private fun AccountScreen(modifier: Modifier, phone: String) {
    Column(
        modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("حساب کاربری", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("برای ثبت و مدیریت آگهی‌ها، حساب کاربری کامل در نسخه آنلاین فعال خواهد شد.")
        Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
            Text("درخواست همکاری")
        }
        CallButton(phone = phone, title = "تماس با ChemTrade")
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    isAdmin: Boolean,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    onAdmin: () -> Unit
) {
    Column(
        modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("پنل مدیریت", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (isAdmin) {
                    Button(onClick = onAdmin, modifier = Modifier.fillMaxWidth()) {
                        Text("ورود به داشبورد مدیریت")
                    }
                    TextButton(onClick = onLogout) {
                        Text("خروج از مدیریت")
                    }
                } else {
                    Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
                        Text("ورود مدیر")
                    }
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

    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("پنل مدیریت ChemTrade", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF3F8))) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("تنظیمات مدیریت", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = phoneEdit,
                        onValueChange = { phoneEdit = it },
                        label = { Text("شماره تماس ChemTrade") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = marginEdit,
                        onValueChange = { marginEdit = it.filter(Char::isDigit) },
                        label = { Text("سود پیش‌فرض درصدی") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onPhone(phoneEdit)
                            onMargin(marginEdit.toIntOrNull() ?: 0)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ذخیره تنظیمات")
                    }
                    OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                        Text("ثبت آگهی مستقیم ChemTrade")
                    }
                }
            }
        }

        item {
            Text("آگهی‌های در انتظار تأیید", fontWeight = FontWeight.Bold)
        }
        items(offers.filter { it.status == OfferStatus.Pending }, key = { it.id }) { offer ->
            PendingOfferCard(
                offer = offer,
                onApprove = { offer.status = OfferStatus.Approved },
                onReject = { offer.status = OfferStatus.Rejected },
                onDelete = { offers.remove(offer) }
            )
        }

        item {
            Text("همه آگهی‌ها", fontWeight = FontWeight.Bold)
        }
        items(offers, key = { it.id }) { offer ->
            AdminOfferCard(
                offer = offer,
                onDelete = { offers.remove(offer) }
            )
        }
    }
}

@Composable
private fun PendingOfferCard(
    offer: ChemicalOffer,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit
) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(offer.name, fontWeight = FontWeight.Bold)
            Text("فروشنده: ${offer.supplier}")
            Text("تحویل: ${offer.deliveryPlace}")
            Text("زمان: ${offer.deliveryTime}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onApprove) { Text("تأیید") }
                OutlinedButton(onClick = onReject) { Text("رد") }
                TextButton(onClick = onDelete) { Text("حذف", color = ChemRed) }
            }
        }
    }
}

@Composable
private fun AdminOfferCard(
    offer: ChemicalOffer,
    onDelete: () -> Unit
) {
    Card {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(offer.name, fontWeight = FontWeight.Bold)
                Text("وضعیت: ${offer.status.title}", style = MaterialTheme.typography.bodySmall)
                Text("سود ثبت‌شده: ${offer.marginPercent}%", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = ChemRed)
            }
        }
    }
}

@Composable
private fun OfferDialog(
    offer: ChemicalOffer,
    fallbackPhone: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(offer.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("فروشنده: ${offer.supplier}")
                if (offer.officialPrice.isNotBlank()) Text("قیمت رسمی: ${offer.officialPrice}")
                if (offer.marketPrice.isNotBlank()) Text("قیمت بازار: ${offer.marketPrice}")
                Text("مکان تحویل: ${offer.deliveryPlace}")
                Text("زمان تحویل: ${offer.deliveryTime}")
                CallButton(phone = if (offer.phone.isBlank()) fallbackPhone else offer.phone, title = "تماس مستقیم")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن") }
        }
    )
}

@Composable
private fun CallButton(phone: String, title: String) {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Phone, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(title)
    }
}

@Composable
private fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ورود مدیر") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("رمز مدیریت نسخه اولیه را وارد کنید.")
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = false
                    },
                    singleLine = true,
                    label = { Text("رمز مدیریت") },
                    isError = error
                )
                if (error) {
                    Text("رمز صحیح نیست.", color = ChemRed)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (password == "ChemTrade2026") onSuccess() else error = true
            }) {
                Text("ورود")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
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
    var officialPrice by remember { mutableStateOf("") }
    var marketPrice by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf(if (adminMode) "ChemTrade" else "") }
    var phone by remember { mutableStateOf(if (adminMode) supportPhone else "") }
    var margin by remember { mutableStateOf(defaultMargin.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (adminMode) "ثبت آگهی مستقیم" else "ثبت آگهی جدید") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(name, { name = it }, label = { Text("نام ماده") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(officialPrice, { officialPrice = it }, label = { Text("قیمت رسمی - اختیاری") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(marketPrice, { marketPrice = it }, label = { Text("قیمت غیررسمی / بازار - اختیاری") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(place, { place = it }, label = { Text("مکان تحویل") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(time, { time = it }, label = { Text("زمان تحویل") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(supplier, { supplier = it }, label = { Text("نام فروشنده") }, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(phone, { phone = it }, label = { Text("شماره تماس") }, modifier = Modifier.fillMaxWidth())
                }
                if (adminMode) {
                    item {
                        OutlinedTextField(
                            value = margin,
                            onValueChange = { margin = it.filter(Char::isDigit) },
                            label = { Text("سود این آگهی درصدی") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank() && place.isNotBlank() && time.isNotBlank()) {
                    onSubmit(
                        ChemicalOffer(
                            id = 0,
                            name = name,
                            officialPrice = officialPrice,
                            marketPrice = marketPrice,
                            deliveryPlace = place,
                            deliveryTime = time,
                            supplier = supplier.ifBlank { "فروشنده جدید" },
                            phone = phone.ifBlank { supportPhone },
                            status = if (adminMode) OfferStatus.Approved else OfferStatus.Pending,
                            isChemTrade = adminMode,
                            marginPercent = margin.toIntOrNull() ?: 0
                        )
                    )
                }
            }) {
                Text("ثبت")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
