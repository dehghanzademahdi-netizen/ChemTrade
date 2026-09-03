package com.dehghanzadeh.chemtrade

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

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
private val Blue = Color(0xFF1565A8)
private val Gold = Color(0xFFC8A24A)
private val Cream = Color(0xFFFBF6EE)
private val Green = Color(0xFF2E7D5B)
private val Red = Color(0xFFB3261E)
private const val ADMIN_PHONE = "09357236476"
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
private fun waUri(): Uri = Uri.parse("https://wa.me/989357236476")
private fun tgUri(): Uri = Uri.parse("tg://resolve?phone=989357236476")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ChemTradeApp() } }
}

@Composable
private fun ChemTradeApp() {
    val scheme = lightColorScheme(primary = Navy, onPrimary = Color.White, secondary = Gold, onSecondary = Navy, background = Cream, onBackground = Navy, surface = Color.White, onSurface = Navy)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { MaterialTheme(colorScheme = scheme) { MainScreen() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE) }
    var admin by remember { mutableStateOf(false) }
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
        val list = offers.toMutableList(); val index = list.indexOfFirst { it.id == input.id }
        if (index >= 0) list[index] = input else list.add(input.copy(id = (list.maxOfOrNull { it.id } ?: 0) + 1))
        offers = list; persistOffers(prefs, list)
    }
    fun logout() {
        phone = ""; admin = false; prefs.edit().remove("phone").remove("type").remove("adminSession").apply()
        context.startActivity(Intent(context, EntryActivity::class.java)); (context as? ComponentActivity)?.finish()
    }

    Scaffold(
        topBar = { TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(34.dp))
                Column { Text("ChemLink", fontWeight = FontWeight.ExtraBold); Text(if (admin) "پنل مدیریت" else "بازار مواد اولیه", style = MaterialTheme.typography.labelSmall, color = Gold) }
            }
        }, actions = { if (admin) TextButton({ admin = false; page = 0 }) { Text("خروج") } }) },
        bottomBar = { if (!admin) NavigationBar {
            NavigationBarItem(page == 0, { page = 0 }, { Text("⌂") }, label = { Text("بازار") })
            NavigationBarItem(page == 1, { page = 1 }, { Text("+") }, label = { Text("ثبت آگهی") })
            NavigationBarItem(page == 2, { page = 2 }, { Text("●") }, label = { Text("حساب من") })
            NavigationBarItem(page == 4, { page = 4 }, { Text("▤") }, label = { Text("آگهی‌های من") })
        } }
    ) { padding ->
        when {
            admin -> AdminDashboard(Modifier.padding(padding), offers, { reviewOffer = it }, { editOffer = null; showOfferForm = true })
            page == 0 -> MarketPage(Modifier.padding(padding), offers, search, { search = it }) { detailsOffer = it }
            page == 1 -> NewOfferPage(Modifier.padding(padding), phone) { if (phone.isBlank()) page = 2 else { editOffer = null; showOfferForm = true } }
            page == 2 -> AccountPage(Modifier.padding(padding), phone, { page = 0 }, ::logout, { editOffer = null; showOfferForm = true }, { page = 4 }, { openExternal(context, waUri()) })
            page == 4 -> MyOffersPage(Modifier.padding(padding), offers, phone, { detailsOffer = it }, { offer, msg -> saveOffer(offer.copy(status = Status.PENDING, reason = msg)) }, { editOffer = it; showOfferForm = true })
        }
    }

    if (showAdminLogin) AdminLoginDialog({ showAdminLogin = false }, { admin = true; page = 0; showAdminLogin = false })
    if (showOfferForm) OfferFormDialog(editOffer, if (admin) ADMIN_PHONE else phone, { showOfferForm = false; editOffer = null }) { saveOffer(it); showOfferForm = false; editOffer = null; page = if (admin) 0 else 4 }
    reviewOffer?.let { offer -> ReviewDialog(offer, { reviewOffer = null }, { saveOffer(it); reviewOffer = null }, { target, reason -> saveOffer(target.copy(status = Status.REJECTED, reason = reason)); reviewOffer = null }) }
    detailsOffer?.let { offer -> OfferDetails(offer) { detailsOffer = null } }
}

