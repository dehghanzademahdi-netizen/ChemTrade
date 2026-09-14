from pathlib import Path

root = Path('app/src/main/java/com/dehghanzadeh/chemtrade')

# Entry: write every newly completed/updated account to Firebase immediately.
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

# MainActivity: pull cloud state on startup, push every offer change immediately,
# and let the owner open the complete offer for edits even after publication.
m = root / 'MainActivity.kt'
s = m.read_text(encoding='utf-8')
if 'LaunchedEffect(Unit) {\n        CloudStore.pull(prefs)' not in s:
    needle = 'var offers by remember { mutableStateOf(loadOffers(prefs)) }'
    repl = needle + '''
    LaunchedEffect(Unit) {
        CloudStore.pull(prefs)
        offers = loadOffers(prefs)
    }'''
    if needle not in s:
        raise SystemExit('MainActivity startup hook not found')
    s = s.replace(needle, repl, 1)

if 'val scope = rememberCoroutineScope()' not in s:
    needle = '''val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE) }'''
    repl = '''val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE) }'''
    if needle not in s:
        raise SystemExit('MainActivity scope hook not found')
    s = s.replace(needle, repl, 1)

old = 'offers = list; persistOffers(prefs, list)'
if old in s:
    new = '''offers = list
        persistOffers(prefs, list)
        scope.launch(kotlinx.coroutines.Dispatchers.IO) { CloudStore.push(prefs) }'''
    s = s.replace(old, new, 1)
elif 'scope.launch(kotlinx.coroutines.Dispatchers.IO) { CloudStore.push(prefs) }' not in s:
    raise SystemExit('MainActivity offer-save hook not found')

old = '''if (offer.status != Status.APPROVED) OutlinedButton({ edit(offer) }) { Text("ویرایش") }; if (offer.status == Status.APPROVED) Button({ correctionText = ""; correctionTarget = offer }) { Text("درخواست اصلاح") }'''
new = '''OutlinedButton({ edit(offer) }) { Text(if (offer.status == Status.APPROVED) "ویرایش کامل" else "ویرایش") }'''
if old in s:
    s = s.replace(old, new, 1)

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
