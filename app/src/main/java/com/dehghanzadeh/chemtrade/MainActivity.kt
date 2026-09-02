package com.dehghanzadeh.chemtrade

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

private enum class Status { PENDING, APPROVED, REJECTED }
private enum class MarginType { PERCENT, TOMAN }

private data class Offer(
    val id: Int = 0,
    val name: String = "",
    val official: String = "",
    val market: String = "",
    val place: String = "",
    val time: String = "",
    val supplier: String = "",
    val phone: String = "",
    val owner: String = "",
    val description: String = "",
    val photo: String = "",
    val status: Status = Status.PENDING,
    val reason: String = "",
    val marginType: MarginType = MarginType.PERCENT,
    val margin: Double = 0.0,
    val publishedOfficial: String = "",
    val publishedMarket: String = ""
)

private data class UserAccount(val phone: String, val type: String, val name: String = "", val company: String = "")

private val Navy = Color(0xFF0B1F33)
private val Gold = Color(0xFFC8A24A)
private val Cream = Color(0xFFFBF6EE)
private const val ADMIN_HASH = "a1fb4e703a9ef1fa4936801721ff285a97ac85330856674412e054892afe6972"

private fun digits(value: String): String = value.map { c ->
    when (c) {
        in '۰'..'۹' -> ('0'.code + c.code - '۰'.code).toChar()
        in '٠'..'٩' -> ('0'.code + c.code - '٠'.code).toChar()
        else -> c
    }
}.joinToString("")

private fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
private fun parseMoney(value: String): Long? = digits(value).filter(Char::isDigit).toLongOrNull()
private fun money(value: Long): String = String.format("%,d تومان", value)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChemTradeApp() }
    }
}

@Composable
private fun ChemTradeApp() {
    val scheme = lightColorScheme(
        primary = Navy,
        onPrimary = Color.White,
        secondary = Gold,
        onSecondary = Navy,
        background = Cream,
        onBackground = Navy,
        surface = Color.White,
        onSurface = Navy
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = scheme) { MainScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE) }
    var admin by remember { mutableStateOf(prefs.getBoolean("adminSession", false)) }
    var page by remember { mutableIntStateOf(0) }
    var showAdminLogin by remember { mutableStateOf(false) }
    var showOfferForm by remember { mutableStateOf(false) }
    var reviewOffer by remember { mutableStateOf<Offer?>(null) }
    var detailsOffer by remember { mutableStateOf<Offer?>(null) }
    var editOffer by remember { mutableStateOf<Offer?>(null) }
    var search by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(prefs.getString("phone", "").orEmpty()) }
    var offers by remember { mutableStateOf(loadOffers(prefs)) }

    fun saveOffer(input: Offer) {
        val list = offers.toMutableList()
        val index = list.indexOfFirst { it.id == input.id }
        if (index >= 0) list[index] = input else list.add(input.copy(id = (list.maxOfOrNull { it.id } ?: 0) + 1))
        offers = list
        persistOffers(prefs, list)
    }

    fun logout() {
        phone = ""
        prefs.edit().remove("phone").apply()
        admin = false
        prefs.edit().putBoolean("adminSession", false).apply()
        context.startActivity(Intent(context, EntryActivity::class.java))
        (context as? ComponentActivity)?.finish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ChemLink", fontWeight = FontWeight.ExtraBold)
                        Text(if (admin) "پنل مدیریت" else "بازار مواد اولیه", style = MaterialTheme.typography.labelSmall, color = Gold)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (admin) {
                            admin = false
                            prefs.edit().putBoolean("adminSession", false).apply()
                        } else page = 3
                    }) { Text(if (admin) "خروج از مدیریت" else "تنظیمات") }
                }
            )
        },
        bottomBar = {
            if (!admin) NavigationBar {
                NavigationBarItem(selected = page == 0, onClick = { page = 0 }, icon = { Text("⌂") }, label = { Text("بازار") })
                NavigationBarItem(selected = page == 1, onClick = { page = 1 }, icon = { Text("+") }, label = { Text("ثبت آگهی") })
                NavigationBarItem(selected = page == 2, onClick = { page = 2 }, icon = { Text("●") }, label = { Text("حساب") })
                NavigationBarItem(selected = page == 3, onClick = { page = 3 }, icon = { Text("⚙") }, label = { Text("تنظیمات") })
            }
        }
    ) { padding ->
        when {
            admin -> AdminDashboard(
                modifier = Modifier.padding(padding),
                offers = offers,
                onOpen = { reviewOffer = it },
                onSave = ::saveOffer
            )
            page == 0 -> MarketPage(Modifier.padding(padding), offers, search, { search = it }) { detailsOffer = it }
            page == 1 -> NewOfferPage(Modifier.padding(padding), phone) {
                if (phone.isBlank()) page = 2 else { editOffer = null; showOfferForm = true }
            }
            page == 2 -> AccountPage(Modifier.padding(padding), phone, { page = 0 }, ::logout) {
                editOffer = null
                showOfferForm = true
            }
            else -> SettingsPage(Modifier.padding(padding)) { showAdminLogin = true }
        }
    }

    if (showAdminLogin) {
        AdminLoginDialog(
            close = { showAdminLogin = false },
            success = {
                admin = true
                page = 0
                prefs.edit().putBoolean("adminSession", true).apply()
                showAdminLogin = false
            }
        )
    }

    if (showOfferForm) {
        OfferFormDialog(
            old = editOffer,
            owner = phone,
            close = { showOfferForm = false; editOffer = null },
            submit = {
                saveOffer(it)
                showOfferForm = false
                editOffer = null
                page = if (admin) 0 else 2
            }
        )
    }

    reviewOffer?.let { offer ->
        ReviewDialog(
            offer = offer,
            close = { reviewOffer = null },
            approve = { saveOffer(it); reviewOffer = null },
            reject = { target, reason -> saveOffer(target.copy(status = Status.REJECTED, reason = reason)); reviewOffer = null }
        )
    }

    detailsOffer?.let { offer -> OfferDetails(offer) { detailsOffer = null } }
}