@Composable
private fun MarketPage(modifier: Modifier, offers: List<Offer>, query: String, onQuery: (String) -> Unit, open: (Offer) -> Unit) {
    val visible = offers.filter { it.status == Status.APPROVED && (query.isBlank() || it.name.contains(query, true)) }
    LazyColumn(modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            HiddenAdminBrand { /* intentionally hidden management entry */ }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), singleLine = true, label = { Text("جستجوی ماده") }, placeholder = { Text("مثلاً منواتانول آمین") })
            Spacer(Modifier.height(6.dp)); Text("آگهی‌های تأییدشده", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        }
        items(visible, key = { it.id }) { offer ->
            Card(Modifier.fillMaxWidth().clickable { open(offer) }, shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(offer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold); Text("تأیید شده", color = Green, style = MaterialTheme.typography.labelMedium)
                    }
                    if (offer.publishedOfficial.isNotBlank()) Text("قیمت رسمی: ${offer.publishedOfficial}", fontWeight = FontWeight.SemiBold)
                    if (offer.publishedMarket.isNotBlank()) Text("قیمت بازار: ${offer.publishedMarket}", fontWeight = FontWeight.SemiBold)
                    Text("مکان تحویل: ${offer.place}"); Text("زمان تحویل: ${offer.time}")
                    Text("مشاهده جزئیات و خرید ←", color = Blue, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (visible.isEmpty()) item { Text("آگهی تأییدشده‌ای برای نمایش وجود ندارد.") }
    }
}

@Composable
private fun HiddenAdminBrand(onUnlock: () -> Unit) {
    var longPressed by remember { mutableStateOf(false) }
    LaunchedEffect(longPressed) { if (longPressed) { onUnlock(); longPressed = false } }
    Box(Modifier.fillMaxWidth().pointerInput(Unit) {
        androidx.compose.foundation.gestures.awaitEachGesture {
            awaitFirstDown(false)
            var released = false
            val job = launch { delay(5000); if (!released) longPressed = true }
            waitForUpOrCancellation(); released = true; job.cancel()
        }
    }, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(62.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("ChemLink", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Navy); Text("بازار حرفه‌ای مواد اولیه", color = Gold, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun AdminDashboard(modifier: Modifier, offers: List<Offer>, onOpen: (Offer) -> Unit, onNew: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }; var showUsers by remember { mutableStateOf(false) }
    val prefs = LocalContext.current.getSharedPreferences("chemlink", Context.MODE_PRIVATE); val users = loadUsers(prefs)
    val list = when (tab) { 0 -> offers.filter { it.status == Status.PENDING }; 1 -> offers.filter { it.status == Status.REJECTED }; else -> offers.filter { it.status == Status.APPROVED } }
    Column(modifier.padding(14.dp)) {
        Text("پنل مدیریت", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Text("کنترل آگهی، سود، قیمت و زمان انتشار", color = Gold)
        Spacer(Modifier.height(10.dp)); Button(onNew, Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text("＋ ثبت آگهی مدیریت") }; Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Button({ tab = 0 }, Modifier.weight(1f)) { Text("در انتظار ${offers.count { it.status == Status.PENDING }}") }
            Button({ tab = 1 }, Modifier.weight(1f)) { Text("اصلاحیه ${offers.count { it.status == Status.REJECTED }}") }
            Button({ tab = 2 }, Modifier.weight(1f)) { Text("منتشر ${offers.count { it.status == Status.APPROVED }}") }
        }
        Spacer(Modifier.height(8.dp)); OutlinedButton({ showUsers = !showUsers }, Modifier.fillMaxWidth()) { Text(if (showUsers) "بازگشت به آگهی‌ها" else "تأمین‌کنندگان و مصرف‌کنندگان") }
        if (showUsers) UserCounts(users) else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list, key = { it.id }) { offer -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(offer.name, fontWeight = FontWeight.Bold); Text("قیمت رسمی تأمین‌کننده: ${offer.official}"); Text("قیمت بازار تأمین‌کننده: ${offer.market}"); Text("تحویل: ${offer.place} | ${offer.time}")
                if (offer.reason.isNotBlank()) Text("پیام/اصلاحیه: ${offer.reason}", color = Red); Button({ onOpen(offer) }) { Text("باز کردن و بررسی") }
            } } }
            if (list.isEmpty()) item { Text("موردی برای نمایش نیست.") }
        }
    }
}

@Composable private fun UserCounts(users: List<UserAccount>) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("کاربران سامانه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("تأمین‌کنندگان: ${users.count { it.type == "Supplier" }} نفر"); Text("مصرف‌کنندگان: ${users.count { it.type == "Consumer" }} نفر"); Text("مجموع: ${users.size} نفر", fontWeight = FontWeight.Bold) } }
}

