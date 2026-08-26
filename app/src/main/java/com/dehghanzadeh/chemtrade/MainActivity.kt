package com.dehghanzadeh.chemtrade

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import java.security.MessageDigest

private enum class OfferStatus(val title: String) { Pending("در انتظار بررسی"), Approved("تأیید شده"), Rejected("نیاز به اصلاح") }
private enum class AccountType(val title: String) { Supplier("تأمین‌کننده"), Consumer("مصرف‌کننده") }

private data class UserProfile(val phone: String, val fullName: String, val email: String, val address: String, val company: String, val type: AccountType)
private data class ChemicalOffer(
    val id: Int,
    val name: String,
    val officialPrice: String,
    val marketPrice: String,
    val deliveryPlace: String,
    val deliveryTime: String,
    val supplier: String,
    val phone: String,
    val ownerPhone: String = "",
    val isChemLink: Boolean = false,
    val marginPercent: Int = 0,
    val status: OfferStatus = OfferStatus.Pending,
    val rejectionReason: String = "",
    val description: String = "",
    val photoNote: String = "",
    val hidePrice: Boolean = false
)

private val Navy = Color(0xFF0B1F33)
private val NavySoft = Color(0xFF16314D)
private val Gold = Color(0xFFC8A24A)
private val GoldSoft = Color(0xFFE8D39A)
private val Cream = Color(0xFFF4EBDD)
private val CreamLight = Color(0xFFFBF6EE)
private val Ink = Color(0xFF1D2630)
private val Red = Color(0xFFB3261E)
private val Green = Color(0xFF2E7D5B)
private const val ADMIN_HASH = "fda58e44e118473f2e49b6a49f3c83b1ba4a4627dc4abb2b74e90e6a844df374"
private const val DEFAULT_PHONE = "02100000000"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { ChemLinkApp() } }
}

