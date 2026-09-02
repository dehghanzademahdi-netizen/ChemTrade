package com.dehghanzadeh.chemtrade

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.BitmapFactory
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

private enum class Status { PENDING, APPROVED, REJECTED }
private enum class MarginType { PERCENT, TOMAN }
private data class Offer(
    val id:Int=0,
    val name:String="",
    val official:String="",
    val market:String="",
    val place:String="",
    val time:String="",
    val supplier:String="",
    val phone:String="",
    val owner:String="",
    val description:String="",
    val photo:String="",
    val status:Status=Status.PENDING,
    val reason:String="",
    val marginType:MarginType=MarginType.PERCENT,
    val margin:Double=0.0,
    val published:String=""
)

private val Navy=Color(0xFF0B1F33)
private val Gold=Color(0xFFC8A24A)
private val Cream=Color(0xFFFBF6EE)
private const val ADMIN_HASH="a1fb4e703a9ef1fa4936801721ff285a97ac85330856674412e054892afe6972"

private fun digits(s:String):String=s.map{c->when(c){in '۰'..'۹'->('0'.code+(c.code-'۰'.code)).toChar();in '٠'..'٩'->('0'.code+(c.code-'٠'.code)).toChar();else->c}}.joinToString("")
private fun hash(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){ "%02x".format(it) }
private fun num(s:String)=digits(s).filter(Char::isDigit).toLongOrNull()
private fun money(n:Long)=String.format("%,d تومان",n)

class MainActivity:ComponentActivity(){
    override fun onCreate(state:Bundle?){super.onCreate(state);setContent{ChemLinkApp()}}
}

@Composable private fun ChemLinkApp(){
    val colors=lightColorScheme(primary=Navy,onPrimary=Color.White,secondary=Gold,onSecondary=Navy,background=Cream,onBackground=Navy,surface=Cream,onSurface=Navy)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){MaterialTheme(colorScheme=colors){MainScreen()}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun MainScreen(){
    val ctx=LocalContext.current
    val prefs=remember{ctx.getSharedPreferences("chemlink",Context.MODE_PRIVATE)}
    var admin by remember{mutableStateOf(prefs.getBoolean("adminSession",false))}
    var page by remember{mutableIntStateOf(0)}
    var adminLogin by remember{mutableStateOf(false)}
    var form by remember{mutableStateOf(false)}
    var edit by remember{mutableStateOf<Offer?>(null)}
    var review by remember{mutableStateOf<Offer?>(null)}
    var details by remember{mutableStateOf<Offer?>(null)}
    var search by remember{mutableStateOf("")}
    var userPhone by remember{mutableStateOf(prefs.getString("phone","")?:"")}
    var offers by remember{mutableStateOf(loadOffers(prefs))}

    fun save(o:Offer){
        val list=offers.toMutableList();val index=list.indexOfFirst{it.id==o.id}
        if(index>=0) list[index]=o else list.add(o.copy(id=(list.maxOfOrNull{it.id}?:0)+1))
        offers=list;persistOffers(prefs,list)
    }
    fun logout(){userPhone="";prefs.edit().remove("phone").apply()}

    Scaffold(
        topBar={TopAppBar(title={Column{Text("ChemLink",fontWeight=FontWeight.ExtraBold);Text(if(admin)"پنل مدیریت" else "بازار مواد اولیه",style=MaterialTheme.typography.labelSmall,color=Gold)}},actions={
            TextButton(onClick={if(admin){admin=false;prefs.edit().putBoolean("adminSession",false).apply()}else page=3}){Text(if(admin)"خروج" else "تنظیمات")}
        })},
        bottomBar={if(!admin)NavigationBar{
            NavigationBarItem(selected=page==0,onClick={page=0},icon={Text("بازار")},label={Text("بازار")})
            NavigationBarItem(selected=page==1,onClick={page=1},icon={Text("+")},label={Text("ثبت آگهی")})
            NavigationBarItem(selected=page==2,onClick={page=2},icon={Text("من")},label={Text("حساب")})
            NavigationBarItem(selected=page==3,onClick={page=3},icon={Text("⚙")},label={Text("تنظیمات")})
        }}
    ){pad->
        when{
            admin->AdminDashboard(Modifier.padding(pad),offers,{review=it},{o->save(o)},{page=0})
            page==0->MarketPage(Modifier.padding(pad),offers,search,{search=it}){details=it}
            page==1->NewOfferPage(Modifier.padding(pad),userPhone){if(userPhone.isBlank())page=2 else{edit=null;form=true}}
            page==2->AccountPage(Modifier.padding(pad),userPhone,{page=0},{logout()},{edit=null;form=true})
            else->SettingsPage(Modifier.padding(pad)){adminLogin=true}
        }
    }

    details?.let{o->OfferDetails(o){details=null}}
    if(adminLogin)AdminLoginDialog({adminLogin=false}){admin=true;prefs.edit().putBoolean("adminSession",true).apply();adminLogin=false}
    if(form)OfferFormDialog(edit,userPhone,admin,{form=false;edit=null}){o->save(o);form=false;edit=null;page=if(admin)0 else 2}
    review?.let{o->ReviewDialog(o,{review=null},{approved->save(approved);review=null},{target,reason->save(target.copy(status=Status.REJECTED,reason=reason));review=null})}
}

@Composable private fun MarketPage(m:Modifier,offers:List<Offer>,q:String,setQ:(String)->Unit,open:(Offer)->Unit){
    val list=offers.filter{it.status==Status.APPROVED&&(q.isBlank()||it.name.contains(q,true)||it.supplier.contains(q,true))}
    LazyColumn(m.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        item{Card(colors=CardDefaults.cardColors(containerColor=Navy),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(20.dp)){Text("ChemLink",color=Color.White,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold);Text("بازار حرفه‌ای مواد اولیه",color=Gold);Text("قیمت رسمی و بازار • مکان و زمان تحویل",color=Color.White)}};Spacer(Modifier.height(12.dp));OutlinedTextField(q,setQ,Modifier.fillMaxWidth(),singleLine=true,label={Text("جستجوی ماده یا فروشنده")})}
        items(list,key={it.id}){o->Card(Modifier.fillMaxWidth().clickable{open(o)},shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(o.name,fontWeight=FontWeight.Bold);Text("فروشنده: ${o.supplier}");Text("قیمت رسمی: ${o.official}");Text("قیمت بازار: ${o.market}");Text("تحویل: ${o.place}");Text("زمان: ${o.time}");if(o.published.isNotBlank())Text("قیمت نهایی: ${o.published}",fontWeight=FontWeight.Bold,color=Gold)}}}
        if(list.isEmpty())item{Text("آگهی تأییدشده‌ای وجود ندارد.")}
    }
}

