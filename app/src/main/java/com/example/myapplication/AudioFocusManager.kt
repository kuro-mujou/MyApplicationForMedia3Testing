package com.example.myapplication

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.media3.common.Player

@RequiresApi(Build.VERSION_CODES.O)
class AudioFocusManager(private val context: Context, private val player: Player) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var focusRequest : Int = 0
    var focusState by mutableStateOf(AudioFocusState.NO_FOCUS)
        private set
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                focusRequest = audioManager.requestAudioFocus(audioFocusRequest!!)
                // Resume playback
                player.volume = 1.0f
                player.play()
                focusState = AudioFocusState.HAS_FOCUS
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // Stop playback
//                    player.pause()
                focusState = AudioFocusState.NO_FOCUS
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Pause playback (short interruption)
//                    player.pause()
                focusState = AudioFocusState.TRANSIENT_FOCUS
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Lower the volume (ducking)
                player.volume = 0.2f // Adjust ducking volume as needed
                focusState = AudioFocusState.DUCKING_FOCUS
            }
        }
    }

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (player.isPlaying) {
                            Log.d("AudioFocusManager", "Playback started, requesting audio focus")
                            requestAudioFocus()
                        }
                    }
                    Player.STATE_ENDED -> {
                        Log.d("AudioFocusManager", "Playback ended, abandoning audio focus")
//                        abandonAudioFocus()
                    }
                    Player.STATE_IDLE -> {
                        Log.d("AudioFocusManager", "Playback ended or idle, abandoning audio focus")
//                        abandonAudioFocus()
                    }
                    else -> {}
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    Log.d("AudioFocusManager", "Playback started, requesting audio focus")
                    requestAudioFocus()
                } else {
                    Log.d("AudioFocusManager", "Playback paused, abandoning audio focus")
//                    abandonAudioFocus()
                }
            }
        })
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
    }
    private fun requestAudioFocus() {

    }


    private fun abandonAudioFocus() {
        Log.d("AudioFocusManager", "Abandoning audio focus")
        if (focusState == AudioFocusState.NO_FOCUS) return
        if(audioFocusRequest != null){
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        }
        focusState = AudioFocusState.NO_FOCUS
    }
}

enum class AudioFocusState {
    HAS_FOCUS,
    NO_FOCUS,
    TRANSIENT_FOCUS,
    DUCKING_FOCUS
}