@Composable
private fun ReviewDialog(offer: Offer, close: () -> Unit, approve: (Offer) -> Unit, reject: (Offer, String) -> Unit) {
    var marginType by remember(offer.id) { mutableStateOf(offer.marginType) }; var marginText by remember(offer.id) { mutableStateOf(if (offer.margin == 0.0) "" else offer.margin.toString()) }; var reason by remember(offer.id) { mutableStateOf("") }
    val officialBase = parseMoney(offer.official); val marketBase = parseMoney(offer.market); val margin = marginText.replace(',', '.').toDoubleOrNull() ?: 0.0
    fun finalPrice(base: Long?): Long? = base?.let { if (marginType == MarginType.PERCENT) it + (it * margin / 100.0).toLong() else it + margin.toLong() }
    val finalOfficial = finalPrice(officialBase); val finalMarket = finalPrice(marketBase)
    AlertDialog(onDismissRequest = close, title = { Text("بررسی و تأیید آگهی") }, text = { Column(Modifier.heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(offer.name, fontWeight = FontWeight.ExtraBold); if (offer.photo.isNotBlank()) PhotoView(offer.photo); Text("قیمت رسمی تأمین‌کننده: ${offer.official.ifBlank { "ثبت نشده" }}"); Text("قیمت غیررسمی تأمین‌کننده: ${offer.market.ifBlank { "ثبت نشده" }}"); Text("مکان تحویل: ${offer.place}"); Text("زمان تحویل: ${offer.time}"); Text("نام فروشنده: ${offer.supplier}"); Text("تماس: ${offer.phone}")
        Text("سود مدیریت", fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(marginType == MarginType.PERCENT, { marginType = MarginType.PERCENT }, label = { Text("درصد") }); FilterChip(marginType == MarginType.TOMAN, { marginType = MarginType.TOMAN }, label = { Text("تومان") }) }
        OutlinedTextField(marginText, { marginText = digits(it).filter { c -> c.isDigit() || c == '.' || c == ',' } }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(if (marginType == MarginType.PERCENT) "درصد سود" else "مبلغ سود به تومان") })
        Text("قیمت نهایی رسمی: ${finalOfficial?.let(::money) ?: "قابل محاسبه نیست"}", color = Gold, fontWeight = FontWeight.Bold); Text("قیمت نهایی غیررسمی: ${finalMarket?.let(::money) ?: "قابل محاسبه نیست"}", color = Gold, fontWeight = FontWeight.Bold)
        OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), minLines = 2, label = { Text("دلیل اصلاحیه در صورت رد") })
    } }, confirmButton = { Button(enabled = finalOfficial != null || finalMarket != null, onClick = { approve(offer.copy(status = Status.APPROVED, marginType = marginType, margin = margin, publishedOfficial = finalOfficial?.let(::money).orEmpty(), publishedMarket = finalMarket?.let(::money).orEmpty(), reason = "")) }) { Text("اعمال سود و انتشار") } }, dismissButton = { Row { TextButton({ reject(offer, reason.ifBlank { "لطفاً اطلاعات آگهی اصلاح شود" }) }) { Text("رد + اصلاحیه", color = Red) }; TextButton(close) { Text("بستن") } } })
}

