from pathlib import Path

p = Path("app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt")
s = p.read_text(encoding="utf-8")

# Scrollable dialogs/forms.
s = s.replace(
    "import androidx.compose.foundation.layout.*\n",
    "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\n",
    1,
)
s = s.replace(
    "val publishedMarket: String = \"\"\n)",
    "val publishedMarket: String = \"\",\n    val publishedAt: Long = 0L,\n    val expiresAt: Long = 0L\n)",
    1,
)

# Expiry-aware initial state: expired published ads return to management approval.
s = s.replace(
    'var offers by remember { mutableStateOf(loadOffers(prefs)) }',
    '''var offers by remember {
        val now = System.currentTimeMillis()
        mutableStateOf(loadOffers(prefs).map { offer ->
            if (offer.status == Status.APPROVED && offer.expiresAt > 0L && offer.expiresAt <= now) {
                offer.copy(status = Status.PENDING, reason = "زمان انتشار یک‌ماهه تمام شده؛ نیازمند تأیید مجدد قیمت و زمان انتشار")
            } else offer
        })
    }''',
    1,
)

# Market only shows currently active approved ads.
s = s.replace(
    'val visible = offers.filter { it.status == Status.APPROVED && (query.isBlank() || it.name.contains(query, true)) }',
    'val now = System.currentTimeMillis()\n    val visible = offers.filter { it.status == Status.APPROVED && (it.expiresAt == 0L || it.expiresAt > now) && (query.isBlank() || it.name.contains(query, true)) }',
    1,
)

# Make review and ad-entry bodies scroll vertically.
s = s.replace(
    'Column(Modifier.heightIn(max = 620.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {',
    'Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {',
    1,
)
s = s.replace(
    'Column(Modifier.heightIn(max = 650.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {',
    'Column(Modifier.heightIn(max = 650.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {',
    1,
)
s = s.replace(
    'AlertDialog(onDismissRequest = close, title = { Text(offer.name, fontWeight = FontWeight.ExtraBold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {',
    'AlertDialog(onDismissRequest = close, title = { Text(offer.name, fontWeight = FontWeight.ExtraBold) }, text = { Column(Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {',
    1,
)

# Approval: apply the entered margin independently to official and unofficial prices,
# then start a 30-day publication window for both.
s = s.replace(
    'publishedMarket = finalMarket?.let(::money).orEmpty(), reason = ""',
    'publishedMarket = finalMarket?.let(::money).orEmpty(), publishedAt = System.currentTimeMillis(), expiresAt = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L, reason = ""',
    1,
)

# Preserve expiry fields when editing/saving an ad.
s = s.replace(
    'publishedMarket = old?.publishedMarket.orEmpty()))',
    'publishedMarket = old?.publishedMarket.orEmpty(), publishedAt = old?.publishedAt ?: 0L, expiresAt = old?.expiresAt ?: 0L))',
    1,
)

# Persist publication timestamps.
s = s.replace(
    'put("publishedOfficial", o.publishedOfficial); put("publishedMarket", o.publishedMarket) })',
    'put("publishedOfficial", o.publishedOfficial); put("publishedMarket", o.publishedMarket); put("publishedAt", o.publishedAt); put("expiresAt", o.expiresAt) })',
    1,
)

# Load publication timestamps, defaulting old records to no-expiry for backward compatibility.
s = s.replace(
    'publishedOfficial = o.optString("publishedOfficial"), publishedMarket = o.optString("publishedMarket"))',
    'publishedOfficial = o.optString("publishedOfficial"), publishedMarket = o.optString("publishedMarket"), publishedAt = o.optLong("publishedAt", 0L), expiresAt = o.optLong("expiresAt", 0L))',
    1,
)

# Show remaining publication time in the user's own ads.
s = s.replace(
    'if (offer.status == Status.APPROVED) { Text("قیمت رسمی: ${offer.publishedOfficial}"); Text("قیمت بازار: ${offer.publishedMarket}") }; Text("تحویل: ${offer.place} • ${offer.time}")',
    '''if (offer.status == Status.APPROVED) {
                Text("قیمت رسمی: ${offer.publishedOfficial}")
                Text("قیمت غیررسمی: ${offer.publishedMarket}")
                if (offer.expiresAt > 0L) {
                    val daysLeft = ((offer.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L) / (24L * 60L * 60L * 1000L))
                    Text("اعتبار انتشار: ${daysLeft} روز باقی‌مانده", color = Gold, fontWeight = FontWeight.Bold)
                }
            }; Text("تحویل: ${offer.place} • ${offer.time}")''',
    1,
)

# Show expiry in management review list.
s = s.replace(
    'Text("تحویل: ${offer.place} | ${offer.time}")',
    'Text("تحویل: ${offer.place} | ${offer.time}")\n                if (offer.expiresAt > 0L && offer.status == Status.APPROVED) Text("اعتبار انتشار تا: ${java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.US).format(java.util.Date(offer.expiresAt))}", color = Gold)',
    1,
)

p.write_text(s, encoding="utf-8")
print("BUSINESS FEATURES PATCH OK")
