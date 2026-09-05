from pathlib import Path

path = Path("app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt")
s = path.read_text(encoding="utf-8")

# Keep every Android/Compose import used by the existing admin SMS flow.
required_imports = [
    "import android.Manifest",
    "import android.content.pm.PackageManager",
    "import android.telephony.SmsManager",
    "import androidx.core.content.ContextCompat",
    "import androidx.activity.compose.rememberLauncherForActivityResult",
    "import androidx.activity.result.contract.ActivityResultContracts",
    "import androidx.compose.foundation.gestures.detectTapGestures",
]
for imp in required_imports:
    if imp not in s:
        if imp.startswith("import androidx.compose"):
            marker = "import androidx.compose.foundation.Image"
        else:
            marker = "import androidx.activity.ComponentActivity"
        s = s.replace(marker, marker + "\n" + imp, 1)

# The market screen must receive the admin-unlock callback from MainScreen;
# otherwise showAdminLogin is out of scope and Kotlin compilation fails.
s = s.replace(
    "MarketPage(Modifier.padding(padding), offers, search, { search = it }) { detailsOffer = it }",
    "MarketPage(Modifier.padding(padding), offers, search, { search = it }, { detailsOffer = it }) { showAdminLogin = true }"
)
s = s.replace(
    "private fun MarketPage(modifier: Modifier, offers: List<Offer>, query: String, onQuery: (String) -> Unit, open: (Offer) -> Unit) {",
    "private fun MarketPage(modifier: Modifier, offers: List<Offer>, query: String, onQuery: (String) -> Unit, open: (Offer) -> Unit, onAdminUnlock: () -> Unit) {"
)
s = s.replace(
    "HiddenAdminBrand { showAdminLogin = true }",
    "HiddenAdminBrand { onAdminUnlock() }"
)

# Replace the unavailable awaitEachGesture implementation with the simpler
# detectTapGestures long-press API, compatible with the project's Compose version.
old_start = s.find("    Box(Modifier.fillMaxWidth().pointerInput(Unit) {")
old_end = s.find("    }, contentAlignment = Alignment.Center) {", old_start)
if old_start >= 0 and old_end >= 0:
    replacement = '''    Box(Modifier.fillMaxWidth().pointerInput(Unit) {
        detectTapGestures(onLongPress = {
            longPressed = true
        })
    }, contentAlignment = Alignment.Center) {'''
    s = s[:old_start] + replacement + s[old_end + len("    }, contentAlignment = Alignment.Center) {"):]

path.write_text(s, encoding="utf-8")
print("Admin login compile fixes applied")