@Composable
private fun ChemLinkApp() {
    val scheme = lightColorScheme(
        primary = Navy, onPrimary = Color.White,
        secondary = Gold, onSecondary = Navy,
        tertiary = Green,
        background = CreamLight, onBackground = Ink,
        surface = CreamLight, onSurface = Ink,
        surfaceVariant = Cream, primaryContainer = Color(0xFFDDE8F0), onPrimaryContainer = Navy
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { MaterialTheme(colorScheme = scheme) { ChemLinkHome() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChemLinkHome() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE) }
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var isAdmin by remember { mutableStateOf(false) }
    var showAdminLogin by remember { mutableStateOf(false) }
    var showRegistration by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var showOfferForm by remember { mutableStateOf(false) }
    var selectedOffer by remember { mutableStateOf<ChemicalOffer?>(null) }
    var editingOffer by remember { mutableStateOf<ChemicalOffer?>(null) }
    var currentUser by remember { mutableStateOf(loadUser(prefs)) }
    var supportPhone by remember { mutableStateOf(prefs.getString("supportPhone", DEFAULT_PHONE) ?: DEFAULT_PHONE) }
    var defaultMargin by remember { mutableIntStateOf(prefs.getInt("defaultMargin", 3)) }

    val offers = remember {
        mutableStateListOf(
            ChemicalOffer(1, "مونو اتانول آمین (MEA)", "۱۲۵,۰۰۰ تومان", "۱۲۸,۵۰۰ تومان", "تهران، شورآباد / حواله از درب پتروشیمی", "هماهنگی تلفنی", "بازرگانی دهقان‌زاده", DEFAULT_PHONE, isChemLink = true, marginPercent = 3, status = OfferStatus.Approved, description = "منو اتانول آمین در بشکه پلمپ ۲۰۰ کیلوگرمی، مناسب صنایع شیمیایی و تولید کود.", photoNote = "تصویر بشکه آبی محصول")
        )
    }
    fun saveOffer(offer: ChemicalOffer) {
        val index = offers.indexOfFirst { it.id == offer.id }
        if (index >= 0) offers[index] = offer else offers.add(offer.copy(id = (offers.maxOfOrNull { it.id } ?: 0) + 1))
    }
    val myOffers = offers.filter { currentUser != null && it.ownerPhone == currentUser!!.phone }

    Scaffold(
        containerColor = CreamLight,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamLight),
                title = { Column { Text("ChemLink", fontWeight = FontWeight.ExtraBold, color = Navy); Text("بازار حرفه‌ای مواد اولیه", style = MaterialTheme.typography.labelSmall, color = Gold) } },
                navigationIcon = { IconButton(onClick = { tab = 0 }) { Icon(Icons.Default.Hub, null, tint = Gold) } },
                actions = { IconButton(onClick = { tab = 3 }) { Icon(Icons.Default.Settings, null, tint = Navy) } }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Cream) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Store, null) }, label = { Text("بازار") })
                NavigationBarItem(selected = tab == 1, onClick = { if (currentUser == null) showRegistration = true else { editingOffer = null; showOfferForm = true } }, icon = { Icon(Icons.Default.AddCircle, null) }, label = { Text("ثبت آگهی") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("حساب من") })
            }
        }
    ) { padding ->
        when (tab) {
            0 -> MarketScreen(Modifier.padding(padding), offers, query, currentUser?.phone ?: "", { query = it }) { selectedOffer = it }
            1 -> AddOfferScreen(Modifier.padding(padding), currentUser != null) { if (currentUser == null) showRegistration = true else { editingOffer = null; showOfferForm = true } }
            2 -> AccountScreen(Modifier.padding(padding), currentUser, myOffers, supportPhone, { showRegistration = true }, { showLogin = true }, { selectedOffer = it }, { editingOffer = it; showOfferForm = true })
            3 -> SettingsScreen(Modifier.padding(padding), currentUser, supportPhone, { showRegistration = true }, { showLogin = true }, { currentUser = null; prefs.edit().clear().apply() }, { dial(context, supportPhone) }, { showAdminLogin = true })
            4 -> if (isAdmin) AdminScreen(Modifier.padding(padding), offers, supportPhone, defaultMargin, { supportPhone = it; prefs.edit().putString("supportPhone", it).apply() }, { defaultMargin = it; prefs.edit().putInt("defaultMargin", it).apply() }, { editingOffer = null; showOfferForm = true }, { tab = 0 }, ::saveOffer) else tab = 3
        }
    }

    selectedOffer?.let { offer -> OfferDialog(offer, supportPhone, currentUser?.phone == offer.ownerPhone, { selectedOffer = null }, { editingOffer = offer; selectedOffer = null; showOfferForm = true }) }
    if (showAdminLogin) AdminLoginDialog({ showAdminLogin = false }) { isAdmin = true; showAdminLogin = false; tab = 4 }
    if (showRegistration) RegistrationDialog({ showRegistration = false }) { user -> currentUser = user; saveUser(prefs, user); showRegistration = false; tab = 2 }
    if (showLogin) LoginDialog({ showLogin = false }) { user -> currentUser = user; saveUser(prefs, user); showLogin = false; tab = 2 }
    if (showOfferForm) OfferFormDialog(editingOffer, isAdmin, currentUser?.phone ?: "", supportPhone, defaultMargin, { showOfferForm = false }) { offer -> saveOffer(offer); showOfferForm = false; editingOffer = null; tab = if (isAdmin) 4 else 2 }
}

private fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
private fun saveUser(p: android.content.SharedPreferences, u: UserProfile) { p.edit().putString("phone",u.phone).putString("name",u.fullName).putString("email",u.email).putString("address",u.address).putString("company",u.company).putString("type",u.type.name).apply() }
private fun loadUser(p: android.content.SharedPreferences): UserProfile? { val phone=p.getString("phone",null)?:return null; return UserProfile(phone,p.getString("name","")?:"",p.getString("email","")?:"",p.getString("address","")?:"",p.getString("company","")?:"",runCatching{AccountType.valueOf(p.getString("type",AccountType.Supplier.name)!!)}.getOrDefault(AccountType.Supplier)) }

