from pathlib import Path

root = Path('app/src/main/java/com/dehghanzadeh/chemtrade')
e = root / 'EntryActivity.kt'
m = root / 'MainActivity.kt'

# EntryActivity: after SMS verification, synchronously read the cloud account before
# deciding whether the user must fill the registration form again. Use fully-qualified
# coroutine names so this hook is independent of import formatting in generated sources.
s = e.read_text(encoding='utf-8')
old = 'val existing = loadRegisteredUser(context, phone); if (existing != null && existing.name.isNotBlank() && existing.company.isNotBlank() && existing.address.isNotBlank())'
new = '''val remote = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) { CloudStore.loadUser(context.getSharedPreferences("chemlink", Context.MODE_PRIVATE), phone) }
                    if (remote != null) {
                        saveRegisteredUser(context, EntryUser(phone, remote.optString("type", "Consumer"), remote.optString("name"), remote.optString("company"), remote.optString("address"), remote.optString("city"), remote.optString("landline")))
                    }
                    val existing = loadRegisteredUser(context, phone); if (existing != null && existing.name.isNotBlank() && existing.company.isNotBlank() && existing.address.isNotBlank())'''
if old in s and 'val remote = kotlinx.coroutines.runBlocking' not in s:
    s = s.replace(old, new, 1)
e.write_text(s, encoding='utf-8')

# MainActivity: pull cloud state on startup, then push local state. This makes the
# cloud the durable source while SharedPreferences remain only a local cache.
s = m.read_text(encoding='utf-8')
needle = 'var offers by remember { mutableStateOf(loadOffers(prefs)) }'
if 'CloudStore.pull(prefs)' not in s:
    s = s.replace(needle, needle + '''

    LaunchedEffect(Unit) {
        CloudStore.pull(prefs)
        offers = loadOffers(prefs)
        CloudStore.push(prefs)
    }''', 1)
elif 'CloudStore.push(prefs)' not in s:
    marker = 'offers = loadOffers(prefs)'
    s = s.replace(marker, marker + '\n        CloudStore.push(prefs)', 1)
m.write_text(s, encoding='utf-8')
print('CLOUD PERSISTENCE HOOKS OK')
