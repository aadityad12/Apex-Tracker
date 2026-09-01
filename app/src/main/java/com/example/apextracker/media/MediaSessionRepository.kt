package com.example.apextracker.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val TAG = "MediaSessionRepository"

/**
 * The one place this app touches the platform's media session APIs, so the whole feature is a
 * single file's worth of surface — the same containment `ReceiptOcr.kt` gives ML Kit.
 *
 * Everything here is gated on the user having granted notification access
 * ([hasAccess]); without it `getActiveSessions` throws `SecurityException`, and the panel shows its
 * connect prompt rather than pretending there is nothing playing.
 */
class MediaSessionRepository(private val context: Context) {

    private val listenerComponent = ComponentName(context, ApexMediaListenerService::class.java)

    private val sessionManager: MediaSessionManager?
        get() = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    /** Whether the user has granted this app notification access, which is what unlocks the rest. */
    fun hasAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    /**
     * The session the panel should follow, re-emitted whenever it changes, what it is playing
     * changes, or it goes away. Null while nothing is active or access has not been granted.
     *
     * The position in each emission is a snapshot; the caller is responsible for advancing it (see
     * [NowPlaying.positionAt]) rather than this flow emitting once a second forever.
     */
    fun nowPlayingFlow(): Flow<NowPlaying?> = callbackFlow {
        val manager = sessionManager
        if (manager == null || !hasAccess()) {
            trySend(null)
            // awaitClose even on the giving-up path: callbackFlow throws without it (Issue #246).
            return@callbackFlow awaitClose { }
        }

        var boundController: MediaController? = null
        var boundCallback: MediaController.Callback? = null

        fun unbind() {
            boundCallback?.let { boundController?.unregisterCallback(it) }
            boundController = null
            boundCallback = null
        }

        fun bind(controllers: List<MediaController>) {
            unbind()
            val chosen = chooseController(controllers)
            if (chosen == null) {
                trySend(null)
                return
            }
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    trySend(readNowPlaying(chosen))
                }

                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    trySend(readNowPlaying(chosen))
                }

                override fun onSessionDestroyed() {
                    // The active-sessions listener below will re-bind; emitting null first stops
                    // the panel showing a track from a player that has already gone.
                    trySend(null)
                }
            }
            chosen.registerCallback(callback)
            boundController = chosen
            boundCallback = callback
            trySend(readNowPlaying(chosen))
        }

        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            bind(controllers.orEmpty())
        }

        try {
            // Explicit main-Looper Handler: the 2-arg overload posts to the calling thread's
            // looper, and a flow collected on a background dispatcher has none.
            manager.addOnActiveSessionsChangedListener(
                listener,
                listenerComponent,
                Handler(Looper.getMainLooper())
            )
            bind(manager.getActiveSessions(listenerComponent))
        } catch (e: SecurityException) {
            // Access revoked between the check above and here — treat it as "nothing playing"
            // rather than taking the app down.
            Log.w(TAG, "Media session access denied", e)
            trySend(null)
        }

        awaitClose {
            unbind()
            runCatching { manager.removeOnActiveSessionsChangedListener(listener) }
                .onFailure { Log.w(TAG, "Failed to remove session listener", it) }
        }
    }

    private fun chooseController(controllers: List<MediaController>): MediaController? {
        val candidates = controllers.map {
            MediaCandidate(
                packageName = it.packageName,
                isPlaying = it.playbackState?.state == PlaybackState.STATE_PLAYING
            )
        }
        val preferred = pickPreferredSession(candidates) ?: return null
        // Match on identity of position, not package: two sessions can share a package.
        return controllers.getOrNull(candidates.indexOf(preferred))
    }

    private fun readNowPlaying(controller: MediaController): NowPlaying {
        val metadata = controller.metadata
        val state = controller.playbackState
        return NowPlaying(
            packageName = controller.packageName,
            title = metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString().orEmpty(),
            artist = metadata?.let {
                it.getText(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: it.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                    ?: it.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            }?.toString().orEmpty(),
            // Three keys, because players disagree about which one carries the cover.
            artwork = metadata?.let {
                it.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: it.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: it.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            },
            durationMillis = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            isPlaying = state?.state == PlaybackState.STATE_PLAYING,
            reportedPositionMillis = state?.position ?: 0L,
            positionUpdatedAtRealtime = state?.lastPositionUpdateTime ?: 0L,
            playbackSpeed = state?.playbackSpeed ?: 0f
        )
    }

    /** Runs [action] against the currently preferred session, if there is one. */
    private fun withController(action: (MediaController.TransportControls) -> Unit) {
        val manager = sessionManager ?: return
        if (!hasAccess()) return
        try {
            val controller = chooseController(manager.getActiveSessions(listenerComponent)) ?: return
            action(controller.transportControls)
        } catch (e: SecurityException) {
            Log.w(TAG, "Media control denied", e)
        }
    }

    fun playPause() = withController { controls ->
        // Read the state through the same chooser the command goes to, so a session that changed
        // between the panel rendering and the tap can't get the wrong command.
        val manager = sessionManager ?: return@withController
        val playing = runCatching {
            chooseController(manager.getActiveSessions(listenerComponent))
                ?.playbackState?.state == PlaybackState.STATE_PLAYING
        }.getOrDefault(false)
        if (playing) controls.pause() else controls.play()
    }

    fun next() = withController { it.skipToNext() }

    fun previous() = withController { it.skipToPrevious() }

    /**
     * Where the user grants access. API 30+ can deep-link straight to this app's own row; below
     * that the best available is the full listener list, where they find ApexTracker themselves.
     */
    fun accessSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                listenerComponent.flattenToString()
            )
        } else {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        }
}