@Composable private fun AdminDashboard(m:Modifier,offers:List<Offer>,open:(Offer)->Unit,save:(Offer)->Unit,home:()->Unit){
    var tab by remember{mutableIntStateOf(0)}
    val list=when(tab){0->offers.filter{it.status==Status.PENDING};1->offers.filter{it.status==Status.REJECTED};else->offers.filter{it.status==Status.APPROVED}}
    Column(m.padding(16.dp)){Text("مدیریت آگهی‌ها",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.ExtraBold);Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){Button({tab=0},Modifier.weight(1f)){Text("در انتظار ${offers.count{it.status==Status.PENDING}}")};Button({tab=1},Modifier.weight(1f)){Text("اصلاحیه ${offers.count{it.status==Status.REJECTED}}")};Button({tab=2},Modifier.weight(1f)){Text("منتشر")}};Spacer(Modifier.height(8.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){items(list,key={it.id}){o->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){Text(o.name,fontWeight=FontWeight.Bold);Text("رسمی: ${o.official} | بازار: ${o.market}");Text("تحویل: ${o.place} | ${o.time}");if(o.reason.isNotBlank())Text("اصلاحیه: ${o.reason}",color=MaterialTheme.colorScheme.error);Button({open(o)}){Text("باز کردن و بررسی کامل")}}}};if(list.isEmpty())item{Text("موردی برای نمایش نیست.")}}}
}

