from pathlib import Path

p = Path("app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt")
s = p.read_text(encoding="utf-8")

# Keep the build pipeline's established source transformations intact, then
# apply the premium home banner as the final presentation-only patch.
s = s.replace(
    "import androidx.compose.ui.graphics.Color\n",
    "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.Brush\n",
    1,
)
s = s.replace(
    "import androidx.compose.foundation.clickable\n",
    "import androidx.compose.foundation.clickable\nimport androidx.compose.foundation.background\n",
    1,
)

needle = "            HiddenAdminBrand(onAdminUnlock)\n"
if needle not in s:
    raise SystemExit("Could not locate marketplace home header after standard build patches.")
s = s.replace(needle, "            ChemLinkPremiumBanner(onAdminUnlock)\n", 1)

banner = '''
@Composable
private fun ChemLinkPremiumBanner(onAdminUnlock: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HiddenAdminBrand(onAdminUnlock)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Box(
                Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(Color(0xFF087FE0), Color(0xFF0755A3), Color(0xFF062B59)))
                ).padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("CHEMLINK  •  B2B MARKETPLACE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBDEBFF), fontWeight = FontWeight.Bold)
                        Text("تأمین و تجارت مواد شیمیایی", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text("با تکیه بر اعتماد، کیفیت و تأمین پایدار", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE4F5FF))
                    }
                    Image(painterResource(R.drawable.ic_chemtrade_logo), "لوگوی ChemLink", Modifier.size(68.dp))
                }
            }
        }
    }
}

'''
marker = "@Composable\nprivate fun HiddenAdminBrand("
if marker not in s:
    raise SystemExit("Could not locate hidden admin brand function for banner insertion.")
s = s.replace(marker, banner + marker, 1)

# Align the app's shared visual palette with the approved blue/white web design.
for old, new in {
    "private val Navy = Color(0xFF0B1F33)": "private val Navy = Color(0xFF082C59)",
    "private val Blue = Color(0xFF1565A8)": "private val Blue = Color(0xFF087FE0)",
    "private val Gold = Color(0xFFC8A24A)": "private val Gold = Color(0xFF20AEE8)",
    "private val Cream = Color(0xFFFBF6EE)": "private val Cream = Color(0xFFF3F9FF)",
}.items():
    s = s.replace(old, new, 1)

p.write_text(s, encoding="utf-8")
print("CHEMLINK PREMIUM HOME PATCH OK")