@Composable
private fun MarketScreen(modifier: Modifier, offers: List<ChemicalOffer>, query: String, userPhone: String, onQuery: (String)->Unit, onSelect: (ChemicalOffer)->Unit) {
    val ordered = offers.filter { it.status == OfferStatus.Approved && (it.name.contains(query,true) || it.supplier.contains(query,true)) }.sortedWith(compareByDescending<ChemicalOffer>{it.ownerPhone.isNotBlank() && it.ownerPhone==userPhone}.thenByDescending{it.isChemLink}.thenByDescending{it.id})
    LazyColumn(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { HeroCard(); Spacer(Modifier.height(12.dp)); OutlinedTextField(query,onQuery,Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},label={Text("جستجوی ماده یا فروشنده")}) }
        if (userPhone.isNotBlank() && ordered.any { it.ownerPhone==userPhone }) { item { SectionTitle("آگهی‌های من") }; items(ordered.filter{it.ownerPhone==userPhone}, key={it.id}) { OfferCard(it,onSelect,true) } }
        if (ordered.any { it.isChemLink && it.ownerPhone!=userPhone }) { item { SectionTitle("آگهی‌های مستقیم ChemLink") }; items(ordered.filter{it.isChemLink && it.ownerPhone!=userPhone}, key={it.id}) { OfferCard(it,onSelect,false) } }
        item { SectionTitle("سایر آگهی‌های تأییدشده") }
        items(ordered.filter{it.ownerPhone!=userPhone && !it.isChemLink}, key={it.id}) { OfferCard(it,onSelect,false) }
        if (ordered.isEmpty()) item { Text("آگهی تأییدشده‌ای پیدا نشد.") }
    }
}

@Composable private fun HeroCard() { Card(shape=RoundedCornerShape(28.dp),colors=CardDefaults.cardColors(containerColor=Navy)) { Column(Modifier.padding(20.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.WaterDrop,null,tint=Gold,modifier=Modifier.size(38.dp)); Spacer(Modifier.width(10.dp)); Column { Text("ChemLink",color=Color.White,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.headlineSmall); Text("پل مطمئن تجارت مواد شیمیایی",color=GoldSoft) } }; Spacer(Modifier.height(12.dp)); Text("قیمت رسمی و بازار، جزئیات تحویل و ارتباط مستقیم با فروشنده",color=Cream) } } }
@Composable private fun SectionTitle(text:String) { Text(text,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.ExtraBold,color=Navy) }

