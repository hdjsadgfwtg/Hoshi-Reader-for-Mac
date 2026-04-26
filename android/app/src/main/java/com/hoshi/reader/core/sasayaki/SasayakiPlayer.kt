package com.hoshi.reader.core.sasayaki

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hoshi.reader.data.model.SasayakiMatch
import com.hoshi.reader.data.model.SasayakiPlaybackData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SasayakiPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: ExoPlayer? = null
    private var positionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _playbackData = MutableStateFlow(SasayakiPlaybackData())
    val playbackData: StateFlow<SasayakiPlaybackData> = _playbackData

    private val _currentMatch = MutableStateFlow<SasayakiMatch?>(null)
    val currentMatch: StateFlow<SasayakiMatch?> = _currentMatch

    private var matches: List<SasayakiMatch> = emptyList()

    fun loadAudio(uri: Uri, cueMatches: List<SasayakiMatch>) {
        release()
        matches = cueMatches

        player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playbackData.value = _playbackData.value.copy(isPlaying = isPlaying)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _playbackData.value = _playbackData.value.copy(
                            durationMs = duration
                        )
                    }
                }
            })
        }

        startPositionTracking()
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        updateCurrentMatch(positionMs)
    }

    fun skipToCue(match: SasayakiMatch) {
        seekTo(match.cueStartMs)
        play()
    }

    fun skipForward(ms: Long = 5000) {
        val p = player ?: return
        seekTo((p.currentPosition + ms).coerceAtMost(p.duration))
    }

    fun skipBackward(ms: Long = 5000) {
        val p = player ?: return
        seekTo((p.currentPosition - ms).coerceAtLeast(0))
    }

    fun release() {
        positionJob?.cancel()
        player?.release()
        player = null
        _playbackData.value = SasayakiPlaybackData()
        _currentMatch.value = null
        matches = emptyList()
    }

    private fun startPositionTracking() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val p = player ?: break
                val pos = p.currentPosition
                _playbackData.value = _playbackData.value.copy(positionMs = pos)
                updateCurrentMatch(pos)
                delay(100)
            }
        }
    }

    private fun updateCurrentMatch(positionMs: Long) {
        val match = matches.firstOrNull { positionMs in it.cueStartMs..it.cueEndMs }
        if (match != _currentMatch.value) {
            _currentMatch.value = match
        }
    }
}
