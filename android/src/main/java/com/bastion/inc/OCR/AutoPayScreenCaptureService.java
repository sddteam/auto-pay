package com.bastion.inc.OCR;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.bastion.inc.OverlayManager;

public class AutoPayScreenCaptureService extends Service {
    private static final String TAG = "MediaProjectionService";
    public static final String CHANNEL_ID = "ScreenCaptureChannel";
    private MediaProjectionHandler mediaProjectionHandler;
    private OverlayManager overlayManager;

    @Override
    public void onCreate(){
        super.onCreate();

        mediaProjectionHandler = MediaProjectionHandler.getInstance(getApplicationContext());
        overlayManager = OverlayManager.getInstance(getApplicationContext());

        try{
            // Create a notification channel for foreground service
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Projection Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            Intent stopIntent = new Intent(this, AutoPayScreenCaptureService.class);
            stopIntent.setAction("STOP_CAPTURE");
            PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

            // Create a notification for the foreground service
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Media Projection Service")
                    .setContentText("Capturing screen...")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Capture", stopPendingIntent)
                    .build();

            //overlayManager.showWhiteOverlay();
            overlayManager.showOverlay("AutoPay processing...");

            // Start the service in the foreground
            startForeground(1, notification);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        if (intent != null && "STOP_CAPTURE".equals(intent.getAction())) {
            // Stop the screen capture if the stop action was triggered
            mediaProjectionHandler.stopMediaProjection();
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        stopSelf();
        overlayManager.removeOverlay();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
