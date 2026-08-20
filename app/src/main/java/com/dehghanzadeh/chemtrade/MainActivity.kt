package com.dehghanzadeh.chemtrade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection

private data class Chemical(
    val name: String,
    val category: String,
    val price: String,
    val unit: String,
    val supplier: String,
    val stock: String,
    val badge: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChemTradeApp() }
    }
}

@Composable
private fun ChemTradeApp() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme()) {
            ChemTradeHome()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChemTradeHome() {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedChemical by remember { mutableStateOf<Chemical?>(null) }
    var supplierDialog by remember { mutableStateOf(false) }
    var requestSent by remember { mutableStateOf(false) }

    val chemicals = remember {
        listOf(
            Chemical("مونو اتانول آمین (MEA)", "آمین‌ها و افزودنی‌ها", "۱۲۸,۵۰۰ تومان", "کیلوگرم", "بازرگانی دهقان‌زاده", "بشکه ۲۰۰ کیلویی", "پیشنهادی"),
            Chemical("اسید استیک", "اسیدها", "۷۹,۰۰۰ تومان", "کیلوگرم", "تأمین‌کننده تأییدشده", "موجود", "تأییدشده"),
            Chemical("متانول", "حلال‌ها", "۴۳,۵۰۰ تومان", "کیلوگرم", "تأمین‌کننده تأییدشده", "موجود", "فوری"),
            Chemical("اوره صنعتی", "مواد اولیه کود", "۲۶,۰۰۰ تومان", "کیلوگرم", "تأمین‌کننده تأییدشده", "حداقل سفارش ۱ تن", "عمده")
        )
    }
    val filtered = chemicals.filter {
        it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ChemTrade", style = MaterialTheme.typography.titleLarge)
                        Text("بازار حرفه‌ای مواد اولیه", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = { selectedTab = 0 }) {
                        Icon(Icons.Default.Search, contentDescription = "جستجو")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ShoppingCart, null) },
                    label = { Text("بازار") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Inventory2, null) },
                    label = { Text("تأمین") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.AccountCircle, null) },
                    label = { Text("حساب من") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> MarketScreen(
                modifier = Modifier.padding(padding),
                query = query,
                onQueryChange = { query = it },
                chemicals = filtered,
                onSelect = { selectedChemical = it }
            )
            1 -> SupplyScreen(
                modifier = Modifier.padding(padding),
                onAddOffer = { supplierDialog = true }
            )
            else -> AccountScreen(
                modifier = Modifier.padding(padding),
                requestSent = requestSent,
                onRequest = { requestSent = true }
            )
        }
    }

    selectedChemical?.let { chemical ->
        AlertDialog(
            onDismissRequest = { selectedChemical = null },
            title = { Text(chemical.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("قیمت فعلی: ${chemical.price} / ${chemical.unit}")
                    Text("موجودی: ${chemical.stock}")
                    Text("فروشنده: ${chemical.supplier}")
                    Text("پس از ثبت درخواست، تیم ChemTrade قیمت و شرایط نهایی را برای شما تأیید می‌کند.")
                }
            },
            confirmButton = {
                Button(onClick = { selectedChemical = null; requestSent = true }) { Text("ثبت درخواست خرید") }
            },
            dismissButton = {
                TextButton(onClick = { selectedChemical = null }) { Text("بستن") }
            }
        )
    }

    if (supplierDialog) {
        AlertDialog(
            onDismissRequest = { supplierDialog = false },
            title = { Text("ثبت پیشنهاد تأمین") },
            text = {
                Text("در نسخه نهایی، تأمین‌کننده ماده، موجودی، حداقل سفارش و قیمت پایه را ثبت می‌کند. ChemTrade با سود مشخص، قیمت نهایی را به خریداران نمایش می‌دهد.")
            },
            confirmButton = {
                Button(onClick = { supplierDialog = false }) { Text("ثبت آزمایشی") }
            },
            dismissButton = {
                TextButton(onClick = { supplierDialog = false }) { Text("بستن") }
            }
        )
    }
}

@Composable
private fun MarketScreen(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
    chemicals: List<Chemical>,
    onSelect: (Chemical) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("بازار مواد اولیه", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text("قیمت‌ها از موجودی و پیشنهادهای تأمین‌کنندگان به‌روزرسانی می‌شوند.")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                label = { Text("جستجوی ماده شیمیایی") }
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("پرفروش") })
                AssistChip(onClick = {}, label = { Text("موجود") })
                AssistChip(onClick = {}, label = { Text("تحویل فوری") })
            }
        }
        if (chemicals.isEmpty()) {
            item { Text("ماده‌ای با این عبارت پیدا نشد.") }
        }
        items(chemicals) { chemical ->
            ChemicalCard(chemical = chemical, onClick = { onSelect(chemical) })
        }
    }
}

@Composable
private fun ChemicalCard(chemical: Chemical, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(chemical.name, style = MaterialTheme.typography.titleMedium)
                    Text(chemical.category, style = MaterialTheme.typography.bodySmall)
                }
                AssistChip(onClick = {}, label = { Text(chemical.badge) })
            }
            Spacer(Modifier.height(12.dp))
            Text(chemical.price, style = MaterialTheme.typography.titleLarge)
            Text("هر ${chemical.unit}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text("${chemical.stock} • ${chemical.supplier}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text("مشاهده و درخواست خرید")
            }
        }
    }
}

@Composable
private fun SupplyScreen(modifier: Modifier = Modifier, onAddOffer: () -> Unit) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("پنل تأمین‌کنندگان", style = MaterialTheme.typography.headlineSmall)
            Text("قیمت پایه و موجودی خود را ثبت کنید تا پس از بررسی در بازار نمایش داده شود.")
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("فرآیند همکاری", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("۱. ثبت ماده و مشخصات")
                    Text("۲. ثبت قیمت پایه توسط تأمین‌کننده")
                    Text("۳. اعمال سود ChemTrade و نمایش قیمت نهایی")
                    Text("۴. دریافت درخواست واقعی خریدار")
                }
            }
        }
        item {
            Button(onClick = onAddOffer, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("ثبت پیشنهاد تأمین")
            }
        }
    }
}

@Composable
private fun AccountScreen(modifier: Modifier = Modifier, requestSent: Boolean, onRequest: () -> Unit) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("حساب کاربری", style = MaterialTheme.typography.headlineSmall)
            Text("نسخه آزمایشی ChemTrade")
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("خریدار صنعتی", style = MaterialTheme.typography.titleMedium)
                    Text("برای مشاهده سفارش‌ها و قیمت‌های اختصاصی وارد حساب کاربری شوید.")
                }
            }
        }
        item {
            if (requestSent) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("درخواست ثبت شد", style = MaterialTheme.typography.titleMedium)
                        Text("این فقط یک نمونه اولیه است و درخواست به سرور واقعی ارسال نمی‌شود.")
                    }
                }
            } else {
                Button(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                    Text("ثبت درخواست آزمایشی")
                }
            }
        }
    }
}
