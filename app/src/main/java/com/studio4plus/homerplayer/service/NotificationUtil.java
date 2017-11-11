package com.studio4plus.homerplayer.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.studio4plus.homerplayer.ui.MainActivity;

class NotificationUtil {
    static Notification createForegroundServiceNotification(
            Context context, int stringId, int drawableId) {
        Intent activityIntent = new Intent(context, MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(
                context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(context)
                .setContentTitle(context.getResources().getString(stringId))
                .setContentIntent(intent)
                .setSmallIcon(drawableId)
                .setOngoing(true)
                .build();
    }
}