@Composable
private fun OfferCard(o: ChemicalOffer, onClick:(ChemicalOffer)->Unit, mine:Boolean) {
    Card(onClick={onClick(o)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=if(mine) Color(0xFFEAF0F3) else Color.White)) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.Top) { Column(Modifier.weight(1f)) { Text(o.name,fontWeight=FontWeight.ExtraBold,style=MaterialTheme.typography.titleMedium); Text(o.supplier,style=MaterialTheme.typography.bodySmall,color=NavySoft) }; AssistChip(onClick={onClick(o)},label={Text(if(mine)"آگهی من" else if(o.isChemLink)"ChemLink" else "تأییدشده")}) }
        if(o.photoNote.isNotBlank()) InfoLine(Icons.Default.Image,"عکس محصول ثبت شده")
        if(o.hidePrice) Text("قیمت پس از تماس اعلام می‌شود",color=Gold,fontWeight=FontWeight.Bold) else { if(o.officialPrice.isNotBlank()) Text("قیمت رسمی: ${o.officialPrice}",color=Green,fontWeight=FontWeight.Bold); if(o.marketPrice.isNotBlank()) Text("قیمت بازار: ${o.marketPrice}",color=Navy,fontWeight=FontWeight.Bold) }
        HorizontalDivider(); InfoLine(Icons.Default.LocationOn,"مکان تحویل: ${o.deliveryPlace}"); InfoLine(Icons.Default.Schedule,"زمان تحویل: ${o.deliveryTime}")
        Button(onClick={onClick(o)},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Navy)) { Icon(Icons.Default.Phone,null); Spacer(Modifier.width(6.dp)); Text("جزئیات و تماس") }
    } }
}
@Composable private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text:String) { Row(verticalAlignment=Alignment.CenterVertically) { Icon(icon,null,tint=Gold,modifier=Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(text,style=MaterialTheme.typography.bodySmall) } }

@Composable private fun AddOfferScreen(modifier:Modifier,logged:Boolean,onAdd:()->Unit) { Column(modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) { Icon(Icons.Default.Inventory2,null,tint=Gold,modifier=Modifier.size(54.dp)); Text("ثبت آگهی فروش",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Navy); Text(if(logged)"آگهی پس از ثبت برای تأیید مدیریت ارسال می‌شود. در صورت نیاز به اصلاح، پیام مدیریت را می‌بینید و همان آگهی را ویرایش و دوباره ارسال می‌کنید." else "برای ثبت آگهی ابتدا حساب تأمین‌کننده بسازید."); Button(onClick=onAdd,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Navy)){Text(if(logged)"ثبت آگهی جدید" else "ثبت‌نام تأمین‌کننده")} } }

@Composable
private fun AccountScreen(modifier:Modifier,user:UserProfile?,offers:List<ChemicalOffer>,phone:String,onRegister:()->Unit,onLogin:()->Unit,onSelect:(ChemicalOffer)->Unit,onEdit:(ChemicalOffer)->Unit) {
    LazyColumn(modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { item { Text("حساب کاربری",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Navy); if(user==null){ Text("برای ثبت آگهی و درخواست همکاری وارد حساب شوید."); Button(onClick=onRegister,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Navy)){Text("ثبت‌نام تأمین‌کننده / مصرف‌کننده")}; OutlinedButton(onClick=onLogin,modifier=Modifier.fillMaxWidth()){Text("ورود به حساب")} } else { Card(colors=CardDefaults.cardColors(containerColor=Cream),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp)){Text(user.fullName,fontWeight=FontWeight.ExtraBold); Text("${user.type.title} • ${user.company}"); Text(user.phone)}}; Spacer(Modifier.height(6.dp)); SectionTitle("آگهی‌های من") } }
        if(user!=null) items(offers,key={it.id}) { o -> Card(onClick={onSelect(o)}) { Column(Modifier.padding(14.dp)) { Text(o.name,fontWeight=FontWeight.Bold); Text("وضعیت: ${o.status.title}"); if(o.rejectionReason.isNotBlank()) Text("پیام مدیریت: ${o.rejectionReason}",color=Red); Row { TextButton(onClick={onEdit(o)}){Icon(Icons.Default.Edit,null); Text("اصلاح آگهی")}; TextButton(onClick={onSelect(o)}){Text("جزئیات")} } } } }
        if(user!=null) item { CallButton(phone,"تماس با ChemLink") }
    }
}

@Composable
private fun SettingsScreen(modifier:Modifier,user:UserProfile?,phone:String,onRegister:()->Unit,onLogin:()->Unit,onLogout:()->Unit,onCall:()->Unit,onAdmin:()->Unit) {
    var taps by remember { mutableIntStateOf(0) }
    LazyColumn(modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("تنظیمات",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold,color=Navy) }
        item { Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("تماس با ما",fontWeight=FontWeight.Bold); Text("برای استعلام قیمت، همکاری یا پشتیبانی با ChemLink تماس بگیرید."); Button(onClick=onCall,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Navy)){Icon(Icons.Default.Phone,null); Spacer(Modifier.width(6.dp)); Text("تماس با ما")}}} }
        item { Card(shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("حساب کاربری",fontWeight=FontWeight.Bold); if(user==null){Button(onClick=onRegister,modifier=Modifier.fillMaxWidth()){Text("ثبت‌نام")};OutlinedButton(onClick=onLogin,modifier=Modifier.fillMaxWidth()){Text("ورود")}}else{Text("وارد شده با ${user.phone}");TextButton(onClick=onLogout){Text("خروج از حساب",color=Red)}}}} }
        item { Text("نسخه 1.1.0",style=MaterialTheme.typography.labelSmall,color=NavySoft,modifier=Modifier.clickable{taps++;if(taps>=5){taps=0;onAdmin()}}) }
    }
}

