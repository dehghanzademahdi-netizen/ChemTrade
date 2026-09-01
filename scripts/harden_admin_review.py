from pathlib import Path

path = Path("app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt")
s = path.read_text(encoding="utf-8")

# 1) Re-open the admin dashboard after a process restart when an admin session is still valid.
s = s.replace(
    'var tab by remember { mutableIntStateOf(0) }',
    'var tab by remember { mutableIntStateOf(if (prefs.getBoolean("adminSession", false)) 4 else 0) }'
)

# 2) Keep a dedicated review target instead of trying to reuse the public offer dialog.
s = s.replace(
    'var selectedOffer by remember { mutableStateOf<ChemicalOffer?>(null) }\n    var editingOffer',
    'var selectedOffer by remember { mutableStateOf<ChemicalOffer?>(null) }\n    var adminReviewTarget by remember { mutableStateOf<ChemicalOffer?>(null) }\n    var editingOffer'
)

# 3) Do not wipe the entire database/preferences when management logs out.
s = s.replace(
    '{currentUser=null;prefs.edit().clear().apply();isAdmin=false}',
    '{currentUser=null;prefs.edit().remove("phone").remove("name").remove("email").remove("address").remove("company").remove("type").remove("adminSession").apply();isAdmin=false}'
)

# 4) Give AdminScreen an explicit review callback.
s = s.replace(
    '::saveOffer,::deleteOffer,{isAdmin=false;prefs.edit().putBoolean("adminSession",false).apply();tab=3})',
    '::saveOffer,::deleteOffer,{adminReviewTarget=it},{isAdmin=false;prefs.edit().putBoolean("adminSession",false).apply();tab=3})'
)

# 5) Render the review dialog outside AdminScreen. It uses a simple Column rather than a
# nested LazyColumn inside AlertDialog, which avoids the crash observed on some devices.
s = s.replace(
    'selectedOffer?.let{offer->OfferDialog(offer,supportPhone,currentUser?.phone==offer.ownerPhone,{selectedOffer=null},{editingOffer=offer;selectedOffer=null;showOfferForm=true})}\n',
    'selectedOffer?.let{offer->OfferDialog(offer,supportPhone,currentUser?.phone==offer.ownerPhone,{selectedOffer=null},{editingOffer=offer;selectedOffer=null;showOfferForm=true})}\n    adminReviewTarget?.let{offer->AdminReviewDialog(offer,defaultMargin,{adminReviewTarget=null}){approved->saveOffer(approved);adminReviewTarget=null}}\n'
)

old_sig = """@Composable private fun AdminScreen(modifier:Modifier,offers:MutableList<ChemicalOffer>,phone:String,margin:Int,onPhone:(String)->Unit,onMargin:(Int)->Unit,onAdd:()->Unit,onBack:()->Unit,onSave:(ChemicalOffer)->Unit,onDelete:(ChemicalOffer)->Unit,onLogout:()->Unit)"""
new_sig = """@Composable private fun AdminScreen(modifier:Modifier,offers:MutableList<ChemicalOffer>,phone:String,margin:Int,onPhone:(String)->Unit,onMargin:(Int)->Unit,onAdd:()->Unit,onBack:()->Unit,onSave:(ChemicalOffer)->Unit,onDelete:(ChemicalOffer)->Unit,onReview:(ChemicalOffer)->Unit,onLogout:()->Unit)"""
s = s.replace(old_sig, new_sig)

# Pending cards: open the safe review screen first; do not directly launch the old dialog.
s = s.replace(
    'AdminPendingCard(o,{profitTarget=o},{rejectTarget=o},{onSave(o.copy(hidePrice=!o.hidePrice))})',
    'AdminPendingCard(o,{onReview(o)},{rejectTarget=o},{onSave(o.copy(hidePrice=!o.hidePrice))})'
)

# Rejected cards should also reopen the same review screen.
s = s.replace(
    'items(offers.filter{it.status==OfferStatus.Rejected},key={it.id}){o->Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text(o.name,fontWeight=FontWeight.Bold);Text("فروشنده: ${o.supplier}");Text("دلیل/پیام مدیریت: ${o.rejectionReason.ifBlank{"نیاز به اصلاح"}}",color=Red);Text("این آگهی حذف نشده و در سیستم نگهداری می‌شود.",color=Gold);Row{Button(onClick={profitTarget=o}){Text("بررسی مجدد")};TextButton(onClick={onDelete(o)}){Text("حذف کامل",color=Red)}}}}}',
    'items(offers.filter{it.status==OfferStatus.Rejected},key={it.id}){o->Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Text(o.name,fontWeight=FontWeight.Bold);Text("فروشنده: ${o.supplier}");Text("دلیل/پیام مدیریت: ${o.rejectionReason.ifBlank{"نیاز به اصلاح"}}",color=Red);Text("این آگهی حذف نشده و در سیستم نگهداری می‌شود.",color=Gold);Row{Button(onClick={onReview(o)}){Text("بررسی مجدد")};TextButton(onClick={onDelete(o)}){Text("حذف کامل",color=Red)}}}}}'
)

