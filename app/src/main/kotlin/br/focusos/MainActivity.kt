@file:OptIn(ExperimentalFoundationApi::class)
package br.focusos

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.graphics.drawable.toBitmap
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val Blue = Color(0xFF4F8DDB)
val Muted = Color(0xFF9AA4B0)
val Glass = Color(0x22FFFFFF)
val PT = Locale("pt", "BR")

class MainActivity : ComponentActivity() {
    private val tick = mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { FocusApp(tick.intValue) } }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); tick.intValue++ }
}

@Composable
fun FocusApp(tick: Int) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs by ctx.store.data.collectAsState(initial = emptyPreferences())
    var screen by remember { mutableStateOf("home") }
    var pick by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tick) { screen = "home"; pick = null }
    BackHandler(screen != "home") { screen = "home"; pick = null }
    val car = Color(prefs[K_CAR] ?: CAR_COLORS[0]); val anim = prefs[K_ANIM] ?: true
    fun slotPkg(s: String): String? = prefs[slotKey(s)] ?: DEFAULT_SLOTS[s]
    fun tap(s: String) {
        when (s) {
            "bt" -> ctx.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            "apps" -> screen = "apps"
            "settings" -> screen = "settings"
            else -> { val p = slotPkg(s)
                if (p == null) { pick = s; screen = "apps" }
                else if (!ctx.open(p)) Toast.makeText(ctx, "App não instalado", Toast.LENGTH_SHORT).show() }
        }
    }
    fun long(s: String) { if (s in DEFAULT_SLOTS || s == "radio" || s == "usb") { pick = s; screen = "apps" } }
    MaterialTheme(colorScheme = darkColorScheme(primary = Blue)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF14171C), Color(0xFF050506))))) {
            when (screen) {
                "home" -> Home(car, anim, prefs[K_24] ?: true, ::slotPkg, ::tap, ::long) { screen = "focus" }
                "focus" -> MeuFocus(car, anim)
                "apps" -> AppsScreen(pick) { a ->
                    val p = pick
                    if (p != null) { scope.launch { ctx.store.edit { it[slotKey(p)] = a.pkg } }; pick = null; screen = "home" }
                    else ctx.open(a.pkg)
                }
                else -> SettingsScreen(prefs) { f -> scope.launch { ctx.store.edit { f(it) } } }
            }
        }
    }
}

@Composable
fun Home(car: Color, anim: Boolean, h24: Boolean, slotPkg: (String) -> String?, onSlot: (String) -> Unit, onLong: (String) -> Unit, onCar: () -> Unit) {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var bt by remember { mutableStateOf(false) }
    var wifi by remember { mutableStateOf(false) }
    var access by remember { mutableStateOf(false) }
    var media by remember { mutableStateOf<Now?>(null) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }
    LaunchedEffect(Unit) { while (true) { bt = ctx.btOn(); wifi = ctx.wifiOn(); access = ctx.hasNotifAccess(); media = if (access) ctx.nowPlaying() else null; delay(2000) } }
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(now.format(DateTimeFormatter.ofPattern(if (h24) "HH:mm" else "hh:mm a")), fontSize = 64.sp, fontWeight = FontWeight.Light, color = Color.White)
            Spacer(Modifier.width(20.dp))
            Column {
                Text(now.format(DateTimeFormatter.ofPattern("EEEE", PT)).replaceFirstChar { it.uppercase() }, fontSize = 20.sp, color = Blue)
                Text(now.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", PT)), fontSize = 16.sp, color = Muted)
            }
            Spacer(Modifier.weight(1f))
            Chip("Bluetooth", bt); Spacer(Modifier.width(8.dp)); Chip("Wi-Fi", wifi)
        }
        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoCard("Bluetooth", if (bt) "Ligado" else "Desligado")
                InfoCard("Wi-Fi", if (wifi) "Conectado" else "Sem conexão")
                InfoCard("Veículo", "Dados não conectados")
            }
            FocusCar(car, anim, Modifier.weight(2.2f).clickable(onClick = onCar))
            Column(Modifier.weight(1f)) { MediaCard(media, access) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("waze" to "Waze", "maps" to "Maps", "spotify" to "Spotify", "bt" to "Bluetooth", "radio" to "Rádio FM", "usb" to "USB", "apps" to "Apps", "settings" to "Config.").forEach { (s, l) ->
                val g = mapOf("bt" to "BT", "radio" to "FM", "usb" to "USB", "apps" to "⋯", "settings" to "⚙")[s] ?: l.take(1)
                Tile(l, slotPkg(s), g, { onSlot(s) }, { onLong(s) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun Tile(label: String, pkg: String?, glyph: String, onClick: () -> Unit, onLong: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val icon = remember(pkg) { pkg?.let { runCatching { ctx.packageManager.getApplicationIcon(it).toBitmap(112, 112).asImageBitmap() }.getOrNull() } }
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Glass).combinedClickable(onClick = onClick, onLongClick = onLong).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            if (icon != null) Image(icon, null, Modifier.fillMaxSize()) else Text(glyph, fontSize = 24.sp, color = Blue, fontWeight = FontWeight.Bold)
        }
        Text(label, fontSize = 13.sp, color = Color(0xFFCCD3DC), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable fun Chip(t: String, on: Boolean) = Text(t, color = if (on) Blue else Color(0xFF5B636D), fontSize = 14.sp,
    modifier = Modifier.clip(RoundedCornerShape(50)).background(Glass).padding(horizontal = 12.dp, vertical = 6.dp))

@Composable fun InfoCard(t: String, b: String, modifier: Modifier = Modifier) =
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Glass).padding(14.dp)) {
        Text(t, fontSize = 12.sp, color = Muted); Text(b, fontSize = 16.sp, color = Color.White)
    }

@Composable
fun MediaCard(m: Now?, access: Boolean) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Glass).padding(14.dp)) {
        Text("Música", fontSize = 12.sp, color = Muted)
        when {
            !access -> { Text("Sem acesso à mídia", color = Color.White, fontSize = 15.sp)
                TextButton({ ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("Permitir") } }
            m == null -> Text("Nada tocando", color = Color.White, fontSize = 15.sp)
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    m.art?.let { Image(it.asImageBitmap(), null, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))); Spacer(Modifier.width(10.dp)) }
                    Column { Text(m.title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(m.artist, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Row {
                    TextButton({ m.ctl.transportControls.skipToPrevious() }) { Text("⏮") }
                    TextButton({ if (m.playing) m.ctl.transportControls.pause() else m.ctl.transportControls.play() }) { Text(if (m.playing) "⏸" else "▶") }
                    TextButton({ m.ctl.transportControls.skipToNext() }) { Text("⏭") }
                }
            }
        }
    }
}

