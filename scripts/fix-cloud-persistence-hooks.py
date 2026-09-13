from pathlib import Path

root = Path('app/src/main/java/com/dehghanzadeh/chemtrade')
e = root / 'EntryActivity.kt'
m = root / 'MainActivity.kt'

# EntryActivity: after SMS verification, synchronously read the cloud account before
# deciding whether the user must fill the registration form again.
s = e.read_text(encoding='utf-8')
if 'import kotlinx.coroutines.Dispatchers' not in s:
    s = s.replace('import androidx.core.content.ContextCompat\n', 'import androidx.core.content.ContextCompat\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.runBlocking\n', 1)
if 'val prefs = context.getSharedPreferences("chemlink", Context.MODE_PRIVATE)' not in s:
    s = s.replace('val context = LocalContext.current; var step', 'val context = LocalContext.current; val prefs = context.getSharedPreferences("chemlink", Context.MODE_PRIVATE); var step', 1)
old = 'val existing = loadRegisteredUser(context, phone); if (existing != null && existing.name.isNotBlank() && existing.company.isNotBlank() && existing.address.isNotBlank())'
new = '''val remote = runBlocking(Dispatchers.IO) { CloudStore.loadUser(prefs, phone) }
                    if (remote != null) {
                        saveRegisteredUser(context, EntryUser(phone, remote.optString("type", "Consumer"), remote.optString("name"), remote.optString("company"), remote.optString("address"), remote.optString("city"), remote.optString("landline")))
                    }
                    val existing = loadRegisteredUser(context, phone); if (existing != null && existing.name.isNotBlank() && existing.company.isNotBlank() && existing.address.isNotBlank())'''
if old in s and 'val remote = runBlocking(Dispatchers.IO)' not in s:
    s = s.replace(old, new, 1)
e.write_text(s, encoding='utf-8')

# MainActivity: always pull cloud state on startup, then push local state so a newly
# registered account reaches the cloud before the user leaves the app.
s = m.read_text(encoding='utf-8')
needle = 'var offers by remember { mutableStateOf(loadOffers(prefs)) }'
if 'CloudStore.pull(prefs)' not in s:
    repl = needle + '''

    LaunchedEffect(Unit) {
        CloudStore.pull(prefs)
        offers = loadOffers(prefs)
        CloudStore.push(prefs)
    }'''
    s = s.replace(needle, repl, 1)
else:
    # If an earlier pull exists, ensure the startup push also exists.
    marker = 'offers = loadOffers(prefs)'
    if marker in s and 'CloudStore.push(prefs)' not in s:
        s = s.replace(marker, marker + '\n            CloudStore.push(prefs)', 1)
m.write_text(s, encoding='utf-8')
print('CLOUD PERSISTENCE HOOKS OK')
