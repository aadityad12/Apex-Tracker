package com.example.apextracker.media

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The focus surface's view of whatever is playing on the device.
 *
 * Separate from `StudyViewModel` on purpose: that class is about the stopwatch, and the media
 * session has nothing to do with banking study time. Keeping them apart also means the platform
 * listener is only registered while something is actually collecting this — `WhileSubscribed`, and
 * the panel is only composed in focus mode — rather than for the life of the app.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaControlViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaSessionRepository(application)

    // Note there is no position ticker here. A session publishes its position once and then goes
    // quiet, so it has to be advanced by *something* — but a StateFlow conflates equal values, so
    // re-emitting the same snapshot once a second would reach no collector at all. The panel
    // advances it instead, from the snapshot, in a produceState scoped to the panel itself.

    private val _hasAccess = MutableStateFlow(repository.hasAccess())
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    val nowPlaying: StateFlow<NowPlaying?> = _hasAccess
        .flatMapLatest { granted -> if (granted) repository.nowPlayingFlow() else flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Re-reads the notification-access grant. Called when the study screen resumes, because the
     * user grants it in system Settings — the app is in the background for the entire act of
     * turning this feature on, and nothing tells it when they come back.
     */
    fun refreshAccess() {
        _hasAccess.value = repository.hasAccess()
    }

    /** Where the user grants access; the caller starts it so a failure is handled in one place. */
    fun accessSettingsIntent(): Intent = repository.accessSettingsIntent()

    // Transport commands hop off the main thread: each one re-reads the active sessions, which is a
    // binder call to the system server.
    fun playPause() = dispatch { repository.playPause() }

    fun next() = dispatch { repository.next() }

    fun previous() = dispatch { repository.previous() }

    private fun dispatch(action: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) { action() }
    }
}
