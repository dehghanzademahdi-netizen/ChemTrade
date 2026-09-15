from pathlib import Path

root = Path('app/src/main/java/com/dehghanzadeh/chemtrade')
e = root / 'EntryActivity.kt'
m = root / 'MainActivity.kt'

# EntryActivity: after SMS verification, query Firebase before deciding whether
# the user must complete the profile again. Anchor on the stable load call.
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
e.write_text(s, encoding='utf-8')

# MainActivity: insert cloud startup sync immediately before saveOffer(). This
# anchor survives the other UI/build patches that may alter the offers state line.
s = m.read_text(encoding='utf-8')
if 'CloudStore.pull(prefs)' not in s:
    marker = '    fun saveOffer(input: Offer) {'
    if marker not in s:
        marker = 'fun saveOffer(input: Offer) {'
    if marker not in s:
        raise SystemExit('MainActivity saveOffer marker not found')
    replacement = '''    LaunchedEffect(Unit) {
        CloudStore.pull(prefs)
        offers = loadOffers(prefs)
        CloudStore.push(prefs)
    }

'''
    s = s.replace(marker, replacement + marker, 1)
m.write_text(s, encoding='utf-8')

entry = e.read_text(encoding='utf-8')
main = m.read_text(encoding='utf-8')
assert 'CloudStore.loadUser' in entry, 'CloudStore.loadUser hook missing from EntryActivity'
assert 'CloudStore.pull(prefs)' in main, 'CloudStore.pull hook missing from MainActivity'
assert 'CloudStore.push(prefs)' in main, 'CloudStore.push hook missing from MainActivity'
print('CLOUD PERSISTENCE HOOKS VERIFIED')