@Composable
private fun OfferFormDialog(old: Offer?, owner: String, close: () -> Unit, submit: (Offer) -> Unit) {
    var name by remember { mutableStateOf(old?.name.orEmpty()) }; var official by remember { mutableStateOf(old?.official.orEmpty()) }; var market by remember { mutableStateOf(old?.market.orEmpty()) }; var place by remember { mutableStateOf(old?.place.orEmpty()) }; var time by remember { mutableStateOf(old?.time.orEmpty()) }; var supplier by remember { mutableStateOf(old?.supplier.orEmpty()) }; var phone by remember { mutableStateOf(old?.phone ?: owner) }; var description by remember { mutableStateOf(old?.description.orEmpty()) }; var photo by remember { mutableStateOf(old?.photo.orEmpty()) }
    val context = LocalContext.current; val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }; photo = it.toString() } }
    AlertDialog(onDismissRequest = close, title = { Text(if (old == null) "ثبت آگهی" else "ویرایش آگهی") }, text = { Column(Modifier.heightIn(max = 650.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Field("نام ماده", name) { name = it }; Field("قیمت رسمی تأمین‌کننده", official) { official = it }; Field("قیمت غیررسمی تأمین‌کننده", market) { market = it }; Field("مکان تحویل", place) { place = it }; Field("زمان تحویل", time) { time = it }; Field("نام فروشنده (فقط مدیریت)", supplier) { supplier = it }; Field("شماره تماس", phone) { phone = digits(it).filter(Char::isDigit).take(11) }; Field("توضیحات", description) { description = it }
        Button({ picker.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth()) { Text(if (photo.isBlank()) "آپلود عکس محصول" else "تغییر عکس") }; if (photo.isNotBlank()) PhotoView(photo)
    } }, confirmButton = { Button({ if (name.isNotBlank()) submit(Offer(id = old?.id ?: 0, name = name, official = official, market = market, place = place, time = time, supplier = supplier, phone = phone, owner = owner, description = description, photo = photo, status = old?.status ?: Status.PENDING, reason = old?.reason.orEmpty(), marginType = old?.marginType ?: MarginType.PERCENT, margin = old?.margin ?: 0.0, publishedOfficial = old?.publishedOfficial.orEmpty(), publishedMarket = old?.publishedMarket.orEmpty())) }) { Text("ذخیره") } }, dismissButton = { TextButton(close) { Text("انصراف") } })
}

@Composable private fun Field(label: String, value: String, onChange: (String) -> Unit) = OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), singleLine = true, label = { Text(label) })

@Composable
private fun OfferDetails(offer: Offer, close: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(onDismissRequest = close, title = { Text(offer.name, fontWeight = FontWeight.ExtraBold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (offer.photo.isNotBlank()) PhotoView(offer.photo); if (offer.publishedOfficial.isNotBlank()) Text("قیمت رسمی: ${offer.publishedOfficial}", fontWeight = FontWeight.Bold); if (offer.publishedMarket.isNotBlank()) Text("قیمت بازار: ${offer.publishedMarket}", fontWeight = FontWeight.Bold); Text("مکان تحویل: ${offer.place}"); Text("زمان تحویل: ${offer.time}"); if (offer.description.isNotBlank()) Text(offer.description)
        Text("برای خرید یا هماهنگی با مدیریت:", fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button({ openExternal(context, waUri()) }, Modifier.weight(1f)) { Text("واتساپ") }; OutlinedButton({ openTelegram(context) }, Modifier.weight(1f)) { Text("تلگرام") } }
        OutlinedButton({ openExternal(context, Uri.parse("tel:$ADMIN_PHONE")) }, Modifier.fillMaxWidth()) { Text("تماس تلفنی با مدیریت") }
    } }, confirmButton = { TextButton(close) { Text("بستن") } })
}

private fun openTelegram(context: Context) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, tgUri())) }.onFailure { openExternal(context, Uri.parse("https://t.me/+989357236476")) } }
private fun openExternal(context: Context, uri: Uri) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } }

@Composable private fun PhotoView(value: String) {
    val context = LocalContext.current; val bitmap = remember(value) { runCatching { context.contentResolver.openInputStream(Uri.parse(value)).use { BitmapFactory.decodeStream(it) } }.getOrNull() }
    if (bitmap != null) Image(bitmap.asImageBitmap(), "عکس محصول", Modifier.fillMaxWidth().height(170.dp), contentScale = ContentScale.Crop)
}

@Composable private fun NewOfferPage(modifier: Modifier, phone: String, onStart: () -> Unit) { Column(modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("ثبت آگهی فروش", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Text(if (phone.isBlank()) "برای ثبت آگهی ابتدا وارد حساب شوید." else "آگهی شما ابتدا توسط مدیریت بررسی و سپس منتشر می‌شود."); Button(onStart, Modifier.fillMaxWidth()) { Text(if (phone.isBlank()) "ورود به حساب" else "ثبت آگهی جدید") } } }

@Composable
private fun AccountPage(modifier: Modifier, phone: String, home: () -> Unit, logout: () -> Unit, newOffer: () -> Unit, myOffers: () -> Unit, support: () -> Unit) {
    val prefs = LocalContext.current.getSharedPreferences("chemlink", Context.MODE_PRIVATE); val users = loadUsers(prefs); val account = users.firstOrNull { it.phone == phone }
    LazyColumn(modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("حساب من", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); if (phone.isBlank()) Text("برای استفاده از امکانات حساب وارد شوید.") else Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("شماره: $phone", fontWeight = FontWeight.Bold); Text("تأیید هویت: ✓ تأیید شده", color = Green, fontWeight = FontWeight.Bold); Text("نوع حساب: ${if (account?.type == "Supplier") "تأمین‌کننده" else "مصرف‌کننده"}"); account?.company?.takeIf { it.isNotBlank() }?.let { Text("شرکت: $it") } } } }
        if (phone.isBlank()) item { Button(home, Modifier.fillMaxWidth()) { Text("ورود / ثبت‌نام") } } else {
            item { AccountAction("✓", "تأیید هویت", "شماره موبایل شما با پیامک تأیید شده است") {} }
            item { AccountAction("▤", "آگهی‌های من", "مشاهده و مدیریت آگهی‌های ثبت‌شده", myOffers) }
            item { AccountAction("＋", "ثبت آگهی جدید", "ثبت پیشنهاد فروش مواد اولیه", newOffer) }
            item { AccountAction("☎", "پشتیبانی", "چت مستقیم با مدیریت در واتساپ", support) }
            item { OutlinedButton(logout, Modifier.fillMaxWidth()) { Text("خروج از حساب") } }
        }
        item { TextButton(home) { Text("بازگشت به بازار") } }
    }
}

