from pathlib import Path

main_path = Path("app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt")
s = main_path.read_text(encoding="utf-8")

# Make the actual header logo the hidden admin entry point.
imp = "import androidx.compose.foundation.gestures.detectTapGestures"
if imp not in s:
    marker = "import androidx.compose.foundation.gestures.waitForUpOrCancellation"
    if marker in s:
        s = s.replace(marker, marker + "\n" + imp, 1)
    else:
        marker = "import androidx.compose.foundation.Image"
        s = s.replace(marker, marker + "\n" + imp, 1)

old = 'Image(painterResource(R.drawable.ic_chemtrade_logo), "ChemLink", Modifier.size(34.dp))'
new = '''Image(
                    painterResource(R.drawable.ic_chemtrade_logo),
                    "ChemLink",
                    Modifier.size(44.dp).pointerInput(Unit) {
                        detectTapGestures(onLongPress = { showAdminLogin = true })
                    }
                )'''
if old in s:
    s = s.replace(old, new, 1)

# Keep the market page's hidden logo gesture wired correctly.
s = s.replace(
    'MarketPage(Modifier.padding(padding), offers, search, { search = it }) { detailsOffer = it }',
    'MarketPage(Modifier.padding(padding), offers, search, { search = it }, { detailsOffer = it }) { showAdminLogin = true }'
)
s = s.replace(
    'private fun MarketPage(modifier: Modifier, offers: List<Offer>, query: String, onQuery: (String) -> Unit, open: (Offer) -> Unit) {',
    'private fun MarketPage(modifier: Modifier, offers: List<Offer>, query: String, onQuery: (String) -> Unit, open: (Offer) -> Unit, onAdminUnlock: () -> Unit) {'
)
s = s.replace('HiddenAdminBrand { showAdminLogin = true }', 'HiddenAdminBrand { onAdminUnlock() }')

# Store/read the new profile fields in the existing local user registry.
s = s.replace(
    'private data class UserAccount(val phone: String, val type: String, val name: String = "", val company: String = "")',
    'private data class UserAccount(val phone: String, val type: String, val name: String = "", val company: String = "", val address: String = "", val city: String = "", val landline: String = "")'
)
s = s.replace(
    'result += UserAccount(o.optString("phone"), o.optString("type", "Consumer"), o.optString("name"), o.optString("company"))',
    'result += UserAccount(o.optString("phone"), o.optString("type", "Consumer"), o.optString("name"), o.optString("company"), o.optString("address"), o.optString("city"), o.optString("landline"))'
)

# Show profile details in حساب من, but never expose the seller name in market cards.
old_card = 'account?.company?.takeIf { it.isNotBlank() }?.let { Text("شرکت: $it") }'
new_card = '''account?.company?.takeIf { it.isNotBlank() }?.let { Text("شرکت: $it") }
            account?.city?.takeIf { it.isNotBlank() }?.let { Text("شهر: $it") }
            account?.address?.takeIf { it.isNotBlank() }?.let { Text("آدرس: $it") }
            account?.landline?.takeIf { it.isNotBlank() }?.let { Text("تلفن ثابت: $it") }'''
s = s.replace(old_card, new_card, 1)
main_path.write_text(s, encoding="utf-8")

# Fix EntryActivity's local forward-reference: the permission launcher must be
# available to requestOtp without referring to a later local declaration.
entry_path = Path("app/src/main/java/com/dehghanzadeh/chemtrade/EntryActivity.kt")
e = entry_path.read_text(encoding="utf-8")

needle = '    var sending by rememberSaveable { mutableStateOf(false) }\n\n'
if needle in e and 'var launchSmsPermission' not in e:
    e = e.replace(needle, needle + '    var launchSmsPermission: (() -> Unit)? = null\n\n', 1)

e = e.replace('                permissionLauncher.launch(Manifest.permission.SEND_SMS)', '                launchSmsPermission?.invoke()', 1)

launcher_start = '    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->'
if launcher_start in e and 'launchSmsPermission = { permissionLauncher.launch(Manifest.permission.SEND_SMS) }' not in e:
    # Place the launcher bridge immediately after the launcher callback block.
    marker = '    Surface(modifier = Modifier.fillMaxSize(), color = EntryCream) {'
    e = e.replace(marker, '    launchSmsPermission = { permissionLauncher.launch(Manifest.permission.SEND_SMS) }\n\n' + marker, 1)

entry_path.write_text(e, encoding="utf-8")
print("Final authentication flow patch applied")