@Composable
private fun AdminScreen(modifier:Modifier,offers:MutableList<ChemicalOffer>,phone:String,margin:Int,onPhone:(String)->Unit,onMargin:(Int)->Unit,onAdd:()->Unit,onBack:()->Unit,onSave:(ChemicalOffer)->Unit) {
    var phoneEdit by remember(phone){mutableStateOf(phone)}; var marginEdit by remember(margin){mutableStateOf(margin.toString())}; var rejectTarget by remember{mutableStateOf<ChemicalOffer?>(null)}
    LazyColumn(modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column{Text("داشبورد مدیریت",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=Navy);Text("کنترل انتشار، سود و نمایش قیمت")};IconButton(onClick=onBack){Icon(Icons.Default.Close,null)}} }
        item { Card(colors=CardDefaults.cardColors(containerColor=Cream),shape=RoundedCornerShape(20.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(phoneEdit,{phoneEdit=it},label={Text("شماره تماس ChemLink")},modifier=Modifier.fillMaxWidth());OutlinedTextField(marginEdit,{marginEdit=it.filter(Char::isDigit)},label={Text("سود پیش‌فرض درصدی")},modifier=Modifier.fillMaxWidth());Button(onClick={onPhone(phoneEdit);onMargin(marginEdit.toIntOrNull()?:0)},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Navy)){Text("ذخیره")};OutlinedButton(onClick=onAdd,modifier=Modifier.fillMaxWidth()){Text("ثبت آگهی مستقیم ChemLink")}}} }
        item { SectionTitle("درخواست‌های در انتظار تأیید") }
        items(offers.filter{it.status==OfferStatus.Pending},key={it.id}) { o -> Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text(o.name,fontWeight=FontWeight.Bold);Text("فروشنده: ${o.supplier}");Row{Button(onClick={onSave(o.copy(status=OfferStatus.Approved,rejectionReason=""))}){Text("تأیید")};OutlinedButton(onClick={rejectTarget=o}){Text("رد + پیام")};TextButton(onClick={onSave(o.copy(hidePrice=!o.hidePrice))}){Text(if(o.hidePrice)"نمایش قیمت" else "مخفی کردن قیمت")}}}} }
        item { SectionTitle("همه آگهی‌ها") }
        items(offers,key={it.id}) { o -> Card{Row(Modifier.padding(14.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(o.name,fontWeight=FontWeight.Bold);Text("${o.status.title} • سود ${o.marginPercent}%");if(o.hidePrice)Text("قیمت مخفی است",color=Gold)};IconButton(onClick={offers.remove(o)}){Icon(Icons.Default.Delete,null,tint=Red)}}} }
    }
    rejectTarget?.let{target->RejectionDialog({rejectTarget=null}){reason->onSave(target.copy(status=OfferStatus.Rejected,rejectionReason=reason));rejectTarget=null}}
}