@Composable private fun AccountAction(icon: String, title: String, subtitle: String, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { Text(icon, style = MaterialTheme.typography.headlineSmall, color = Gold); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall) }; Text("‹", style = MaterialTheme.typography.headlineSmall, color = Color.Gray) } } }

@Composable
private fun MyOffersPage(modifier: Modifier, offers: List<Offer>, phone: String, open: (Offer) -> Unit, requestCorrection: (Offer, String) -> Unit, edit: (Offer) -> Unit) {
    var correctionTarget by remember { mutableStateOf<Offer?>(null) }; var correctionText by remember { mutableStateOf("") }; val mine = offers.filter { it.owner == phone || (it.owner.isBlank() && it.phone == phone) }
    LazyColumn(modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("آگهی‌های من", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Text("وضعیت، قیمت و اصلاحیه آگهی‌های شما") }
        if (mine.isEmpty()) item { Text("هنوز آگهی‌ای ثبت نکرده‌اید.") }
        items(mine, key = { it.id }) { offer -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(offer.name, fontWeight = FontWeight.Bold); Text(when (offer.status) { Status.PENDING -> "در انتظار بررسی مدیریت"; Status.REJECTED -> "نیازمند اصلاح"; Status.APPROVED -> "منتشر شده" }, fontWeight = FontWeight.Bold, color = if (offer.status == Status.REJECTED) Red else Green)
            if (offer.status == Status.APPROVED) { Text("قیمت رسمی: ${offer.publishedOfficial}"); Text("قیمت بازار: ${offer.publishedMarket}") }; Text("تحویل: ${offer.place} • ${offer.time}"); if (offer.reason.isNotBlank()) Text("پیام مدیریت: ${offer.reason}", color = Red)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { OutlinedButton({ open(offer) }) { Text("مشاهده") }; if (offer.status != Status.APPROVED) OutlinedButton({ edit(offer) }) { Text("ویرایش") }; if (offer.status == Status.APPROVED) Button({ correctionText = ""; correctionTarget = offer }) { Text("درخواست اصلاح") } }
        } } }
    }
    correctionTarget?.let { offer -> AlertDialog(onDismissRequest = { correctionTarget = null }, title = { Text("درخواست اصلاح") }, text = { OutlinedTextField(correctionText, { correctionText = it }, Modifier.fillMaxWidth(), minLines = 3, label = { Text("توضیح اصلاح موردنظر") }) }, confirmButton = { Button(enabled = correctionText.isNotBlank(), onClick = { requestCorrection(offer, correctionText); correctionTarget = null }) { Text("ارسال") } }, dismissButton = { TextButton({ correctionTarget = null }) { Text("انصراف") } }) }
}

