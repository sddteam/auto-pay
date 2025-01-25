package com.bastion.inc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import java.nio.ByteBuffer;

public class AutoPayScreenCaptureService extends Service {
    private static final String TAG = "MediaProjectionService";
    public static final String CHANNEL_ID = "ScreenCaptureChannel";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Surface mSurface;

    @Override
    public void onCreate(){
        super.onCreate();

        //imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 2);
        //mSurface = imageReader.getSurface();
        try{
            // Create a notification channel for foreground service
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Projection Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            // Create a notification for the foreground service
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("Media Projection Service")
                    .setContentText("Capturing screen and audio...")
                    .setSmallIcon(android.R.drawable.ic_menu_camera)
                    .build();

            // Start the service in the foreground
            startForeground(1, notification);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d("MediaProjectionService", "Service started");

        if (intent != null && intent.hasExtra("data") && intent.hasExtra("resultCode")) {
            int resultCode = intent.getIntExtra("resultCode", 0);
            Intent data = intent.getParcelableExtra("data");



            // Start capturing the screen
            startScreenCapture2();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d("MediaProjectionService", "Service stopped");
        if(virtualDisplay != null){
            virtualDisplay.release();
        }
        if(mediaProjection != null){
            mediaProjection.stop();
        }
    }

    private void startScreenCapture2() {
        // Create a virtual display
        if (mediaProjection != null) {
            mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    1080, 1920, 320,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mSurface, null, null);
        }
    }

    private void captureScreen() {
        // Capture an image from ImageReader
        Image image = imageReader.acquireLatestImage();
        if (image != null) {
            // Convert the image to Bitmap
            Bitmap bitmap = imageToBitmap(image);
            image.close();

            // Use the Bitmap (for example, save it or return it via a callback)
            saveBitmap(bitmap);
        }
    }

    private Bitmap imageToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return bitmap;
    }

    private void saveBitmap(Bitmap bitmap) {
        // Implement your bitmap saving logic here (e.g., save to storage, or send to Activity)
        Log.d(TAG, "Bitmap captured: " + bitmap);
    }

    private void startScreenCapture(){
        try{
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            int density = metrics.densityDpi;

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenCapture",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null,
                    null
            );
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public Notification createNotification(){
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Capture")
                .setContentText("Screen capture is running...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel(){
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Screen Capture Service",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if(manager != null){
            manager.createNotificationChannel(channel);
        }
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
