from pathlib import Path
p = Path('app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt')
s = p.read_text(encoding='utf-8')
s = s.replace('if (kind == OfferKind.SALE) { Field("قیمت رسمی تأمین‌کننده", official) { official = it }; Field("قیمت غیررسمی تأمین‌کننده", market) { market = it } } else { Spacer(Modifier.height(0.dp)) }', 'Field(if (kind == OfferKind.REQUEST) "قیمت رسمی (اختیاری)" else "قیمت رسمی تأمین‌کننده", official) { official = it }; Field(if (kind == OfferKind.REQUEST) "قیمت غیررسمی (اختیاری)" else "قیمت غیررسمی تأمین‌کننده", market) { market = it };', 1)
s = s.replace('if (kind == OfferKind.SALE) { Field("نام فروشنده (فقط مدیریت)", supplier) { supplier = it } } else { Spacer(Modifier.height(0.dp)) };', 'Field("نام فروشنده (فقط مدیریت)", supplier) { supplier = it };', 1)
p.write_text(s, encoding='utf-8')
print('FINAL FORM STABILITY PATCH OK')