# All-offer rows become explicitly reviewable instead of only having a delete icon.
s = s.replace(
    'items(offers,key={it.id}){o->Card{Row(Modifier.padding(14.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(o.name,fontWeight=FontWeight.Bold);Text("${o.status.title} • سود ${if(o.marginType==MarginType.Percent)"${o.marginValue}%" else "${o.marginValue.toLong()} تومان"}");if(o.publishedPrice.isNotBlank())Text("قیمت نهایی: ${o.publishedPrice}",fontWeight=FontWeight.Bold);Text("${o.deliveryPlace} • ${o.deliveryTime}",style=MaterialTheme.typography.bodySmall)};IconButton(onClick={onDelete(o)}){Icon(Icons.Default.Delete,null,tint=Red)}}}}}',
    'items(offers,key={it.id}){o->Card(onClick={onReview(o)}){Row(Modifier.padding(14.dp).fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(o.name,fontWeight=FontWeight.Bold);Text("${o.status.title} • سود ${if(o.marginType==MarginType.Percent)"${o.marginValue}%" else "${o.marginValue.toLong()} تومان"}");if(o.publishedPrice.isNotBlank())Text("قیمت نهایی: ${o.publishedPrice}",fontWeight=FontWeight.Bold);Text("${o.deliveryPlace} • ${o.deliveryTime}",style=MaterialTheme.typography.bodySmall)};IconButton(onClick={onDelete(o)}){Icon(Icons.Default.Delete,null,tint=Red)}}}}}'
)

# Remove the old profit dialog invocation from AdminScreen. The new review dialog handles it.
s = s.replace(
    ';profitTarget?.let{target->ProfitApprovalDialog(target,defaultMargin=margin,{profitTarget=null}){approved->onSave(approved);profitTarget=null}}}',
    '}'
)

marker = '@Composable private fun AdminPendingCard('
if marker not in s:
    raise SystemExit("AdminPendingCard marker not found")

review = r'''@Composable private fun AdminReviewDialog(o:ChemicalOffer,defaultMargin:Int,onDismiss:()->Unit,onApprove:(ChemicalOffer)->Unit){
    var type by remember(o.id){mutableStateOf(o.marginType)}
    var value by remember(o.id){mutableStateOf(if(o.marginValue>0)o.marginValue.toString() else defaultMargin.toString())}
    var showReject by remember(o.id){mutableStateOf(false)}
    val base=parsePrice(o.marketPrice)?:parsePrice(o.officialPrice)
    val amount=value.replace(',','.').toDoubleOrNull()?:0.0
    val final=if(base==null)null else if(type==MarginType.Percent)(base+(base*amount/100.0)).toLong()else(base+amount.toLong())
    if(showReject){
        RejectionDialog({showReject=false}){reason->onApprove(o.copy(status=OfferStatus.Rejected,rejectionReason=reason));showReject=false}
        return
    }
    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("بررسی آگهی")},
        text={Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
            Text(o.name,fontWeight=FontWeight.ExtraBold,color=Navy)
            Text("فروشنده: ${o.supplier}")
            if(o.description.isNotBlank())Text(o.description)
            if(o.photoNote.isNotBlank())InfoLine(Icons.Default.Image,o.photoNote)
            Text("قیمت رسمی: ${o.officialPrice.ifBlank{"ثبت نشده"}}")
            Text("قیمت بازار: ${o.marketPrice.ifBlank{"ثبت نشده"}}")
            Text("مکان تحویل: ${o.deliveryPlace}")
            Text("زمان تحویل: ${o.deliveryTime}")
            Text("انتخاب سود",fontWeight=FontWeight.Bold)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(selected=type==MarginType.Percent,onClick={type=MarginType.Percent},label={Text("درصد")})
                FilterChip(selected=type==MarginType.Toman,onClick={type=MarginType.Toman},label={Text("تومان")})
            }
            OutlinedTextField(value=value,onValueChange={value=normalizeDigits(it).filter{c->c.isDigit()||c=='.'||c==','}},singleLine=true,label={Text(if(type==MarginType.Percent)"درصد سود"else"مبلغ سود به تومان")},modifier=Modifier.fillMaxWidth())
            if(final!=null)Text("قیمت نهایی انتشار: ${formatToman(final)}",fontWeight=FontWeight.ExtraBold,color=Green)
            else Text("قیمت پایه عددی نیست؛ آگهی را اصلاح کنید.",color=Red)
        }},
        confirmButton={Button(enabled=final!=null&&amount>=0,onClick={if(final!=null)onApprove(o.copy(status=OfferStatus.Approved,rejectionReason="",marginType=type,marginValue=amount,marginPercent=if(type==MarginType.Percent)amount.toInt()else 0,publishedPrice=formatToman(final)))},colors=ButtonDefaults.buttonColors(containerColor=Navy)){Text("اعمال سود و انتشار")}},
        dismissButton={Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){TextButton(onClick={showReject=true}){Text("رد + اصلاحیه",color=Red)};TextButton(onClick=onDismiss){Text("بستن")}}}
    )
}

'''
s = s.replace(marker, review + marker, 1)

path.write_text(s, encoding="utf-8")
print("Admin review hardening applied")