@Composable
private fun MarketPage(modifier: Modifier, offers: List<Offer>, query: String, onQuery: (String) -> Unit, open: (Offer) -> Unit) {
    val visible = offers.filter { it.status == Status.APPROVED && (query.isBlank() || it.name.contains(query, true)) }
    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("ChemLink", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text("بازار حرفه‌ای مواد اولیه", color = Gold)
                    Text("قیمت نهایی رسمی و غیررسمی • بدون نمایش نام فروشنده", color = Color.White)
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("جستجوی ماده") })
        }
        items(visible, key = { it.id }) { offer ->
            Card(Modifier.fillMaxWidth().clickable { open(offer) }, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(offer.name, fontWeight = FontWeight.Bold)
                    Text("قیمت نهایی رسمی: ${offer.publishedOfficial}", fontWeight = FontWeight.SemiBold)
                    Text("قیمت نهایی غیررسمی: ${offer.publishedMarket}", fontWeight = FontWeight.SemiBold)
                    Text("مکان تحویل: ${offer.place}")
                    Text("زمان تحویل: ${offer.time}")
                }
            }
        }
        if (visible.isEmpty()) item { Text("آگهی تأییدشده‌ای وجود ندارد.") }
    }
}

@Composable
private fun AdminDashboard(modifier: Modifier, offers: List<Offer>, onOpen: (Offer) -> Unit, onSave: (Offer) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var showUsers by remember { mutableStateOf(false) }
    val prefs = LocalContext.current.getSharedPreferences("chemlink", Context.MODE_PRIVATE)
    val users = loadUsers(prefs)
    val list = when (tab) {
        0 -> offers.filter { it.status == Status.PENDING }
        1 -> offers.filter { it.status == Status.REJECTED }
        else -> offers.filter { it.status == Status.APPROVED }
    }
    Column(modifier.padding(16.dp)) {
        Text("مدیریت", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button({ tab = 0 }, Modifier.weight(1f)) { Text("در انتظار ${offers.count { it.status == Status.PENDING }}") }
            Button({ tab = 1 }, Modifier.weight(1f)) { Text("اصلاحیه ${offers.count { it.status == Status.REJECTED }}") }
            Button({ tab = 2 }, Modifier.weight(1f)) { Text("منتشر ${offers.count { it.status == Status.APPROVED }}") }
        }
        Spacer(Modifier.height(8.dp))
        Button({ showUsers = !showUsers }, Modifier.fillMaxWidth()) { Text(if (showUsers) "بازگشت به آگهی‌ها" else "تأمین‌کنندگان و مصرف‌کنندگان") }
        if (showUsers) {
            UserCounts(users)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { offer ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(offer.name, fontWeight = FontWeight.Bold)
                            Text("قیمت تأمین‌کننده رسمی: ${offer.official}")
                            Text("قیمت تأمین‌کننده غیررسمی: ${offer.market}")
                            Text("تحویل: ${offer.place} | ${offer.time}")
                            if (offer.reason.isNotBlank()) Text("اصلاحیه: ${offer.reason}", color = MaterialTheme.colorScheme.error)
                            Button({ onOpen(offer) }) { Text("باز کردن و بررسی") }
                        }
                    }
                }
                if (list.isEmpty()) item { Text("موردی برای نمایش نیست.") }
            }
        }
    }
}

