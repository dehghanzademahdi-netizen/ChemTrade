from pathlib import Path

root = Path('app/src/main/java/com/dehghanzadeh/chemtrade')
e = root / 'EntryActivity.kt'
m = root / 'MainActivity.kt'

# Always resolve an existing account from Firebase after SMS verification.
s = e.read_text(encoding='utf-8')
if 'CloudStore.loadUser' not in s:
    marker = 'val existing = loadRegisteredUser(context, phone)'
    if marker not in s:
        raise SystemExit('EntryActivity login marker not found')
    replacement = '''val remote = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                            CloudStore.loadUser(context.getSharedPreferences("chemlink", Context.MODE_PRIVATE), phone)
                        }
                        if (remote != null) {
                            saveRegisteredUser(context, EntryUser(phone, remote.optString("type", "Consumer"), remote.optString("name"), remote.optString("company"), remote.optString("address"), remote.optString("city"), remote.optString("landline")))
                        }
                        val existing = loadRegisteredUser(context, phone)'''
    s = s.replace(marker, replacement, 1)

# If local session data survived an update, refresh that account from Firebase first.
if 'CloudStore.refresh' not in s:
    marker = 'if (prefs.getString("phone", "").orEmpty().isNotBlank()) { startActivity(Intent(this, MainActivity::class.java)); finish(); return }'
    if marker in s:
        replacement = '''if (prefs.getString("phone", "").orEmpty().isNotBlank()) {
            val localPhone = prefs.getString("phone", "").orEmpty()
            val remote = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                CloudStore.loadUser(prefs, localPhone)
            }
            if (remote != null) {
                saveRegisteredUser(this, EntryUser(localPhone, remote.optString("type", "Consumer"), remote.optString("name"), remote.optString("company"), remote.optString("address"), remote.optString("city"), remote.optString("landline")))
                prefs.edit().putString("type", remote.optString("type", "Consumer")).apply()
            }
            startActivity(Intent(this, MainActivity::class.java)); finish(); return
        }'''
        s = s.replace(marker, replacement, 1)
e.write_text(s, encoding='utf-8')

# Pull remote state before showing offers. Only push after a successful pull so a
# temporary network failure can never overwrite good cloud data with an empty cache.
s = m.read_text(encoding='utf-8')
if 'CloudStore.pull(prefs)' not in s:
    marker = '    Scaffold(\n'
    if marker not in s:
        marker = 'Scaffold(\n'
    if marker not in s:
        raise SystemExit('MainActivity Scaffold marker not found')
    replacement = '''    LaunchedEffect(Unit) {
        val pulled = CloudStore.pull(prefs)
        if (pulled) {
            offers = loadOffers(prefs)
            CloudStore.push(prefs)
        }
    }

'''
    s = s.replace(marker, replacement + marker, 1)
m.write_text(s, encoding='utf-8')

entry = e.read_text(encoding='utf-8')
main = m.read_text(encoding='utf-8')
assert 'CloudStore.loadUser' in entry, 'CloudStore.loadUser hook missing from EntryActivity'
assert 'CloudStore.pull(prefs)' in main, 'CloudStore.pull hook missing from MainActivity'
assert 'CloudStore.push(prefs)' in main, 'CloudStore.push hook missing from MainActivity'
print('CLOUD ACCOUNT RESTORE + SAFE SYNC HOOKS VERIFIED')
