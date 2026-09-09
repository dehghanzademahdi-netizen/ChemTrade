from pathlib import Path

PKG = Path('app/src/main/java/com/dehghanzadeh/chemtrade')
M = PKG / 'MainActivity.kt'
E = PKG / 'EntryActivity.kt'

m = M.read_text(encoding='utf-8')
e = E.read_text(encoding='utf-8')

# Offer lifecycle timestamps: visible for 30 days, then retained for management history.
old_offer = '''    val publishedOfficial: String = "",\n    val publishedMarket: String = ""\n)'''
new_offer = '''    val publishedOfficial: String = "",\n    val publishedMarket: String = "",\n    val createdAt: Long = System.currentTimeMillis(),\n    val expiresAt: Long = createdAt + 30L * 24L * 60L * 60L * 1000L\n)'''
if old_offer in m and 'val expiresAt:' not in m:
    m = m.replace(old_offer, new_offer, 1)

# Cloud sync on startup; local cache remains available if the backend is not configured.
old = '''    var phone by remember { mutableStateOf(prefs.getString("phone", "").orEmpty()) }\n    var offers by remember { mutableStateOf(loadOffers(prefs)) }'''
new = '''    var phone by remember { mutableStateOf(prefs.getString("phone", "").orEmpty()) }\n    var offers by remember { mutableStateOf(loadOffers(prefs)) }\n    var cloudRefreshing by remember { mutableStateOf(false) }\n\n    LaunchedEffect(Unit) {\n        if (CloudStore.enabled()) {\n            cloudRefreshing = true\n            CloudStore.pull(prefs)\n            offers = loadOffers(prefs)\n            cloudRefreshing = false\n        }\n    }'''
if old in m and 'cloudRefreshing' not in m:
    m = m.replace(old, new, 1)

# Push every local offer change to the cloud.
old = '''        offers = list; persistOffers(prefs, list)\n    }'''
new = '''        offers = list; persistOffers(prefs, list)\n        if (CloudStore.enabled()) {\n            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { CloudStore.push(prefs) }\n        }\n    }'''
if old in m and 'CloudStore.push(prefs)' not in m:
    m = m.replace(old, new, 1)

# Market: only approved and unexpired ads are public.
old = '''    val visible = offers.filter { it.status == Status.APPROVED && (query.isBlank() || it.name.contains(query, true)) }'''
new = '''    val now = System.currentTimeMillis()\n    val visible = offers.filter { it.status == Status.APPROVED && it.expiresAt > now && (query.isBlank() || it.name.contains(query, true)) }'''
if old in m:
    m = m.replace(old, new, 1)

# Admin tabs: add expired archive.
old = '''    var tab by remember { mutableIntStateOf(0) }; var showUsers by remember { mutableStateOf(false) }'''
new = '''    var tab by remember { mutableIntStateOf(0) }; var showUsers by remember { mutableStateOf(false) }\n    val now = System.currentTimeMillis()'''
if old in m:
    m = m.replace(old, new, 1)
old = '''    val list = when (tab) { 0 -> offers.filter { it.status == Status.PENDING }; 1 -> offers.filter { it.status == Status.REJECTED }; else -> offers.filter { it.status == Status.APPROVED } }'''
new = '''    val list = when (tab) {\n        0 -> offers.filter { it.status == Status.PENDING }\n        1 -> offers.filter { it.status == Status.REJECTED }\n        2 -> offers.filter { it.status == Status.APPROVED && it.expiresAt > now }\n        else -> offers.filter { it.expiresAt <= now }\n    }'''
if old in m:
    m = m.replace(old, new, 1)
old = '''            Button({ tab = 2 }, Modifier.weight(1f)) { Text("منتشر ${offers.count { it.status == Status.APPROVED }}") }'''
new = '''            Button({ tab = 2 }, Modifier.weight(1f)) { Text("منتشر ${offers.count { it.status == Status.APPROVED && it.expiresAt > now }}") }\n            Button({ tab = 3 }, Modifier.weight(1f)) { Text("منقضی ${offers.count { it.expiresAt <= now }}") }'''
if old in m:
    m = m.replace(old, new, 1)

