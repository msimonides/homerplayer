package com.studio4plus.homerplayer.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.R;
import com.studio4plus.homerplayer.ui.MainActivity;

public class NotificationUtil {

    // This channel is also used for samples download notifications but I guess
    // it's better to reuse the channel instead of providing another one just for
    // one-time action.
    private static final String PLAYBACK_SERVICE_CHANNEL_ID = "playback";

    static Notification createForegroundServiceNotification(
            Context context, int stringId, int drawableId) {
        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(
                context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(context, PLAYBACK_SERVICE_CHANNEL_ID)
                .setContentTitle(context.getResources().getString(stringId))
                .setContentIntent(intent)
                .setSmallIcon(drawableId)
                .setOngoing(true)
                .build();
    }

    @TargetApi(26)
    public static class API26 {
        public  static void registerPlaybackServiceChannel(Context context) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Preconditions.checkNotNull(manager);

            NotificationChannel channel = new NotificationChannel(
                    PLAYBACK_SERVICE_CHANNEL_ID,
                    context.getString(R.string.notificationChannelPlayback),
                    NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
    }
}