@Composable
private fun OfferDialog(o:ChemicalOffer,fallbackPhone:String,canEdit:Boolean,onDismiss:()->Unit,onEdit:()->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={Text(o.name)},text={LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){item{if(o.photoNote.isNotBlank())InfoLine(Icons.Default.Image,"${o.photoNote}")};item{Text("فروشنده: ${o.supplier}")};if(!o.hidePrice){if(o.officialPrice.isNotBlank())item{Text("قیمت رسمی: ${o.officialPrice}")};if(o.marketPrice.isNotBlank())item{Text("قیمت بازار: ${o.marketPrice}")}}else item{Text("قیمت فقط پس از تماس اعلام می‌شود",color=Gold,fontWeight=FontWeight.Bold)};item{Text("مکان تحویل: ${o.deliveryPlace}")};item{Text("زمان تحویل: ${o.deliveryTime}")};if(o.description.isNotBlank())item{Text("توضیحات",fontWeight=FontWeight.Bold);Text(o.description)};if(o.rejectionReason.isNotBlank())item{Text("پیام مدیریت: ${o.rejectionReason}",color=Red,fontWeight=FontWeight.Bold)};item{CallButton(if(o.phone.isBlank())fallbackPhone else o.phone,"تماس مستقیم")}}},confirmButton={if(canEdit)Button(onClick=onEdit,colors=ButtonDefaults.buttonColors(containerColor=Navy)){Text(if(o.status==OfferStatus.Rejected)"اصلاح و ارسال مجدد" else "ویرایش آگهی")}else TextButton(onClick=onDismiss){Text("بستن")}},dismissButton={if(canEdit)TextButton(onClick=onDismiss){Text("بستن")}})
}
@Composable private fun CallButton(phone:String,title:String){val c=LocalContext.current;Button(onClick={dial(c,phone)},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Navy)){Icon(Icons.Default.Phone,null);Spacer(Modifier.width(8.dp));Text(title)}}
private fun dial(c:Context,phone:String){c.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:$phone")))}

@Composable private fun AdminLoginDialog(onDismiss:()->Unit,onSuccess:()->Unit){var password by remember{mutableStateOf("")};var error by remember{mutableStateOf(false)};AlertDialog(onDismissRequest=onDismiss,title={Text("ورود مدیریت")},text={Column{Text("دسترسی مدیریت محافظت شده است.");OutlinedTextField(password,{password=it;error=false},singleLine=true,visualTransformation=PasswordVisualTransformation(),label={Text("رمز مدیریت")},isError=error);if(error)Text("رمز صحیح نیست.",color=Red)}},confirmButton={Button(onClick={if(hash(password)==ADMIN_HASH)onSuccess()else error=true},colors=ButtonDefaults.buttonColors(containerColor=Navy)){Text("ورود")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})}

@Composable private fun RegistrationDialog(onDismiss:()->Unit,onSuccess:(UserProfile)->Unit){var type by remember{mutableStateOf(AccountType.Supplier)};var phone by remember{mutableStateOf("")};var name by remember{mutableStateOf("")};var email by remember{mutableStateOf("")};var address by remember{mutableStateOf("")};var company by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("ثبت‌نام در ChemLink")},text={LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.heightIn(max=430.dp)){item{Row{FilterChip(selected=type==AccountType.Supplier,onClick={type=AccountType.Supplier},label={Text("تأمین‌کننده")});Spacer(Modifier.width(8.dp));FilterChip(selected=type==AccountType.Consumer,onClick={type=AccountType.Consumer},label={Text("مصرف‌کننده")})}};item{OutlinedTextField(phone,{phone=it},label={Text("شماره موبایل")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(name,{name=it},label={Text("نام و نام خانوادگی")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(company,{company=it},label={Text("نام شرکت")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(email,{email=it},label={Text("ایمیل")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(address,{address=it},label={Text("آدرس کامل")},modifier=Modifier.fillMaxWidth())}}},confirmButton={Button(onClick={if(phone.length>=10&&name.isNotBlank()&&email.isNotBlank()&&address.isNotBlank())onSuccess(UserProfile(phone,name,email,address,company,type))}){Text("ثبت حساب")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})}
@Composable private fun LoginDialog(onDismiss:()->Unit,onSuccess:(UserProfile)->Unit){var phone by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("ورود به حساب")},text={Column{Text("شماره موبایل ثبت‌شده را وارد کنید.");OutlinedTextField(phone,{phone=it},label={Text("شماره موبایل")})}},confirmButton={Button(onClick={if(phone.isNotBlank())onSuccess(UserProfile(phone,"کاربر ChemLink","","","",AccountType.Supplier))}){Text("ورود")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})}
@Composable private fun RejectionDialog(onDismiss:()->Unit,onReject:(String)->Unit){var reason by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text("دلیل نیاز به اصلاح")},text={OutlinedTextField(reason,{reason=it},label={Text("پیام برای تأمین‌کننده")},modifier=Modifier.fillMaxWidth(),minLines=3)},confirmButton={Button(onClick={if(reason.isNotBlank())onReject(reason)}){Text("ارسال پیام")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})}

@Composable
private fun OfferFormDialog(initial:ChemicalOffer?,adminMode:Boolean,ownerPhone:String,supportPhone:String,defaultMargin:Int,onDismiss:()->Unit,onSubmit:(ChemicalOffer)->Unit){
    var name by remember{mutableStateOf(initial?.name?:"")};var official by remember{mutableStateOf(initial?.officialPrice?:"")};var market by remember{mutableStateOf(initial?.marketPrice?:"")};var place by remember{mutableStateOf(initial?.deliveryPlace?:"")};var time by remember{mutableStateOf(initial?.deliveryTime?:"")};var supplier by remember{mutableStateOf(initial?.supplier?:if(adminMode)"ChemLink" else "")};var phone by remember{mutableStateOf(initial?.phone?:if(adminMode)supportPhone else ownerPhone)};var margin by remember{mutableStateOf((initial?.marginPercent?:defaultMargin).toString())};var description by remember{mutableStateOf(initial?.description?:"")};var photoNote by remember{mutableStateOf(initial?.photoNote?:"")}
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(initial!=null)"ویرایش آگهی" else if(adminMode)"ثبت آگهی مستقیم ChemLink" else "ثبت آگهی جدید")},text={LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.heightIn(max=500.dp)){item{OutlinedTextField(name,{name=it},label={Text("نام ماده")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(official,{official=it},label={Text("قیمت رسمی")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(market,{market=it},label={Text("قیمت غیررسمی / بازار")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(place,{place=it},label={Text("مکان تحویل")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(time,{time=it},label={Text("زمان تحویل")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(supplier,{supplier=it},label={Text("نام فروشنده")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(phone,{phone=it},label={Text("شماره تماس")},modifier=Modifier.fillMaxWidth())};item{OutlinedTextField(description,{description=it},label={Text("توضیحات آگهی")},modifier=Modifier.fillMaxWidth(),minLines=3)};item{OutlinedTextField(photoNote,{photoNote=it},label={Text("توضیح عکس محصول / نام تصویر")},modifier=Modifier.fillMaxWidth())};if(adminMode)item{OutlinedTextField(margin,{margin=it.filter(Char::isDigit)},label={Text("سود این آگهی درصدی")},modifier=Modifier.fillMaxWidth())}}},confirmButton={Button(onClick={if(name.isNotBlank()&&place.isNotBlank()&&time.isNotBlank()){val status=if(adminMode)OfferStatus.Approved else OfferStatus.Pending;onSubmit(ChemicalOffer(initial?.id?:0,name,official,market,place,time,supplier.ifBlank{"فروشنده"},phone.ifBlank{supportPhone},ownerPhone=if(adminMode)"" else ownerPhone,isChemLink=adminMode||initial?.isChemLink==true,marginPercent=margin.toIntOrNull()?:0,status=status,rejectionReason="",description=description,photoNote=photoNote,hidePrice=initial?.hidePrice?:false))}}){Text(if(initial!=null)"ثبت اصلاحات" else "ثبت")}},dismissButton={TextButton(onClick=onDismiss){Text("انصراف")}})
}
