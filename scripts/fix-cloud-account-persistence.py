from pathlib import Path

ROOT = Path('app/src/main/java/com/dehghanzadeh/chemtrade')
M = ROOT / 'MainActivity.kt'
E = ROOT / 'EntryActivity.kt'

# EntryActivity: after OTP verification, fetch the account from Firebase before deciding
# whether profile data is already complete. After first registration, upload the account.
e = E.read_text(encoding='utf-8')
if 'import kotlinx.coroutines.launch' not in e:
    e = e.replace('import androidx.core.content.ContextCompat\n', 'import androidx.core.content.ContextCompat\nimport kotlinx.coroutines.launch\n')
if 'val cloudScope = rememberCoroutineScope()' not in e:
    e = e.replace('val context = LocalContext.current; var step', 'val context = LocalContext.current; val cloudScope = rememberCoroutineScope(); var step', 1)

old = 'val existing = loadRegisteredUser(context, phone); if (existing != null && existing.name.isNotBlank() && existing.company.isNotBlank() && existing.address.isNotBlank()) finishLogin(existing.type) else {'
new = '''cloudScope.launch {
                            if (CloudStore.enabled()) {
                                val remote = CloudStore.loadUser(prefs, phone)
                                if (remote != null) {
                                    val merged = EntryUser(phone, remote.optString("type", "Consumer"), remote.optString("name"), remote.optString("company"), remote.optString("address"), remote.optString("city"), remote.optString("landline"))
                                    saveRegisteredUser(context, merged)
                                } else {
                                    CloudStore.pull(prefs)
                                }
                            }
                            val existing = loadRegisteredUser(context, phone)
                            if (existing != null && existing.name.isNotBlank() && existing.company.isNotBlank() && existing.address.isNotBlank()) finishLogin(existing.type) else {'''
if old in e and 'val remote = CloudStore.loadUser' not in e:
    e = e.replace(old, new, 1)
    # Close the coroutine lambda at the end of the existing else branch.
    marker = 'step = 3; error = "" } } else error = "کد واردشده صحیح نیست."'
    e = e.replace(marker, 'step = 3; error = "" } } else error = "کد واردشده صحیح نیست."', 1)

old_save = 'saveRegisteredUser(context, EntryUser(phone, accountType, profileName.trim(), company.trim(), address.trim(), city.trim(), landline)); error = ""; finishLogin(accountType)'
new_save = '''val user = EntryUser(phone, accountType, profileName.trim(), company.trim(), address.trim(), city.trim(), landline)
                        saveRegisteredUser(context, user)
                        cloudScope.launch {
                            if (CloudStore.enabled()) CloudStore.saveUser(prefs, JSONObject().apply {
                                put("phone", user.phone); put("type", user.type); put("name", user.name); put("company", user.company)
                                put("address", user.address); put("city", user.city); put("landline", user.landline)
                            })
                        }
                        error = ""; finishLogin(accountType)'''
if old_save in e and 'CloudStore.saveUser(prefs' not in e:
    e = e.replace(old_save, new_save, 1)

E.write_text(e, encoding='utf-8')

# MainActivity: load cloud state on every app start, and push account/offer changes.
m = M.read_text(encoding='utf-8')
if 'LaunchedEffect(Unit) {' not in m or 'CloudStore.pull(prefs)' not in m:
    needle = 'var offers by remember { mutableStateOf(loadOffers(prefs)) }'
    repl = needle + '''

    LaunchedEffect(Unit) {
        if (CloudStore.enabled()) {
            CloudStore.pull(prefs)
            offers = loadOffers(prefs)
        }
    }'''
    m = m.replace(needle, repl, 1)

needle = 'offers = list; persistOffers(prefs, list)'
if needle in m and 'CloudStore.push(prefs)' not in m:
    m = m.replace(needle, needle + '''
        if (CloudStore.enabled()) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { CloudStore.push(prefs) }
        }''', 1)

M.write_text(m, encoding='utf-8')
print('CLOUD ACCOUNT PERSISTENCE PATCH OK')
