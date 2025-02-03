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
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import java.nio.ByteBuffer;

public class MediaProjectionHandler {
    private final String TAG = "MediaProjectionHandler";
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Context context;

    private ProjectionImageListener projectionImageListener;
    private ApplicationStateListener applicationStateListener;


    public void setProjectionImageListener(ProjectionImageListener listener){
        this.projectionImageListener = listener;
    }

    public void setApplicationStateListener(ApplicationStateListener listener){
        this.applicationStateListener = listener;
    }

    public MediaProjectionHandler(MediaProjectionManager mediaProjectionManager, Context context){
        this.mediaProjectionManager = mediaProjectionManager;
    }

    public void startMediaProjection(Context context, Intent data){
        try{
            this.context = context;

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

            imageReader.setOnImageAvailableListener(reader -> {
                Image image = null;
                try{
                    image = reader.acquireLatestImage();
                    if(image != null){
                        Bitmap bitmap = convertImageToBitmap(image, width, height);
                        projectionImageListener.onImage(bitmap);
                    }
                }finally{
                    if(image != null){
                        image.close();
                    }
                }
            }, new Handler(Looper.getMainLooper()));

        }catch (Exception e){
            applicationStateListener.onError(e);
        }
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
        }

        if(virtualDisplay != null){
            virtualDisplay.release();
            virtualDisplay = null;
        }

        OverlayManager overlayManager = OverlayManager.getInstance(context);
        overlayManager.removeOverlay();
    }
}
