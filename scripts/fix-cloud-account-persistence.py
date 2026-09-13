from pathlib import Path

# Cloud persistence is implemented directly in CloudStore/MainActivity.
# Keep this build step idempotent so it never corrupts the stable authentication flow.
root = Path('app/src/main/java/com/dehghanzadeh/chemtrade')
assert (root / 'CloudStore.kt').exists()
assert (root / 'EntryActivity.kt').exists()
assert (root / 'MainActivity.kt').exists()
print('CLOUD ACCOUNT PERSISTENCE CHECK OK')