@Composable private fun ReviewDialog(o:Offer,close:()->Unit,approve:(Offer)->Unit,reject:(Offer,String)->Unit){
 var type by remember(o.id){mutableStateOf(o.marginType)};var value by remember(o.id){mutableStateOf(if(o.margin==0.0)"" else o.margin.toString())};var reason by remember(o.id){mutableStateOf("")}
 val base=num(o.market)?:num(o.official)?:0L;val margin=value.toDoubleOrNull()?:0.0;val final=if(type==MarginType.PERCENT)base+(base*margin/100).toLong() else base+margin.toLong()
 AlertDialog(onDismissRequest=close,title={Text("بررسی آگهی")},text={Column(Modifier.heightIn(max=560.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text(o.name,fontWeight=FontWeight.Bold);if(o.photo.isNotBlank())PhotoView(o.photo);Text(o.description);Text("قیمت رسمی: ${o.official}");Text("قیمت بازار: ${o.market}");Text("مکان تحویل: ${o.place}");Text("زمان تحویل: ${o.time}");Text("سود مدیر:");Row{FilterChip(selected=type==MarginType.PERCENT,onClick={type=MarginType.PERCENT},label={Text("درصد")});Spacer(Modifier.width(8.dp));FilterChip(selected=type==MarginType.TOMAN,onClick={type=MarginType.TOMAN},label={Text("تومان")})};OutlinedTextField(value=value,onValueChange={value=it.filter{c->c.isDigit()||c=='.'}},singleLine=true,label={Text("مقدار سود")});Text("قیمت قابل انتشار: ${money(final)}",fontWeight=FontWeight.Bold,color=Gold);OutlinedTextField(value=reason,onValueChange={reason=it},label={Text("دلیل اصلاحیه")})}},confirmButton={Button({approve(o.copy(status=Status.APPROVED,marginType=type,margin=margin,published=money(final)))}){Text("تأیید و انتشار")}},dismissButton={Row{TextButton(close){Text("بازگشت")};TextButton({reject(o,reason.ifBlank{"لطفاً اطلاعات آگهی اصلاح شود"})}){Text("ثبت اصلاحیه")}}})
}

@Composable private fun OfferFormDialog(old:Offer?,owner:String,admin:Boolean,close:()->Unit,submit:(Offer)->Unit){
 var name by remember{mutableStateOf(old?.name?:"")};var official by remember{mutableStateOf(old?.official?:"")};var market by remember{mutableStateOf(old?.market?:"")};var place by remember{mutableStateOf(old?.place?:"")};var time by remember{mutableStateOf(old?.time?:"")};var supplier by remember{mutableStateOf(old?.supplier?:"")};var phoneV by remember{mutableStateOf(old?.phone?:owner)};var description by remember{mutableStateOf(old?.description?:"")};var photo by remember{mutableStateOf(old?.photo?:"")};val context=LocalContext.current
 val picker=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri:Uri?->uri?.let{runCatching{context.contentResolver.takePersistableUriPermission(it,Intent.FLAG_GRANT_READ_URI_PERMISSION)};photo=it.toString()}}
 AlertDialog(onDismissRequest=close,title={Text(if(old==null)"ثبت آگهی" else "ویرایش آگهی")},text={Column(Modifier.heightIn(max=620.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Input("نام ماده",name){name=it};Input("قیمت رسمی",official){official=it};Input("قیمت بازار / غیررسمی",market){market=it};Input("مکان تحویل",place){place=it};Input("زمان تحویل",time){time=it};Input("نام فروشنده",supplier){supplier=it};Input("شماره تماس",phoneV){phoneV=digits(it).filter(Char::isDigit).take(11)};Input("توضیحات",description){description=it};Button({picker.launch(arrayOf("image/*"))},Modifier.fillMaxWidth()){Text(if(photo.isBlank())"آپلود عکس محصول مشتری" else "تغییر عکس")};if(photo.isNotBlank())PhotoView(photo)}},confirmButton={Button({if(name.isNotBlank())submit(Offer(old?.id?:0,name,official,market,place,time,supplier,phoneV,owner,description,photo,old?.status?:if(admin)Status.APPROVED else Status.PENDING,old?.reason?:"",old?.marginType?:MarginType.PERCENT,old?.margin?:0.0,old?.published?:""))}){Text("ذخیره")}},dismissButton={TextButton(close){Text("انصراف")}})
}
@Composable private fun Input(label:String,value:String,onChange:(String)->Unit)=OutlinedTextField(value,onChange,Modifier.fillMaxWidth(),singleLine=true,label={Text(label)})

@Composable private fun OfferDetails(o:Offer,close:()->Unit){AlertDialog(onDismissRequest=close,title={Text(o.name)},text={Column(verticalArrangement=Arrangement.spacedBy(5.dp)){if(o.photo.isNotBlank())PhotoView(o.photo);Text("قیمت رسمی: ${o.official}");Text("قیمت بازار: ${o.market}");Text("تحویل: ${o.place}");Text("زمان: ${o.time}");Text("فروشنده: ${o.supplier}");Text("تماس: ${o.phone}");Text(o.description)}},confirmButton={TextButton(close){Text("بستن")}})}
@Composable private fun PhotoView(value:String){val c=LocalContext.current;val bitmap=remember(value){runCatching{c.contentResolver.openInputStream(Uri.parse(value)).use{BitmapFactory.decodeStream(it)}}.getOrNull()};bitmap?.let{Image(it.asImageBitmap(),"عکس محصول",Modifier.fillMaxWidth().heightIn(max=220.dp),contentScale=ContentScale.Fit)}}

@Composable private fun AdminLoginDialog(close:()->Unit,success:()->Unit){var code by remember{mutableStateOf("")};var error by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("ورود مدیریت")},text={Column{Text("کد مدیریت را وارد کنید.");OutlinedTextField(code,{code=digits(it).filter(Char::isDigit).take(20)},visualTransformation=PasswordVisualTransformation(),singleLine=true,label={Text("کد مدیریت")});if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)}},confirmButton={Button({if(hash(code)==ADMIN_HASH)success()else error="کد مدیریت صحیح نیست."}){Text("ورود")}},dismissButton={TextButton(close){Text("انصراف")}})}