@Composable
private fun AdminLoginDialog(close: () -> Unit, success: () -> Unit) {
    val context = LocalContext.current; var step by remember { mutableIntStateOf(0) }; var password by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }; var expected by remember { mutableStateOf("") }; var message by remember { mutableStateOf("") }; var error by remember { mutableStateOf("") }; var sending by remember { mutableStateOf(false) }
    fun sendAdminOtp() {
        val otp = SecureRandom().nextInt(900000).plus(100000).toString()
        try {
            @Suppress("DEPRECATION") val manager = SmsManager.getDefault(); val sms = "ChemLink | کد ورود مدیریت: $otp\nاین کد را در اختیار دیگران قرار ندهید."; val parts = manager.divideMessage(sms)
            if (parts.size == 1) manager.sendTextMessage(ADMIN_PHONE, null, sms, null, null) else manager.sendMultipartTextMessage(ADMIN_PHONE, null, parts, null, null)
            expected = otp; step = 2; message = "کد ورود به شماره مدیریت ارسال شد."; error = ""
        } catch (_: Exception) { error = "ارسال پیامک ناموفق بود. مجوز SMS و سیم‌کارت را بررسی کنید." }
        sending = false
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) sendAdminOtp() else { sending = false; error = "مجوز ارسال پیامک داده نشد." } }
    AlertDialog(onDismissRequest = close, title = { Text(if (step == 0) "ورود مدیریت" else "تأیید دو مرحله‌ای مدیریت") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (step == 0) { Text("دسترسی مدیریت محافظت شده است."); OutlinedTextField(password, { password = digits(it).take(12) }, Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), label = { Text("رمز مدیریت") }) }
        else if (step == 1) Text("رمز صحیح است. برای مرحله دوم کد پیامکی را ارسال کنید.")
        else { Text("کد ۶ رقمی ارسال‌شده به شماره مدیریت را وارد کنید."); OutlinedTextField(code, { code = digits(it).filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("کد پیامکی") }) }
        if (message.isNotBlank()) Text(message, color = Green); if (error.isNotBlank()) Text(error, color = Red)
    } }, confirmButton = { Button(enabled = !sending, onClick = { when (step) { 0 -> if (hash(password) == ADMIN_HASH) { step = 1; error = "" } else error = "رمز مدیریت صحیح نیست."; 1 -> { sending = true; if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) sendAdminOtp() else permissionLauncher.launch(Manifest.permission.SEND_SMS) }; else -> if (code == expected && expected.isNotBlank()) success() else error = "کد پیامکی صحیح نیست." } }) { Text(when (step) { 0 -> "ادامه"; 1 -> "ارسال کد پیامکی"; else -> "تأیید و ورود" }) } }, dismissButton = { TextButton(close) { Text("انصراف") } })
}

private fun loadOffers(prefs: android.content.SharedPreferences): List<Offer> {
    val array = runCatching { JSONArray(prefs.getString("offers", "[]") ?: "[]") }.getOrDefault(JSONArray()); val result = mutableListOf<Offer>()
    for (i in 0 until array.length()) { val o = array.optJSONObject(i) ?: continue; result += Offer(id = o.optInt("id"), name = o.optString("name"), official = o.optString("official"), market = o.optString("market"), place = o.optString("place"), time = o.optString("time"), supplier = o.optString("supplier"), phone = o.optString("phone"), owner = o.optString("owner"), description = o.optString("description"), photo = o.optString("photo"), status = runCatching { Status.valueOf(o.optString("status", "PENDING")) }.getOrDefault(Status.PENDING), reason = o.optString("reason"), marginType = runCatching { MarginType.valueOf(o.optString("marginType", "PERCENT")) }.getOrDefault(MarginType.PERCENT), margin = o.optDouble("margin", 0.0), publishedOfficial = o.optString("publishedOfficial"), publishedMarket = o.optString("publishedMarket")) }
    return result
}

private fun persistOffers(prefs: android.content.SharedPreferences, offers: List<Offer>) {
    val array = JSONArray(); offers.forEach { o -> array.put(JSONObject().apply { put("id", o.id); put("name", o.name); put("official", o.official); put("market", o.market); put("place", o.place); put("time", o.time); put("supplier", o.supplier); put("phone", o.phone); put("owner", o.owner); put("description", o.description); put("photo", o.photo); put("status", o.status.name); put("reason", o.reason); put("marginType", o.marginType.name); put("margin", o.margin); put("publishedOfficial", o.publishedOfficial); put("publishedMarket", o.publishedMarket) }) }; prefs.edit().putString("offers", array.toString()).apply()
}

private fun loadUsers(prefs: android.content.SharedPreferences): List<UserAccount> {
    val array = runCatching { JSONArray(prefs.getString("users", "[]") ?: "[]") }.getOrDefault(JSONArray()); val result = mutableListOf<UserAccount>()
    for (i in 0 until array.length()) { val o = array.optJSONObject(i) ?: continue; result += UserAccount(o.optString("phone"), o.optString("type", "Consumer"), o.optString("name"), o.optString("company")) }
    return result
}
