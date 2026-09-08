from pathlib import Path

ROOT = Path("app/src/main")
PKG = ROOT / "java/com/dehghanzadeh/chemtrade"

backup = PKG / "PersistentBackup.kt"
backup.write_text(r'''package com.dehghanzadeh.chemtrade

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Keeps an encrypted recovery copy outside the app's private storage.
 * On Android 10+ it is stored in the public Downloads/ChemLink folder, so it
 * is not removed when the APK is uninstalled. Android Auto Backup remains the
 * secondary restore mechanism on devices that support it.
 */
object PersistentBackup {
    private const val FILE_NAME = "ChemLink_Backup.enc"
    private const val RELATIVE_PATH = "Download/ChemLink/"
    private const val PREFIX = "ChemLinkBackupV1"
    private const val KEY_SEED = "ChemLink-local-backup-v1"

    private fun key(): SecretKeySpec {
        val bytes = MessageDigest.getInstance("SHA-256").digest(KEY_SEED.toByteArray())
        return SecretKeySpec(bytes, "AES")
    }

    private fun encrypt(plain: String): ByteArray {
        val iv = ByteArray(12)
        java.security.SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return iv + encrypted
    }

    private fun decrypt(data: ByteArray): String? = runCatching {
        require(data.size > 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, data.copyOfRange(0, 12)))
        cipher.doFinal(data.copyOfRange(12, data.size)).toString(Charsets.UTF_8)
    }.getOrNull()

    private fun serialize(prefs: SharedPreferences): String {
        val root = JSONObject().put("prefix", PREFIX).put("version", 1)
        val values = JSONObject()
        prefs.all.forEach { (k, v) ->
            when (v) {
                is String -> values.put(k, JSONObject().put("t", "s").put("v", v))
                is Boolean -> values.put(k, JSONObject().put("t", "b").put("v", v))
                is Int -> values.put(k, JSONObject().put("t", "i").put("v", v))
                is Long -> values.put(k, JSONObject().put("t", "l").put("v", v))
                is Float -> values.put(k, JSONObject().put("t", "f").put("v", v.toDouble()))
            }
        }
        root.put("values", values)
        return root.toString()
    }

    fun backup(context: Context, prefs: SharedPreferences) {
        runCatching {
            val bytes = encrypt(serialize(prefs))
            if (Build.VERSION.SDK_INT >= 29) {
                val resolver = context.contentResolver
                val existing = resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Downloads._ID),
                    "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?",
                    arrayOf(FILE_NAME, RELATIVE_PATH),
                    null
                )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
                if (existing != null) resolver.delete(Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, existing.toString()), null, null)
                val values = android.content.ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                    put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                    put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                // Older Android: Auto Backup handles reinstall restoration.
                // No SMS/storage permission is requested here.
            }
        }
    }

    fun restore(context: Context, prefs: SharedPreferences): Boolean {
        if (prefs.all.isNotEmpty()) return false
        return runCatching {
            val bytes = if (Build.VERSION.SDK_INT >= 29) {
                val resolver = context.contentResolver
                val uri = resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Downloads._ID),
                    "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?",
                    arrayOf(FILE_NAME, RELATIVE_PATH),
                    "${MediaStore.Downloads.DATE_MODIFIED} DESC"
                )?.use { c -> if (c.moveToFirst()) Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0).toString()) else null }
                    ?.let { resolver.openInputStream(it)?.use { input -> input.readBytes() } }
            } else null
            val text = bytes?.let(::decrypt) ?: return false
            val root = JSONObject(text)
            if (root.optString("prefix") != PREFIX) return false
            val values = root.optJSONObject("values") ?: return false
            val editor = prefs.edit().clear()
            values.keys().forEach { k ->
                val item = values.optJSONObject(k) ?: return@forEach
                when (item.optString("t")) {
                    "s" -> editor.putString(k, item.optString("v"))
                    "b" -> editor.putBoolean(k, item.optBoolean("v"))
                    "i" -> editor.putInt(k, item.optInt("v"))
                    "l" -> editor.putLong(k, item.optLong("v"))
                    "f" -> editor.putFloat(k, item.optDouble("v").toFloat())
                }
            }
            editor.commit()
        }.getOrDefault(false)
    }
}
''', encoding="utf-8")

# EntryActivity: restore before checking whether a session exists; back up after profile save.
e = PKG / "EntryActivity.kt"
t = e.read_text(encoding="utf-8")
needle = 'val prefs = getSharedPreferences("chemlink", Context.MODE_PRIVATE)\n        if (prefs.getString("phone", "").orEmpty().isNotBlank())'
replacement = 'val prefs = getSharedPreferences("chemlink", Context.MODE_PRIVATE)\n        PersistentBackup.restore(this, prefs)\n        if (prefs.getString("phone", "").orEmpty().isNotBlank())'
if needle in t:
    t = t.replace(needle, replacement, 1)
else:
    raise SystemExit("EntryActivity restore marker not found")
needle2 = 'prefs.edit().putString("users", users.toString()).apply()'
replacement2 = 'prefs.edit().putString("users", users.toString()).apply()\n    PersistentBackup.backup(context, prefs)'
if needle2 in t:
    t = t.replace(needle2, replacement2, 1)
else:
    raise SystemExit("EntryActivity save marker not found")
e.write_text(t, encoding="utf-8")

# MainActivity: restore at startup and update backup whenever offers are persisted.
m = PKG / "MainActivity.kt"
t = m.read_text(encoding="utf-8")
needle = 'val prefs = remember { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE) }\n    var admin by'
replacement = 'val prefs = remember { context.getSharedPreferences("chemlink", Context.MODE_PRIVATE) }\n    LaunchedEffect(Unit) { PersistentBackup.restore(context, prefs) }\n    var admin by'
if needle in t:
    t = t.replace(needle, replacement, 1)
else:
    raise SystemExit("MainActivity startup marker not found")
needle2 = 'prefs.edit().putString("offers", array.toString()).apply()'
replacement2 = 'prefs.edit().putString("offers", array.toString()).apply()\n    PersistentBackup.backup(prefs.edit().run { contextForBackup(prefs) }, prefs)'
# Do not use a fake context helper; instead add a process-level context holder below.
replacement2 = 'prefs.edit().putString("offers", array.toString()).apply()\n    BackupContextHolder.context?.let { PersistentBackup.backup(it, prefs) }'
if needle2 in t:
    t = t.replace(needle2, replacement2, 1)
else:
    raise SystemExit("MainActivity persist marker not found")
if 'private object BackupContextHolder' not in t:
    t += '\n\nprivate object BackupContextHolder {\n    var context: Context? = null\n}\n'
# Set holder from MainScreen context.
needle3 = 'val context = LocalContext.current\n    val prefs = remember'
replacement3 = 'val context = LocalContext.current\n    BackupContextHolder.context = context.applicationContext\n    val prefs = remember'
if needle3 in t:
    t = t.replace(needle3, replacement3, 1)
else:
    raise SystemExit("MainActivity context marker not found")
m.write_text(t, encoding="utf-8")

print("PERSISTENT BACKUP PATCH OK")