@Composable
fun MeuFocus(car: Color, anim: Boolean) {
    Column(Modifier.fillMaxSize().padding(28.dp)) {
        Text("MEU FOCUS", fontSize = 28.sp, color = Blue, fontWeight = FontWeight.Bold)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            FocusCar(car, anim, Modifier.weight(1.6f))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoCard("Modelo", "Ford Focus Hatch 2013 · 1.6 · Manual")
                InfoCard("Multimídia", "${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
                listOf("Velocidade", "RPM", "Combustível", "Temp. do motor", "Bateria", "Portas").forEach { InfoCard(it, "Não conectado") }
            }
        }
    }
}

@Composable
fun AppsScreen(pick: String?, onApp: (AppItem) -> Unit) {
    val ctx = LocalContext.current
    val apps by produceState(emptyList<AppItem>()) { value = withContext(Dispatchers.Default) { ctx.installedApps() } }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(if (pick != null) "Escolha o app para este atalho" else "Aplicativos", fontSize = 26.sp, color = Color.White)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(GridCells.Adaptive(110.dp)) { items(apps, key = { it.pkg }) { a -> Tile(a.label, a.pkg, "", { onApp(a) }, {}, Modifier.padding(6.dp)) } }
    }
}

@Composable
fun SwitchRow(t: String, v: Boolean, onChange: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(t, color = Color.White, modifier = Modifier.weight(1f)); Switch(v, onChange)
    }

@Composable
fun SettingsScreen(prefs: Preferences, onEdit: ((MutablePreferences) -> Unit) -> Unit) {
    val ctx = LocalContext.current
    fun go(i: Intent) = ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    val dm = ctx.resources.displayMetrics
    val mem = ActivityManager.MemoryInfo().also { (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it) }
    val cur = prefs[K_CAR] ?: CAR_COLORS[0]
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Configurações", fontSize = 26.sp, color = Color.White)
        Text("Cor do carro", color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CAR_COLORS.forEach { c -> Box(Modifier.size(44.dp).clip(CircleShape).background(Color(c))
                .border(if (cur == c) 3.dp else 0.dp, Color.White, CircleShape).clickable { onEdit { p -> p[K_CAR] = c } }) }
        }
        SwitchRow("Animações", prefs[K_ANIM] ?: true) { v -> onEdit { it[K_ANIM] = v } }
        SwitchRow("Relógio 24 horas", prefs[K_24] ?: true) { v -> onEdit { it[K_24] = v } }
        SwitchRow("Abrir ao ligar a central (exige 'Sobrepor outros apps')", prefs[K_BOOT] ?: false) { v -> onEdit { it[K_BOOT] = v } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button({ go(Intent(Settings.ACTION_HOME_SETTINGS)) }) { Text("Launcher padrão") }
            Button({ go(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))) }) { Text("Sobrepor apps") }
            Button({ go(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Acesso à mídia") }
        }
        InfoCard("Informações do sistema",
            "Fabricante: ${Build.MANUFACTURER}\nModelo: ${Build.MODEL}\nDispositivo: ${Build.DEVICE} / ${Build.HARDWARE} / ${Build.BOARD}\n" +
            "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\nBuild: ${Build.DISPLAY}\nCPU: ${Build.SUPPORTED_ABIS.joinToString()}\n" +
            "Tela: ${dm.widthPixels}x${dm.heightPixels} @ ${dm.densityDpi} dpi\nRAM total: ${mem.totalMem / 1048576} MB")
        Text("FOCUS OS 0.1.0", color = Muted)
    }
}
