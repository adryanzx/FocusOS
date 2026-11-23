package br.focusos

import android.bluetooth.BluetoothManager
import android.content.*
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MediaListener : NotificationListenerService()

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val on = runBlocking { c.store.data.first()[K_BOOT] ?: false }
        if (on) runCatching { c.startActivity(Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

data class Now(val title: String, val artist: String, val art: Bitmap?, val playing: Boolean, val ctl: MediaController)

fun Context.hasNotifAccess() = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

fun Context.nowPlaying(): Now? = try {
    val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    val c = msm.getActiveSessions(ComponentName(this, MediaListener::class.java)).firstOrNull()
    c?.metadata?.let { m ->
        Now(m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "—", m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART),
            c.playbackState?.state == PlaybackState.STATE_PLAYING, c)
    }
} catch (e: Exception) { null }

fun Context.btOn() = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true

fun Context.wifiOn(): Boolean {
    val cm = getSystemService(ConnectivityManager::class.java)
    return cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
}
