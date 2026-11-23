package br.focusos

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.store by preferencesDataStore("focus")
val K_CAR = longPreferencesKey("car")
val K_ANIM = booleanPreferencesKey("anim")
val K_24 = booleanPreferencesKey("h24")
val K_BOOT = booleanPreferencesKey("boot")
fun slotKey(s: String) = stringPreferencesKey("slot_$s")
val DEFAULT_SLOTS = mapOf("waze" to "com.waze", "maps" to "com.google.android.apps.maps", "spotify" to "com.spotify.music")
val CAR_COLORS = listOf(0xFF3B6EA5, 0xFFB8BDC4, 0xFF1B1D21, 0xFFF2F2F2, 0xFFB3262E, 0xFF5A5F66)

data class AppItem(val label: String, val pkg: String)

fun Context.installedApps(): List<AppItem> {
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(i, 0)
        .map { AppItem(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
        .filter { it.pkg != packageName }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
}

fun Context.open(pkg: String): Boolean {
    val i = packageManager.getLaunchIntentForPackage(pkg) ?: return false
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i); return true
}