@Composable
private fun UserCounts(users: List<UserAccount>) {
    val suppliers = users.count { it.type == "Supplier" }
    val consumers = users.count { it.type == "Consumer" }
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("ثبت‌نام کاربران", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("تأمین‌کنندگان: $suppliers نفر")
                Text("مصرف‌کنندگان: $consumers نفر")
                Text("مجموع کاربران: ${users.size} نفر", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReviewDialog(offer: Offer, close: () -> Unit, approve: (Offer) -> Unit, reject: (Offer, String) -> Unit) {
    var marginType by remember(offer.id) { mutableStateOf(offer.marginType) }
    var marginText by remember(offer.id) { mutableStateOf(if (offer.margin == 0.0) "" else offer.margin.toString()) }
    var reason by remember(offer.id) { mutableStateOf("") }
    val officialBase = parseMoney(offer.official) ?: 0L
    val marketBase = parseMoney(offer.market) ?: 0L
    val margin = marginText.toDoubleOrNull() ?: 0.0
    fun finalPrice(base: Long): Long = if (marginType == MarginType.PERCENT) base + (base * margin / 100.0).toLong() else base + margin.toLong()
    val finalOfficial = finalPrice(officialBase)
    val finalMarket = finalPrice(marketBase)

    AlertDialog(
        onDismissRequest = close,
        title = { Text("بررسی آگهی") },
        text = {
            Column(Modifier.heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(offer.name, fontWeight = FontWeight.Bold)
                if (offer.photo.isNotBlank()) PhotoView(offer.photo)
                Text("قیمت تأمین‌کننده رسمی: ${offer.official}")
                Text("قیمت تأمین‌کننده غیررسمی: ${offer.market}")
                Text("مکان تحویل: ${offer.place}")
                Text("زمان تحویل: ${offer.time}")
                Text("نام فروشنده: ${offer.supplier}")
                Text("شماره تماس: ${offer.phone}")
                Text("سود مدیریت روی هر دو قیمت اعمال می‌شود:", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = marginType == MarginType.PERCENT, onClick = { marginType = MarginType.PERCENT }, label = { Text("درصد") })
                    FilterChip(selected = marginType == MarginType.TOMAN, onClick = { marginType = MarginType.TOMAN }, label = { Text("تومان") })
                }
                OutlinedTextField(value = marginText, onValueChange = { marginText = it.filter { c -> c.isDigit() || c == '.' } }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("مقدار سود") })
                Text("قیمت نهایی رسمی: ${money(finalOfficial)}", color = Gold, fontWeight = FontWeight.Bold)
                Text("قیمت نهایی غیررسمی: ${money(finalMarket)}", color = Gold, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = reason, onValueChange = { reason = it }, modifier = Modifier.fillMaxWidth(), label = { Text("دلیل اصلاحیه") })
            }
        },
        confirmButton = {
            Button(onClick = {
                approve(offer.copy(status = Status.APPROVED, marginType = marginType, margin = margin, publishedOfficial = money(finalOfficial), publishedMarket = money(finalMarket)))
            }) { Text("تأیید و انتشار") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = close) { Text("بازگشت") }
                TextButton(onClick = { reject(offer, reason.ifBlank { "لطفاً اطلاعات آگهی اصلاح شود" }) }) { Text("ثبت اصلاحیه") }
            }
        }
    )
}

