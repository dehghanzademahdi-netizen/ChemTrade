from pathlib import Path

p = Path('app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt')
s = p.read_text(encoding='utf-8')

# Distinguish sale ads from supply requests.
s = s.replace('private enum class MarginType { PERCENT, TOMAN }', 'private enum class MarginType { PERCENT, TOMAN }\nprivate enum class OfferKind { SALE, REQUEST }', 1)
s = s.replace('val id: Int = 0,\n    val name:', 'val id: Int = 0,\n    val kind: OfferKind = OfferKind.SALE,\n    val name:', 1)

# Restore before state is read. The previous LaunchedEffect restored too late, so offers could load as empty.
s = s.replace('val prefs = remember { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE) }\n    LaunchedEffect(Unit) { PersistentBackup.restore(context, prefs) }', 'val prefs = remember { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE) }\n    remember { PersistentBackup.restore(context, prefs) }', 1)

# Remove duplicate bottom navigation item for My Offers; it remains under Account.
s = s.replace('            NavigationBarItem(page == 4, { page = 4 }, { Text("▤") }, label = { Text("آگهی‌های من") })\n', '', 1)

# Subtle watermark/logo backdrop: light, not dark.
s = s.replace('LazyColumn(modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {', '''Box(modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.ic_chemtrade_logo), "", Modifier.align(Alignment.Center).size(310.dp), alpha = 0.045f)
        LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {''', 1)
s = s.replace('        if (visible.isEmpty()) item { Text("آگهی تأییدشده‌ای برای نمایش وجود ندارد.") }\n    }\n}', '        if (visible.isEmpty()) item { Text("آگهی تأییدشده‌ای برای نمایش وجود ندارد.") }\n    }\n    }\n}', 1)

# Market: hide seller identity and show the ad type.
s = s.replace('Text("آگهی‌های تأییدشده", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)', 'Text("آگهی‌های تأییدشده", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)\n            Text("قیمت رسمی و قیمت غیررسمی در صورت ثبت نمایش داده می‌شود.", color = Blue, style = MaterialTheme.typography.bodySmall)', 1)
s = s.replace('Text(offer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold); Text("تأیید شده", color = Green, style = MaterialTheme.typography.labelMedium)', 'Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n                            Text(if (offer.kind == OfferKind.REQUEST) "درخواست تأمین" else "فروش", color = Gold, fontWeight = FontWeight.Bold)\n                            Text(offer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)\n                        }; Text("تأیید شده", color = Green, style = MaterialTheme.typography.labelMedium)', 1)
s = s.replace('Text("مشاهده جزئیات و خرید ←", color = Blue, fontWeight = FontWeight.Bold)', 'Text(if (offer.kind == OfferKind.REQUEST) "برای اعلام قیمت به مدیریت پیام دهید ←" else "مشاهده جزئیات و خرید ←", color = Blue, fontWeight = FontWeight.Bold)', 1)

# New ad page: two choices.
s = s.replace('private fun NewOfferPage(modifier: Modifier, phone: String, onStart: () -> Unit) { Column(modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("ثبت آگهی فروش", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold); Text(if (phone.isBlank()) "برای ثبت آگهی ابتدا وارد حساب شوید." else "آگهی شما ابتدا توسط مدیریت بررسی و سپس منتشر می‌شود."); Button(onStart, Modifier.fillMaxWidth()) { Text(if (phone.isBlank()) "ورود به حساب" else "ثبت آگهی جدید") } } }', '''private fun NewOfferPage(modifier: Modifier, phone: String, onStart: () -> Unit) {
    Column(modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("ثبت آگهی", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(if (phone.isBlank()) "برای ثبت آگهی ابتدا وارد حساب شوید." else "نوع آگهی را انتخاب کنید؛ همه آگهی‌ها قبل از انتشار توسط مدیریت بررسی می‌شوند.")
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🛒 آگهی فروش", fontWeight = FontWeight.Bold)
            Text("برای عرضه یک ماده و اعلام قیمت فروش")
            Button(onStart, Modifier.fillMaxWidth()) { Text(if (phone.isBlank()) "ورود به حساب" else "ثبت آگهی فروش") }
        } }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔎 درخواست تأمین", fontWeight = FontWeight.Bold)
            Text("اگر دنبال ماده‌ای هستید، درخواست خود را ثبت کنید تا تأمین‌کنندگان پیشنهاد قیمت بدهند؛ اعلام قیمت از مسیر مدیریت انجام می‌شود.")
            Button(onStart, Modifier.fillMaxWidth()) { Text(if (phone.isBlank()) "ورود به حساب" else "ثبت درخواست تأمین") }
        } }
    }
}''', 1)

# Form: select sale/request and preserve it when editing.
s = s.replace('var name by remember { mutableStateOf(old?.name.orEmpty()) };', 'var kind by remember { mutableStateOf(old?.kind ?: OfferKind.SALE) }; var name by remember { mutableStateOf(old?.name.orEmpty()) };', 1)
s = s.replace('Field("نام ماده", name) { name = it };', 'Text("نوع آگهی", fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(kind == OfferKind.SALE, { kind = OfferKind.SALE }, label = { Text("آگهی فروش") }, modifier = Modifier.weight(1f)); FilterChip(kind == OfferKind.REQUEST, { kind = OfferKind.REQUEST }, label = { Text("درخواست تأمین") }, modifier = Modifier.weight(1f)) }; Field(if (kind == OfferKind.REQUEST) "ماده موردنیاز" else "نام ماده", name) { name = it };', 1)
s = s.replace('Field("قیمت رسمی تأمین‌کننده", official) { official = it }; Field("قیمت غیررسمی تأمین‌کننده", market) { market = it };', 'if (kind == OfferKind.SALE) { Field("قیمت رسمی تأمین‌کننده", official) { official = it }; Field("قیمت غیررسمی تأمین‌کننده", market) { market = it } }', 1)
s = s.replace('Field("نام فروشنده (فقط مدیریت)", supplier) { supplier = it };', 'if (kind == OfferKind.SALE) Field("نام فروشنده (فقط مدیریت)", supplier) { supplier = it };', 1)
s = s.replace('submit(Offer(id = old?.id ?: 0, name = name,', 'submit(Offer(id = old?.id ?: 0, kind = kind, name = name,', 1)

