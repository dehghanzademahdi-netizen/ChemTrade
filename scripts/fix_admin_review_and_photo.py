from pathlib import Path
import re

path = Path("app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt")
s = path.read_text(encoding="utf-8")

# 1) Replace the fragile review dialog with a standalone Material dialog/card layout.
start = s.find('@Composable private fun ProfitApprovalDialog(')
end = s.find('@Composable private fun OfferDialog(', start)
if start < 0 or end < 0:
    raise SystemExit("ProfitApprovalDialog boundaries not found")

new_profit = r'''@Composable private fun ProfitApprovalDialog(o:ChemicalOffer,defaultMargin:Int,onDismiss:()->Unit,onApprove:(ChemicalOffer)->Unit){
    var type by remember(o.id){mutableStateOf(o.marginType)}
    var value by remember(o.id){mutableStateOf(if(o.marginValue>0)o.marginValue.toString() else defaultMargin.toString())}
    val base=parsePrice(o.marketPrice)?:parsePrice(o.officialPrice)
    val amount=normalizeDigits(value).replace(",", ".").toDoubleOrNull()?:0.0
    val final=when {
        base==null -> null
        type==MarginType.Percent -> (base+(base*amount/100.0)).toLong()
        else -> base+amount.toLong()
    }
    androidx.compose.ui.window.Dialog(onDismissRequest=onDismiss){
        Card(shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth().padding(12.dp)){
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text("بررسی و تأیید آگهی",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold,color=Navy)
                Text(o.name,fontWeight=FontWeight.Bold)
                Text("فروشنده: ${o.supplier}")
                if(o.photoNote.isNotBlank()) OfferPhotoPreview(o.photoNote)
                Text("قیمت رسمی: ${o.officialPrice.ifBlank{"ثبت نشده"}}")
                Text("قیمت بازار: ${o.marketPrice.ifBlank{"ثبت نشده"}}")
                Text("مکان تحویل: ${o.deliveryPlace}")
                Text("زمان تحویل: ${o.deliveryTime}")
                Text("نوع سود",fontWeight=FontWeight.Bold)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    FilterChip(selected=type==MarginType.Percent,onClick={type=MarginType.Percent},label={Text("درصد")})
                    FilterChip(selected=type==MarginType.Toman,onClick={type=MarginType.Toman},label={Text("تومان")})
                }
                OutlinedTextField(value=value,onValueChange={value=normalizeDigits(it).filter{c->c.isDigit()||c=='.'||c==','}},singleLine=true,label={Text(if(type==MarginType.Percent)"درصد سود"else"مبلغ سود به تومان")},modifier=Modifier.fillMaxWidth())
                if(final!=null) Text("قیمت نهایی: ${formatToman(final)}",fontWeight=FontWeight.ExtraBold,color=Green)
                else Text("قیمت پایه قابل محاسبه نیست؛ ابتدا قیمت آگهی را اصلاح کنید.",color=Red)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedButton(onClick=onDismiss,modifier=Modifier.weight(1f)){Text("بازگشت")}
                    Button(enabled=final!=null&&amount>=0,onClick={
                        if(final!=null){
                            onApprove(o.copy(status=OfferStatus.Approved,rejectionReason="",marginType=type,marginValue=amount,marginPercent=if(type==MarginType.Percent)amount.toInt()else 0,publishedPrice=formatToman(final)))
                        }
                    },modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Navy)){Text("تأیید و انتشار")}
                }
            }
        }
    }
}

'''
s = s[:start] + new_profit + s[end:]

# 2) Show the actual uploaded image in the admin pending card.
old = 'Text("زمان تحویل: ${o.deliveryTime}");Text("سود قبلی: ${o.marginPercent}%");Row{Button(onClick=onApprove){Text("بررسی سود و انتشار")}'
new = 'Text("زمان تحویل: ${o.deliveryTime}");if(o.photoNote.isNotBlank())OfferPhotoPreview(o.photoNote);Text("سود قبلی: ${o.marginPercent}%");Row{Button(onClick=onApprove){Text("باز کردن و بررسی")}'
if old not in s:
    raise SystemExit("AdminPendingCard target not found")
s = s.replace(old, new, 1)

# 3) Replace the old text-only photo field with a real gallery picker.
old_photo = 'item{OutlinedTextField(photoNote,{photoNote=it},label={Text("توضیح عکس محصول / نام تصویر")},modifier=Modifier.fillMaxWidth())}'
if old_photo not in s:
    raise SystemExit("Offer photo field target not found")
s = s.replace(old_photo, 'item{PhotoPickerField(photoNote){photoNote=it}}', 1)

# 4) Display the image in offer details too.
old_detail = 'text={LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){item{if(o.photoNote.isNotBlank())InfoLine(Icons.Default.Image,o.photoNote)};item{Text("فروشنده: ${o.supplier}")};'
new_detail = 'text={LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){item{if(o.photoNote.isNotBlank())OfferPhotoPreview(o.photoNote)};item{Text("فروشنده: ${o.supplier}")};'
if old_detail in s:
    s = s.replace(old_detail, new_detail, 1)

path.write_text(s, encoding="utf-8")
print("Admin review and customer photo upload patch applied")
