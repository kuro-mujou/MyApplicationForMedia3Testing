package com.example.myapplication

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.ui.PlayerNotificationManager
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class PlaybackService : MediaSessionService() {
    private val customCommandStop = SessionCommand(ACTION_STOP, Bundle.EMPTY)
    private var mediaSession: MediaSession? = null
//    private lateinit var audioFocusHandler: AudioFocusHandler
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        intent?.let {
//            if (Intent.ACTION_MEDIA_BUTTON == it.action) {
//
//            }
//        }
//        return super.onStartCommand(intent, flags, startId)
//    }
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        val player = TtsPlayer(Looper.getMainLooper(), this)
    //        val player = ExoPlayer.Builder(this).build()
//        val forwardingPlayer = object : ForwardingPlayer(player) {
//            override fun getDuration(): Long {
//                return C.TIME_UNSET
//            }
//        }
//        AudioFocusManager(this, forwardingPlayer)
//        mediaSession = MediaSession.Builder(this, forwardingPlayer)
//            .setCallback(MediaSessionCallback())
//            .build()
        AudioFocusManager(this,player)
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MediaSessionCallback())
            .build()
        PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        ).apply {
            setMediaDescriptionAdapter(
                NotificationMediaDescriptionAdapter(
                    pendingIntent = PendingIntent.getActivity(
                        this@PlaybackService,
                        0,
                        Intent(this@PlaybackService, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    ),this@PlaybackService
                )
            )
            setNotificationListener(
                object : PlayerNotificationManager.NotificationListener{
                    override fun onNotificationCancelled(
                        notificationId: Int,
                        dismissedByUser: Boolean
                    ) {
                        super.onNotificationCancelled(notificationId, dismissedByUser)
                        Log.d("PlaybackService", "Notification cancelled")
//                        stopSelf()
                        mediaSession?.player?.stop()
                    }

                    override fun onNotificationPosted(
                        notificationId: Int,
                        notification: Notification,
                        ongoing: Boolean
                    ) {
                        super.onNotificationPosted(notificationId, notification, ongoing)
                        if (ongoing) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                startForeground(notificationId, notification, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                                Log.d("PlaybackService", "startForeground")
                            }else{
                                startForeground(notificationId, notification)
                            }
                        } else {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        }
                    }

                }
            )
        }.build().also {
            it.setMediaSessionToken(mediaSession!!.platformToken)
            it.setPlayer(player)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
    private inner class MediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ConnectionResult {
            val sessionCommands =
                ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(customCommandStop)
                    .build()
            val playerCommands =
                ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .remove(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .remove(Player.COMMAND_SEEK_TO_NEXT)
                    .remove(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .build()
            return ConnectionResult.AcceptedResultBuilder(session)
                .setCustomLayout(
                    ImmutableList.of(
                        createStopButton(customCommandStop)
                    )
                )
                .setAvailablePlayerCommands(playerCommands)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }
        private fun createStopButton(customCommandFavorites: SessionCommand): CommandButton {
            return CommandButton.Builder()
                .setDisplayName("Save to favorites")
                .setIconResId(R.drawable.ic_launcher_foreground)
                .setSessionCommand(customCommandFavorites)
                .build()
        }
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_STOP -> {
                    session.player.stop()
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                else -> {

                }
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            mediaSession.player.addMediaItems(mediaItems)
            return Futures.immediateFuture(mediaItems)
        }
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            mediaSession.player.setMediaItems(mediaItems, startIndex, startPositionMs)
            return Futures.immediateFuture(MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs))
        }
    }
    companion object {
        const val NOTIFICATION_ID = 123
        const val CHANNEL_ID = "TTS_PLAYER_CHANNEL"
        const val ACTION_STOP = "STOP_TTS_PLAYER"
    }
}
@UnstableApi
private class NotificationMediaDescriptionAdapter(
    private val pendingIntent: PendingIntent,
    private val context: Context
) : PlayerNotificationManager.MediaDescriptionAdapter {
    override fun getCurrentContentTitle(player: Player): CharSequence {
        return player.mediaMetadata.title ?: "Unknown title"
    }

    override fun createCurrentContentIntent(player: Player): PendingIntent? {
        return pendingIntent
    }

    override fun getCurrentContentText(player: Player): CharSequence? {
        return player.mediaMetadata.description ?: "Unknown artist"
    }

    override fun getCurrentLargeIcon(
        player: Player,
        callback: PlayerNotificationManager.BitmapCallback
    ): Bitmap? {
        return null
    }
}