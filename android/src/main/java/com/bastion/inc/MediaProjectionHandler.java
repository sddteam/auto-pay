package com.bastion.inc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import java.nio.ByteBuffer;

public class MediaProjectionHandler {
    private static MediaProjectionHandler instance;
    private final String TAG = "MediaProjectionHandler";
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Context context;

    private ProjectionImageListener projectionImageListener;
    private ApplicationStateListener applicationStateListener;
    private boolean isProcessingImage = false;


    public void setProjectionImageListener(ProjectionImageListener listener){
        this.projectionImageListener = listener;
    }

    public void setApplicationStateListener(ApplicationStateListener listener){
        this.applicationStateListener = listener;
    }

    public MediaProjectionHandler(Context context){
        this.context = context.getApplicationContext();
        this.mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public static synchronized MediaProjectionHandler getInstance(Context context){
        if(instance == null){
            instance = new MediaProjectionHandler(context);
        }

        return instance;
    }

    public void startMediaProjection(Context context, Intent data){
        try{
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            int width = displayMetrics.widthPixels;
            int height = displayMetrics.heightPixels;
            int density = displayMetrics.densityDpi;

            mediaProjection = mediaProjectionManager.getMediaProjection(Activity.RESULT_OK, data);
            mediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    if (virtualDisplay != null) {
                        virtualDisplay.release();
                        virtualDisplay = null;
                    }
                }
            }, null );
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 5);
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

            imageReader.setOnImageAvailableListener(reader -> {
                if (isProcessingImage) {
                    return; // Skip if the previous image is still being processed
                }
                Image image = null;
                try{
                    isProcessingImage = true;
                    image = reader.acquireLatestImage();
                    if(image != null){
                        Bitmap bitmap = convertImageToBitmap(image, width, height);
                        projectionImageListener.onImage(bitmap);
                    }
                }finally{
                    if(image != null){
                        image.close();
                    }
                    isProcessingImage = false;
                }
            }, new Handler(Looper.getMainLooper()));

        }catch (Exception e){
            applicationStateListener.onError(e);
        }
    }

    public Bitmap captureLatestImage() {
        if (imageReader == null) {
            Log.e("Screenshot", "ImageReader is null!");
            return null;
        }

        Image image = null;
        try {
            image = imageReader.acquireLatestImage();
            if (image != null) {
                Bitmap bitmap = convertImageToBitmap(image, image.getWidth(), image.getHeight());
                image.close(); // ✅ Close the image to free memory
                return bitmap;
            }
        } catch (Exception e) {
            Log.e("Screenshot", "Failed to capture screenshot: " + e.getMessage());
        } finally {
            if (image != null) {
                image.close();
            }
        }
        return null;
    }
    private Bitmap convertImageToBitmap(Image image, int width, int height){
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    public void stopMediaProjection(){
        if(mediaProjection != null){
            mediaProjection.stop();
            mediaProjection = null;

            Intent serviceIntent = new Intent(context, AutoPayScreenCaptureService.class);
            context.stopService(serviceIntent);

            redirectMain();
        }

        if(virtualDisplay != null){
            virtualDisplay.release();
            virtualDisplay = null;
        }

        OverlayManager.getInstance(context).removeOverlay();
    }

    private void redirectMain(){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("bastion://scdmobile.app/checkout-gcash"));

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