@Composable
private fun OfferFormDialog(old: Offer?, owner: String, close: () -> Unit, submit: (Offer) -> Unit) {
    var name by remember { mutableStateOf(old?.name.orEmpty()) }
    var official by remember { mutableStateOf(old?.official.orEmpty()) }
    var market by remember { mutableStateOf(old?.market.orEmpty()) }
    var place by remember { mutableStateOf(old?.place.orEmpty()) }
    var time by remember { mutableStateOf(old?.time.orEmpty()) }
    var supplier by remember { mutableStateOf(old?.supplier.orEmpty()) }
    var phone by remember { mutableStateOf(old?.phone ?: owner) }
    var description by remember { mutableStateOf(old?.description.orEmpty()) }
    var photo by remember { mutableStateOf(old?.photo.orEmpty()) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photo = it.toString()
        }
    }
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (old == null) "ثبت آگهی" else "ویرایش آگهی") },
        text = {
            Column(Modifier.heightIn(max = 650.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Field("نام ماده", name) { name = it }
                Field("قیمت تأمین‌کننده رسمی", official) { official = it }
                Field("قیمت تأمین‌کننده غیررسمی", market) { market = it }
                Field("مکان تحویل", place) { place = it }
                Field("زمان تحویل", time) { time = it }
                Field("نام فروشنده (فقط مدیریت)", supplier) { supplier = it }
                Field("شماره تماس", phone) { phone = digits(it).filter(Char::isDigit).take(11) }
                Field("توضیحات", description) { description = it }
                Button(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text(if (photo.isBlank()) "آپلود عکس محصول" else "تغییر عکس") }
                if (photo.isNotBlank()) PhotoView(photo)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) submit(
                    Offer(
                        id = old?.id ?: 0,
                        name = name,
                        official = official,
                        market = market,
                        place = place,
                        time = time,
                        supplier = supplier,
                        phone = phone,
                        owner = owner,
                        description = description,
                        photo = photo,
                        status = old?.status ?: Status.PENDING,
                        reason = old?.reason.orEmpty(),
                        marginType = old?.marginType ?: MarginType.PERCENT,
                        margin = old?.margin ?: 0.0,
                        publishedOfficial = old?.publishedOfficial.orEmpty(),
                        publishedMarket = old?.publishedMarket.orEmpty()
                    )
                )
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = close) { Text("انصراف") } }
    )
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(label) })
}

@Composable
private fun OfferDetails(offer: Offer, close: () -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text(offer.name) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (offer.photo.isNotBlank()) PhotoView(offer.photo)
            Text("قیمت نهایی رسمی: ${offer.publishedOfficial}")
            Text("قیمت نهایی غیررسمی: ${offer.publishedMarket}")
            Text("مکان تحویل: ${offer.place}")
            Text("زمان تحویل: ${offer.time}")
            Text(offer.description)
        }
    }, confirmButton = { TextButton(onClick = close) { Text("بستن") } })
}

@Composable
private fun PhotoView(value: String) {
    val context = LocalContext.current
    val bitmap = remember(value) { runCatching { context.contentResolver.openInputStream(Uri.parse(value)).use { BitmapFactory.decodeStream(it) } }.getOrNull() }
    if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = "عکس محصول", modifier = Modifier.fillMaxWidth().height(170.dp), contentScale = ContentScale.Crop)
}

@Composable
private fun NewOfferPage(modifier: Modifier, phone: String, onStart: () -> Unit) {
    Column(modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ثبت آگهی فروش", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(if (phone.isBlank()) "برای ثبت آگهی ابتدا وارد حساب شوید." else "آگهی شما ابتدا توسط مدیریت بررسی می‌شود.")
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(if (phone.isBlank()) "ورود به حساب" else "ثبت آگهی جدید") }
    }
}

