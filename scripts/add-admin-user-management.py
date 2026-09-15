from pathlib import Path

root = Path('app/src/main/java/com/dehghanzadeh/chemtrade')

# CloudStore: expose the complete online customer list to the admin panel.
c = root / 'CloudStore.kt'
s = c.read_text(encoding='utf-8')
if 'suspend fun loadUsers(prefs: SharedPreferences): List<JSONObject>' not in s:
    marker = '    suspend fun loadUser(prefs: SharedPreferences, phone: String): JSONObject? {'
    if marker not in s:
        raise SystemExit('CloudStore loadUser marker not found')
    fn = '''    suspend fun loadUsers(prefs: SharedPreferences): List<JSONObject> {
        val response = request("GET", "$ROOT/$USERS_BY_PHONE")
        if (response.code in 200..299) {
            val obj = response.body?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (obj != null) {
                val out = mutableListOf<JSONObject>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    obj.optJSONObject(key)?.let { out.add(it) }
                }
                return out.sortedWith(compareBy({ it.optString("type") }, { it.optString("name") }, { it.optString("phone") }))
            }
        }
        val local = array(prefs.getString(USERS, "[]"))
        val out = mutableListOf<JSONObject>()
        for (i in 0 until local.length()) local.optJSONObject(i)?.let(out::add)
        return out.sortedWith(compareBy({ it.optString("type") }, { it.optString("name") }, { it.optString("phone") }))
    }

'''
    s = s.replace(marker, fn + marker, 1)
c.write_text(s, encoding='utf-8')

# MainActivity: richer user model, online refresh, and admin actions.
m = root / 'MainActivity.kt'
s = m.read_text(encoding='utf-8')
s = s.replace(
    'private data class UserAccount(val phone: String, val type: String, val name: String = "", val company: String = "")',
    'private data class UserAccount(val phone: String, val type: String, val name: String = "", val company: String = "", val address: String = "", val city: String = "", val landline: String = "")',
    1
)
old = '''    var tab by remember { mutableIntStateOf(0) }; var showUsers by remember { mutableStateOf(false) }\n    val prefs = LocalContext.current.getSharedPreferences("chemlink", Context.MODE_PRIVATE); val users = loadUsers(prefs)'''
new = '''    var tab by remember { mutableIntStateOf(0) }; var showUsers by remember { mutableStateOf(false) }\n    val context = LocalContext.current\n    val prefs = context.getSharedPreferences("chemlink", Context.MODE_PRIVATE)\n    var users by remember { mutableStateOf(loadUsers(prefs)) }\n    LaunchedEffect(showUsers) {\n        if (showUsers) {\n            val remote = CloudStore.loadUsers(prefs)\n            users = remote.map { u ->\n                UserAccount(\n                    phone = u.optString("phone"), type = u.optString("type"), name = u.optString("name"),\n                    company = u.optString("company"), address = u.optString("address"),\n                    city = u.optString("city"), landline = u.optString("landline")\n                )\n            }\n        }\n    }'''
if old not in s:
    raise SystemExit('AdminDashboard state marker not found')
s = s.replace(old, new, 1)
old_block = '''@Composable private fun UserCounts(users: List<UserAccount>) {\n    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("کاربران سامانه", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("تأمین‌کنندگان: ${users.count { it.type == "Supplier" }} نفر"); Text("مصرف‌کنندگان: ${users.count { it.type == "Consumer" }} نفر"); Text("مجموع: ${users.size} نفر", fontWeight = FontWeight.Bold) } }\n}'''
new_block = '''@Composable\nprivate fun UserCounts(users: List<UserAccount>) {\n    val context = LocalContext.current\n    var selectedType by remember { mutableStateOf("همه") }\n    val filtered = users.filter { selectedType == "همه" || (selectedType == "تأمین‌کننده" && it.type == "Supplier") || (selectedType == "مصرف‌کننده" && it.type == "Consumer") }\n    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {\n        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {\n            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {\n                Text("مدیریت کاربران", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)\n                Text("تأمین‌کنندگان: ${users.count { it.type == "Supplier" }} نفر")\n                Text("مصرف‌کنندگان: ${users.count { it.type == "Consumer" }} نفر")\n                Text("مجموع: ${users.size} نفر", fontWeight = FontWeight.Bold)\n                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {\n                    listOf("همه", "تأمین‌کننده", "مصرف‌کننده").forEach { type ->\n                        OutlinedButton({ selectedType = type }, Modifier.weight(1f)) { Text(type) }\n                    }\n                }\n            }\n        }\n        if (filtered.isEmpty()) {\n            Text("هنوز کاربری در این بخش ثبت نشده است.")\n        } else {\n            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {\n                items(filtered, key = { it.phone }) { user ->\n                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {\n                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {\n                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {\n                                Text(user.name.ifBlank { "بدون نام" }, fontWeight = FontWeight.ExtraBold)\n                                Text(if (user.type == "Supplier") "تأمین‌کننده" else "مصرف‌کننده", color = Gold, fontWeight = FontWeight.Bold)\n                            }\n                            if (user.company.isNotBlank()) Text("شرکت: ${user.company}")\n                            Text("موبایل: ${user.phone}")\n                            if (user.landline.isNotBlank()) Text("تلفن ثابت: ${user.landline}")\n                            if (user.city.isNotBlank()) Text("شهر: ${user.city}")\n                            if (user.address.isNotBlank()) Text("آدرس: ${user.address}")\n                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n                                Button({ openExternal(context, Uri.parse("https://wa.me/${digits(user.phone).removePrefix("0").let { "98$it" }}")) }, Modifier.weight(1f)) { Text("واتس‌اپ") }\n                                OutlinedButton({ context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))) }, Modifier.weight(1f)) { Text("تماس") }\n                            }\n                        }\n                    }\n                }\n            }\n        }\n    }\n}'''
if old_block not in s:
    raise SystemExit('UserCounts block not found')
s = s.replace(old_block, new_block, 1)
m.write_text(s, encoding='utf-8')

main = m.read_text(encoding='utf-8')
cloud = c.read_text(encoding='utf-8')
assert 'CloudStore.loadUsers(prefs)' in main
assert 'مدیریت کاربران' in main
assert 'suspend fun loadUsers(prefs: SharedPreferences): List<JSONObject>' in cloud
print('ADMIN USER MANAGEMENT HOOKS VERIFIED')
