package com.knafayim.shush;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * Created by s9iper1 on 11/9/17.
 */

public class ReceiveTransitionsIntentService extends IntentService {

    private NotificationManager notificationManager;
    private AudioManager audioManager;


    public ReceiveTransitionsIntentService() {
        super("name");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        notificationManager =(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        audioManager= (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(
                    geofencingEvent.getErrorCode());
            Log.e("TAG", errorMessage);
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Log.i("TAG", " tranition " + geofenceTransition);
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.i("TAG", "enter");
            AppGlobals.setPreviousRingerState(audioManager.getRingerMode());
            audioManager.setRingerMode(AppGlobals.getEnterMode());
            AppGlobals.ringerModeChangedByUs(true);
            showNotification();
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.i("TAG", "exit");
            if ((AppGlobals.getExitMode() == 0) && AppGlobals.isModeChangedByUs()) {
                audioManager.setRingerMode(AppGlobals.getPreviousRingerState());
                AppGlobals.ringerModeChangedByUs(false);
                notificationManager.cancel(1001);
            }
        } else {
            Log.e("TAG", getString(geofenceTransition));
        }
    }

    private void showNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.shush_icon)
                .setContentTitle("Phone silenced")
                .setContentText("Thank you for using Shush!")
                .setContentIntent(pendingIntent).build();
        notificationManager.notify(1001, notification);
    }
}