@Composable private fun NewOfferPage(m:Modifier,phone:String,start:()->Unit){Column(m.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text("ثبت آگهی فروش",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold);Text("قیمت رسمی و غیررسمی، مکان، زمان تحویل و عکس محصول را ثبت کنید.",textAlign=TextAlign.Center);Spacer(Modifier.height(18.dp));Button(start){Text(if(phone.isBlank())"ابتدا ورود" else "شروع ثبت آگهی")}}}
@Composable private fun AccountPage(m:Modifier,phone:String,home:()->Unit,logout:()->Unit,add:()->Unit){Column(m.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("حساب من",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);if(phone.isBlank()){Text("برای ثبت آگهی وارد شوید.");Button(home){Text("ورود")}}else{Text("شماره: $phone");Button(add){Text("ثبت آگهی جدید")};OutlinedButton(logout){Text("خروج")}}}}
@Composable private fun SettingsPage(m:Modifier,admin:()->Unit){Column(m.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("تنظیمات",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Button(admin,Modifier.fillMaxWidth()){Text("ورود به پنل مدیریت")};Text("مدیریت آگهی‌ها: بررسی، اصلاحیه، انتخاب سود درصدی یا تومانی و انتشار")}}

private fun persistOffers(p:android.content.SharedPreferences,list:List<Offer>){val a=JSONArray();list.forEach{o->a.put(JSONObject().apply{put("id",o.id);put("name",o.name);put("official",o.official);put("market",o.market);put("place",o.place);put("time",o.time);put("supplier",o.supplier);put("phone",o.phone);put("owner",o.owner);put("description",o.description);put("photo",o.photo);put("status",o.status.name);put("reason",o.reason);put("marginType",o.marginType.name);put("margin",o.margin);put("published",o.published)})};p.edit().putString("offers",a.toString()).apply()}
private fun loadOffers(p:android.content.SharedPreferences):List<Offer>{val raw=p.getString("offers",null)?:return emptyList();return runCatching{val a=JSONArray(raw);(0 until a.length()).map{val j=a.getJSONObject(it);Offer(j.optInt("id"),j.optString("name"),j.optString("official"),j.optString("market"),j.optString("place"),j.optString("time"),j.optString("supplier"),j.optString("phone"),j.optString("owner"),j.optString("description"),j.optString("photo"),runCatching{Status.valueOf(j.optString("status"))}.getOrDefault(Status.PENDING),j.optString("reason"),runCatching{MarginType.valueOf(j.optString("marginType"))}.getOrDefault(MarginType.PERCENT),j.optDouble("margin"),j.optString("published"))}}.getOrDefault(emptyList())}
