from pathlib import Path
import re

root = Path('app/src/main/java/com/dehghanzadeh/chemtrade')

# Entry: write every newly completed account to Firebase immediately.
e = root / 'EntryActivity.kt'
s = e.read_text(encoding='utf-8')
old = 'saveRegisteredUser(context, EntryUser(phone, accountType, profileName.trim(), company.trim(), address.trim(), city.trim(), landline)); error = ""; finishLogin(accountType)'
new = '''saveRegisteredUser(context, EntryUser(phone, accountType, profileName.trim(), company.trim(), address.trim(), city.trim(), landline))
                        val saved = EntryUser(phone, accountType, profileName.trim(), company.trim(), address.trim(), city.trim(), landline)
                        val userJson = org.json.JSONObject().apply { put("phone", saved.phone); put("type", saved.type); put("name", saved.name); put("company", saved.company); put("address", saved.address); put("city", saved.city); put("landline", saved.landline) }
                        kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) { CloudStore.saveUser(context.getSharedPreferences("chemlink", Context.MODE_PRIVATE), userJson) }
                        error = ""; finishLogin(accountType)'''
if old in s:
    s = s.replace(old, new, 1)
elif 'CloudStore.saveUser(context.getSharedPreferences("chemlink"' not in s:
    raise SystemExit('EntryActivity cloud-save hook not found')
e.write_text(s, encoding='utf-8')

# MainActivity: ensure an offer save is immediately pushed to Firebase.
m = root / 'MainActivity.kt'
s = m.read_text(encoding='utf-8')
if 'val scope = rememberCoroutineScope()' not in s:
    needle = 'val context = LocalContext.current\n'
    if needle not in s:
        raise SystemExit('MainActivity context hook not found')
    s = s.replace(needle, needle + '    val scope = rememberCoroutineScope()\n', 1)

if 'scope.launch(kotlinx.coroutines.Dispatchers.IO) { CloudStore.push(prefs) }' not in s:
    marker = 'persistOffers(prefs, list)'
    if marker not in s:
        raise SystemExit('MainActivity offer persistence marker not found')
    s = s.replace(marker, marker + '\n        scope.launch(kotlinx.coroutines.Dispatchers.IO) { CloudStore.push(prefs) }', 1)

# The earlier cloud-persistence hook already pulls remote data on startup; do not duplicate it.

# Owner must be able to edit the complete offer, including a published offer.
old = '''if (offer.status != Status.APPROVED) OutlinedButton({ edit(offer) }) { Text("ویرایش") }; if (offer.status == Status.APPROVED) Button({ correctionText = ""; correctionTarget = offer }) { Text("درخواست اصلاح") }'''
new = '''OutlinedButton({ edit(offer) }) { Text(if (offer.status == Status.APPROVED) "ویرایش کامل" else "ویرایش") }'''
if old in s:
    s = s.replace(old, new, 1)

# Editing a published offer must send it back to management review after saving.
old = '''if (showOfferForm) OfferFormDialog(editOffer, if (admin) ADMIN_PHONE else phone, { showOfferForm = false; editOffer = null }) { saveOffer(it); showOfferForm = false; editOffer = null; page = if (admin) 0 else 4 }'''
new = '''if (showOfferForm) OfferFormDialog(editOffer, if (admin) ADMIN_PHONE else phone, { showOfferForm = false; editOffer = null }) { submitted ->
        val edited = if (editOffer != null && editOffer!!.status == Status.APPROVED && !admin) submitted.copy(status = Status.PENDING, reason = "") else submitted
        saveOffer(edited)
        showOfferForm = false; editOffer = null; page = if (admin) 0 else 4
    }'''
if old in s:
    s = s.replace(old, new, 1)

m.write_text(s, encoding='utf-8')
print('REAL CLOUD + FULL OFFER EDIT PATCH OK')
