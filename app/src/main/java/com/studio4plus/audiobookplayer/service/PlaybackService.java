package com.studio4plus.audiobookplayer.service;

import android.app.Notification;
import android.content.Context;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.studio4plus.audiobookplayer.R;
import com.studio4plus.audiobookplayer.model.AudioBook;
import com.studio4plus.audiobookplayer.ui.MainActivity;

public class PlaybackService extends Service implements FaceDownDetector.Listener {

    private static final int NOTIFICATION = R.string.playback_service_notification;

    private AudioBookPlayer player;
    private FaceDownDetector faceDownDetector;

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (FaceDownDetector.hasSensors(sensorManager)) {
            faceDownDetector =
                    new FaceDownDetector(sensorManager, new Handler(getMainLooper()), this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayback();
    }

    public void startPlayback(AudioBook book) {
        if (player != null)
            player.stopPlayback();

        player = new AudioBookPlayer(book);
        player.startPlayback();

        if (faceDownDetector != null)
            faceDownDetector.enable();

        startForeground(NOTIFICATION, createNotification());
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public void stopPlayback() {
        if (player != null)
            player.stopPlayback();

        if (faceDownDetector != null)
            faceDownDetector.disable();

        player = null;
        stopForeground(true);
    }

    @Override
    public void onDeviceFaceDown() {
        stopPlayback();
    }

    private Notification createNotification() {
        Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(
                getApplicationContext(), 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(getResources().getString(R.string.playback_service_notification))
                .setContentIntent(intent)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }

    public class ServiceBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }
}