# Expiry details in admin cards.
old = '''                Text(offer.name, fontWeight = FontWeight.Bold); Text("قیمت رسمی تأمین‌کننده: ${offer.official}"); Text("قیمت بازار تأمین‌کننده: ${offer.market}"); Text("تحویل: ${offer.place} | ${offer.time}")'''
new = '''                Text(offer.name, fontWeight = FontWeight.Bold); Text("قیمت رسمی تأمین‌کننده: ${offer.official}"); Text("قیمت بازار تأمین‌کننده: ${offer.market}"); Text("تحویل: ${offer.place} | ${offer.time}")\n                if (offer.expiresAt <= System.currentTimeMillis()) Text("وضعیت: منقضی و بایگانی‌شده", color = Red, fontWeight = FontWeight.Bold)'''
if old in m:
    m = m.replace(old, new, 1)

# New offers get a fresh 30-day window; edits retain their original lifecycle.
old = '''publishedOfficial = old?.publishedOfficial.orEmpty(), publishedMarket = old?.publishedMarket.orEmpty()))'''
new = '''publishedOfficial = old?.publishedOfficial.orEmpty(), publishedMarket = old?.publishedMarket.orEmpty(), createdAt = old?.createdAt ?: System.currentTimeMillis(), expiresAt = old?.expiresAt ?: (System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L)))'''
if old in m:
    m = m.replace(old, new, 1)

# Persist lifecycle timestamps in JSON. The existing fields remain backward compatible.
old = '''publishedMarket = o.optString("publishedMarket")) }'''
new = '''publishedMarket = o.optString("publishedMarket"), createdAt = o.optLong("createdAt", System.currentTimeMillis()), expiresAt = o.optLong("expiresAt", System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L)) }'''
if old in m:
    m = m.replace(old, new, 1)
old = '''put("publishedOfficial", o.publishedOfficial); put("publishedMarket", o.publishedMarket) })'''
new = '''put("publishedOfficial", o.publishedOfficial); put("publishedMarket", o.publishedMarket); put("createdAt", o.createdAt); put("expiresAt", o.expiresAt) })'''
if old in m:
    m = m.replace(old, new, 1)

# Entry login: after SMS code validation, check the cloud copy so reinstalling the app
# on a new phone can recover the existing account by phone number.
if 'kotlinx.coroutines.runBlocking' not in e:
    e = e.replace('import org.json.JSONObject\n', 'import org.json.JSONObject\nimport kotlinx.coroutines.runBlocking\n', 1)
old = '''                        val existing = loadRegisteredUser(context, phone)\n                        if (existing != null && existing.name.isNotBlank() && existing.company.isNotBlank() && existing.address.isNotBlank()) {'''
new = '''                        val existing = loadRegisteredUser(context, phone)\n                        val cloudExisting = runBlocking { CloudStore.loadUser(phone) }\n                        if (cloudExisting != null) {\n                            val remote = EntryUser(phone = phone, type = cloudExisting.optString("type", existing?.type ?: accountType), name = cloudExisting.optString("name"), company = cloudExisting.optString("company"), address = cloudExisting.optString("address"), city = cloudExisting.optString("city"), landline = cloudExisting.optString("landline"))\n                            saveRegisteredUser(context, remote)\n                            if (remote.name.isNotBlank() && remote.company.isNotBlank() && remote.address.isNotBlank()) {\n                                finishLogin(remote.type)\n                            } else {\n                                profileName = remote.name; company = remote.company; address = remote.address; city = remote.city; landline = remote.landline; accountType = remote.type; step = 3; error = ""\n                            }\n                        } else if (existing != null && existing.name.isNotBlank() && existing.company.isNotBlank() && existing.address.isNotBlank()) {'''
if old in e:
    e = e.replace(old, new, 1)

# Every saved user is mirrored to the cloud.
old = '''prefs.edit().putString("users", users.toString()).apply()'''
new = '''prefs.edit().putString("users", users.toString()).commit()\n    if (CloudStore.enabled()) {\n        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { CloudStore.push(prefs) }\n    }'''
if old in e and 'CloudStore.push(prefs)' not in e:
    e = e.replace(old, new, 1)

# Restore cloud data before the launcher decides whether a local account exists.
old = '''val prefs = getSharedPreferences("chemlink", Context.MODE_PRIVATE)\n        PersistentBackup.restore(this, prefs)'''
new = '''val prefs = getSharedPreferences("chemlink", Context.MODE_PRIVATE)\n        PersistentBackup.restore(this, prefs)\n        if (CloudStore.enabled()) runBlocking { CloudStore.pull(prefs) }'''
if old in e:
    e = e.replace(old, new, 1)

M.write_text(m, encoding='utf-8')
E.write_text(e, encoding='utf-8')
print('CLOUD + EXPIRY PATCH OK')
