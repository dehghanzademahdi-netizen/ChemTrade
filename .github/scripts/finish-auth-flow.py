from pathlib import Path

path = Path("app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt")
s = path.read_text(encoding="utf-8")

# 1) Make the actual header logo the hidden admin entry point.
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

# 2) Keep the market page's existing hidden logo gesture wired correctly.
s = s.replace(
    'MarketPage(Modifier.padding(padding), offers, search, { search = it }) { detailsOffer = it }',
    'MarketPage(Modifier.padding(padding), offers, search, { search = it }, { detailsOffer = it }) { showAdminLogin = true }'
)
s = s.replace(
    'private fun MarketPage(modifier: Modifier, offers: List<Offer>, query: String, onQuery: (String) -> Unit, open: (Offer) -> Unit) {',
    'private fun MarketPage(modifier: Modifier, offers: List<Offer>, query: String, onQuery: (String) -> Unit, open: (Offer) -> Unit, onAdminUnlock: () -> Unit) {'
)
s = s.replace('HiddenAdminBrand { showAdminLogin = true }', 'HiddenAdminBrand { onAdminUnlock() }')

# 3) Store/read the new profile fields in the existing local user registry.
s = s.replace(
    'private data class UserAccount(val phone: String, val type: String, val name: String = "", val company: String = "")',
    'private data class UserAccount(val phone: String, val type: String, val name: String = "", val company: String = "", val address: String = "", val city: String = "", val landline: String = "")'
)
s = s.replace(
    'result += UserAccount(o.optString("phone"), o.optString("type", "Consumer"), o.optString("name"), o.optString("company"))',
    'result += UserAccount(o.optString("phone"), o.optString("type", "Consumer"), o.optString("name"), o.optString("company"), o.optString("address"), o.optString("city"), o.optString("landline"))'
)

# 4) Show the profile information in حساب من without exposing seller names in the market.
old_card = 'account?.company?.takeIf { it.isNotBlank() }?.let { Text("شرکت: $it") }'
new_card = '''account?.company?.takeIf { it.isNotBlank() }?.let { Text("شرکت: $it") }
            account?.city?.takeIf { it.isNotBlank() }?.let { Text("شهر: $it") }
            account?.address?.takeIf { it.isNotBlank() }?.let { Text("آدرس: $it") }
            account?.landline?.takeIf { it.isNotBlank() }?.let { Text("تلفن ثابت: $it") }'''
s = s.replace(old_card, new_card, 1)

path.write_text(s, encoding="utf-8")
print("Hidden admin header and profile display fixed")
