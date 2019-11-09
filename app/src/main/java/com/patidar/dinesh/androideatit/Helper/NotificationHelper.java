package com.patidar.dinesh.androideatit.Helper;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Build;

import com.patidar.dinesh.androideatit.R;

public class NotificationHelper extends ContextWrapper {

    private static final String DINESH_CHENAL_ID = "com.patidar.dinesh.androideatit.Dinesh";
    private static final String DINESH_CHENAL_NAME  = "Eat It";

    private NotificationManager manager;

    public NotificationHelper(Context base) {
        super(base);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createChannel();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationChannel dineshChennel = new NotificationChannel(DINESH_CHENAL_ID,
                DINESH_CHENAL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT);

        dineshChennel.enableLights(false);
        dineshChennel.enableVibration(true);
        dineshChennel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        getManager().createNotificationChannel(dineshChennel);
    }

    public NotificationManager getManager() {
        if (manager == null)
            manager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        return  manager;
    }

    @TargetApi(Build.VERSION_CODES.O)
    public Notification.Builder getEatItChannelNotification(String title, String body, PendingIntent contentIntent, Uri soundUri)
    {
        return new Notification.Builder(getApplicationContext(),DINESH_CHENAL_ID)
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(soundUri)
                .setAutoCancel(false);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public Notification.Builder getEatItChannelNotification(String title, String body, Uri soundUri)
    {
        return new Notification.Builder(getApplicationContext(),DINESH_CHENAL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(soundUri)
                .setAutoCancel(false);
    }
}
