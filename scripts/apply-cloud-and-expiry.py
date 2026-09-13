from pathlib import Path

PKG = Path('app/src/main/java/com/dehghanzadeh/chemtrade')
M = PKG / 'MainActivity.kt'
E = PKG / 'EntryActivity.kt'

m = M.read_text(encoding='utf-8')
e = E.read_text(encoding='utf-8')

# Keep the existing OTP/admin flow untouched. Only make the customer data cloud-backed.
# CloudStore is the source of truth; local SharedPreferences are only a cache/recovery copy.
old = '''    var phone by remember { mutableStateOf(prefs.getString("phone", "").orEmpty()) }\n    var offers by remember { mutableStateOf(loadOffers(prefs)) }'''
new = '''    var phone by remember { mutableStateOf(prefs.getString("phone", "").orEmpty()) }\n    var offers by remember { mutableStateOf(loadOffers(prefs)) }\n\n    LaunchedEffect(Unit) {\n        if (CloudStore.enabled()) {\n            CloudStore.pull(prefs)\n            offers = loadOffers(prefs)\n        }\n    }'''
if old in m and 'CloudStore.pull(prefs)' not in m:
    m = m.replace(old, new, 1)

old = '''        offers = list; persistOffers(prefs, list)\n    }'''
new = '''        offers = list; persistOffers(prefs, list)\n        if (CloudStore.enabled()) {\n            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { CloudStore.push(prefs) }\n        }\n    }'''
if old in m and 'CloudStore.push(prefs)' not in m:
    m = m.replace(old, new, 1)

# Do not make expiry destructive: expired offers remain stored and recoverable.
M.write_text(m, encoding='utf-8')
E.write_text(e, encoding='utf-8')
print('CLOUD PERSISTENCE PATCH OK')