# Management review labels request ads and permits approval without a sale price.
s = s.replace('Text(offer.name, fontWeight = FontWeight.ExtraBold);', 'Text(if (offer.kind == OfferKind.REQUEST) "درخواست تأمین: ${offer.name}" else offer.name, fontWeight = FontWeight.ExtraBold);', 1)
s = s.replace('Text("قیمت رسمی تأمین‌کننده: ${offer.official}"); Text("قیمت بازار تأمین‌کننده: ${offer.market}"); Text("تحویل: ${offer.place} | ${offer.time}")', 'if (offer.kind == OfferKind.SALE) { Text("قیمت رسمی تأمین‌کننده: ${offer.official}"); Text("قیمت غیررسمی تأمین‌کننده: ${offer.market}") } else { Text("درخواست قیمت از تأمین‌کنندگان: فعال") }; Text("تحویل: ${offer.place} | ${offer.time}")', 1)
s = s.replace('Text(offer.name, fontWeight = FontWeight.ExtraBold); if (offer.photo.isNotBlank()) PhotoView(offer.photo); Text("قیمت رسمی تأمین‌کننده: ${offer.official.ifBlank { "ثبت نشده" }}"); Text("قیمت غیررسمی تأمین‌کننده: ${offer.market.ifBlank { "ثبت نشده" }}");', 'Text(if (offer.kind == OfferKind.REQUEST) "درخواست تأمین: ${offer.name}" else offer.name, fontWeight = FontWeight.ExtraBold); if (offer.photo.isNotBlank()) PhotoView(offer.photo); if (offer.kind == OfferKind.SALE) { Text("قیمت رسمی تأمین‌کننده: ${offer.official.ifBlank { "ثبت نشده" }}"); Text("قیمت غیررسمی تأمین‌کننده: ${offer.market.ifBlank { "ثبت نشده" }}") } else Text("این درخواست برای دریافت پیشنهاد قیمت است.")', 1)
s = s.replace('confirmButton = { Button(enabled = finalOfficial != null || finalMarket != null, onClick = { approve(offer.copy(status = Status.APPROVED,', 'confirmButton = { Button(enabled = offer.kind == OfferKind.REQUEST || finalOfficial != null || finalMarket != null, onClick = { approve(offer.copy(status = Status.APPROVED,', 1)

# Details: request ads route price offers through management contact.
s = s.replace('if (offer.publishedOfficial.isNotBlank()) Text("قیمت رسمی: ${offer.publishedOfficial}", fontWeight = FontWeight.Bold); if (offer.publishedMarket.isNotBlank()) Text("قیمت بازار: ${offer.publishedMarket}", fontWeight = FontWeight.Bold);', 'if (offer.kind == OfferKind.SALE) { if (offer.publishedOfficial.isNotBlank()) Text("قیمت رسمی: ${offer.publishedOfficial}", fontWeight = FontWeight.Bold); if (offer.publishedMarket.isNotBlank()) Text("قیمت غیررسمی: ${offer.publishedMarket}", fontWeight = FontWeight.Bold) } else Text("برای این ماده، درخواست تأمین ثبت شده است. تأمین‌کنندگان می‌توانند پیشنهاد خود را از طریق مدیریت اعلام کنند.", fontWeight = FontWeight.Bold);', 1)
s = s.replace('Text("برای خرید یا هماهنگی با مدیریت:", fontWeight = FontWeight.Bold)', 'Text(if (offer.kind == OfferKind.REQUEST) "برای اعلام قیمت یا هماهنگی با مدیریت:" else "برای خرید یا هماهنگی با مدیریت:", fontWeight = FontWeight.Bold)', 1)

# Persist/load the ad type.
s = s.replace('Offer(id = o.optInt("id"), name = o.optString("name"),', 'Offer(id = o.optInt("id"), kind = runCatching { OfferKind.valueOf(o.optString("kind", "SALE")) }.getOrDefault(OfferKind.SALE), name = o.optString("name"),', 1)
s = s.replace('put("id", o.id); put("name", o.name);', 'put("id", o.id); put("kind", o.kind.name); put("name", o.name);', 1)

# Brighter premium cyan/silver canvas while retaining contrast.
s = s.replace('val scheme = lightColorScheme(primary = Navy, onPrimary = Color.White, secondary = Gold, onSecondary = Navy, background = Cream, onBackground = Navy, surface = Color.White, onSurface = Navy)', 'val scheme = lightColorScheme(primary = Blue, onPrimary = Color.White, secondary = Gold, onSecondary = Navy, background = Color(0xFFEAF7FC), onBackground = Navy, surface = Color.White, onSurface = Navy)', 1)

p.write_text(s, encoding='utf-8')
print('FINAL PRODUCT POLISH PATCH OK')