@Composable
private fun AccountPage(modifier: Modifier, phone: String, home: () -> Unit, logout: () -> Unit, newOffer: () -> Unit) {
    val prefs = LocalContext.current.getSharedPreferences("chemlink", Context.MODE_PRIVATE)
    val users = loadUsers(prefs)
    val account = users.firstOrNull { it.phone == phone }
    Column(modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("حساب کاربری", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        if (phone.isBlank()) {
            Text("هنوز وارد حساب نشده‌اید.")
        } else {
            Text("شماره: $phone")
            Text("نوع حساب: ${if (account?.type == "Supplier") "تأمین‌کننده" else "مصرف‌کننده"}")
            account?.company?.takeIf { it.isNotBlank() }?.let { Text("شرکت: $it") }
            Button(onClick = newOffer, modifier = Modifier.fillMaxWidth()) { Text("ثبت آگهی") }
            OutlinedButton(onClick = logout, modifier = Modifier.fillMaxWidth()) { Text("خروج از حساب") }
        }
        TextButton(onClick = home) { Text("بازگشت به بازار") }
    }
}

@Composable
private fun SettingsPage(modifier: Modifier, admin: () -> Unit) {
    Column(modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("مدیریت سامانه", fontWeight = FontWeight.Bold); Text("ورود مخصوص مدیر برای بررسی و انتشار آگهی‌ها") } }
        Button(onClick = admin, modifier = Modifier.fillMaxWidth()) { Text("ورود به مدیریت") }
    }
}

@Composable
private fun AdminLoginDialog(close: () -> Unit, success: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = close, title = { Text("ورود مدیریت") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("کد مدیریت را وارد کنید.")
            OutlinedTextField(value = code, onValueChange = { code = digits(it).filter(Char::isDigit).take(8) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("کد مدیریت") })
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { Button(onClick = { if (hash(code) == ADMIN_HASH) success() else error = "کد مدیریت صحیح نیست." }) { Text("ورود") } }, dismissButton = { TextButton(onClick = close) { Text("انصراف") } })
}

private fun loadOffers(prefs: android.content.SharedPreferences): List<Offer> {
    val array = runCatching { JSONArray(prefs.getString("offers", "[]") ?: "[]") }.getOrDefault(JSONArray())
    val result = mutableListOf<Offer>()
    for (i in 0 until array.length()) {
        val o = array.optJSONObject(i) ?: continue
        result += Offer(
            id = o.optInt("id"), name = o.optString("name"), official = o.optString("official"), market = o.optString("market"),
            place = o.optString("place"), time = o.optString("time"), supplier = o.optString("supplier"), phone = o.optString("phone"),
            owner = o.optString("owner"), description = o.optString("description"), photo = o.optString("photo"),
            status = runCatching { Status.valueOf(o.optString("status", "PENDING")) }.getOrDefault(Status.PENDING), reason = o.optString("reason"),
            marginType = runCatching { MarginType.valueOf(o.optString("marginType", "PERCENT")) }.getOrDefault(MarginType.PERCENT),
            margin = o.optDouble("margin", 0.0), publishedOfficial = o.optString("publishedOfficial"), publishedMarket = o.optString("publishedMarket")
        )
    }
    return result
}

private fun persistOffers(prefs: android.content.SharedPreferences, offers: List<Offer>) {
    val array = JSONArray()
    offers.forEach { o ->
        array.put(JSONObject().apply {
            put("id", o.id); put("name", o.name); put("official", o.official); put("market", o.market); put("place", o.place); put("time", o.time)
            put("supplier", o.supplier); put("phone", o.phone); put("owner", o.owner); put("description", o.description); put("photo", o.photo)
            put("status", o.status.name); put("reason", o.reason); put("marginType", o.marginType.name); put("margin", o.margin)
            put("publishedOfficial", o.publishedOfficial); put("publishedMarket", o.publishedMarket)
        })
    }
    prefs.edit().putString("offers", array.toString()).apply()
}

private fun loadUsers(prefs: android.content.SharedPreferences): List<UserAccount> {
    val array = runCatching { JSONArray(prefs.getString("users", "[]") ?: "[]") }.getOrDefault(JSONArray())
    val result = mutableListOf<UserAccount>()
    for (i in 0 until array.length()) {
        val o = array.optJSONObject(i) ?: continue
        result += UserAccount(o.optString("phone"), o.optString("type", "Consumer"), o.optString("name"), o.optString("company"))
    }
    return result
}
