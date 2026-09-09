from pathlib import Path

# ChemLink visual system: bright, clean, premium cyan/silver with a light blue canvas.
replacements = {
    "0xFF0B1F33": "0xFF164B70",  # primary navy -> lighter blue-navy
    "0xFF1565A8": "0xFF079AD6",  # brand blue -> cyan blue
    "0xFFC8A24A": "0xFF6E8799",  # gold -> cool silver accent
    "0xFFC89618": "0xFF6E8799",  # old gold -> cool silver accent
    "0xFFFBF6EE": "0xFFF1F9FD",  # warm cream -> airy blue-white
    "0xFFFBF8F2": "0xFFF1F9FD",
}

for rel in [
    "app/src/main/java/com/dehghanzadeh/chemtrade/MainActivity.kt",
    "app/src/main/java/com/dehghanzadeh/chemtrade/EntryActivity.kt",
]:
    p = Path(rel)
    s = p.read_text(encoding="utf-8")
    for old, new in replacements.items():
        s = s.replace(old, new)
    p.write_text(s, encoding="utf-8")

print("CHEMLINK BRAND THEME PATCH OK")
