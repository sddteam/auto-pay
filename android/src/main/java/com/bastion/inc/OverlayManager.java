package com.bastion.inc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bastion.inc.OCR.MediaProjectionHandler;

import java.io.InputStream;
import java.util.List;

public class OverlayManager {
    @SuppressLint("StaticFieldLeak")
    private static OverlayManager instance;
    private final WindowManager windowManager;
    private View overlayView;
    private View whiteOverlayView;
    private final Context context;
    private final Handler mainHandler;
    private final MediaProjectionHandler mediaProjectionHandler;
    private BoundingBoxView boundingBoxView;
    private BoundingBoxView boundingBoxView1;


    public OverlayManager(Context context){
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.mediaProjectionHandler = MediaProjectionHandler.getInstance(context.getApplicationContext());

        if (!Settings.canDrawOverlays(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static synchronized OverlayManager getInstance(Context context){
        if(instance == null){
            instance = new OverlayManager(context);
        }

        return instance;
    }


    public void showWhiteOverlay(){
        if (whiteOverlayView != null){
            return;
        }
        mainHandler.post(() -> {
            try{
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                whiteOverlayView = inflater.inflate(R.layout.white_overlay, null);

                ImageView overlayOmnibusinessLogo = whiteOverlayView.findViewById(R.id.overlay_omnibusiness_logo);
                overlayOmnibusinessLogo.setImageBitmap(getImage("OPI-Logo-AutoPay.jpg"));

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                         WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );

                params.gravity = Gravity.TOP | Gravity.START;

                windowManager.addView(whiteOverlayView, params);
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }

    public void showOverlay(String message){
        mainHandler.post(() -> {
            if(overlayView != null){
                removeOverlay();
            }

            try{
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                overlayView = inflater.inflate(R.layout.overlay_toast, null);

                TextView overlayText = overlayView.findViewById(R.id.overlay_text);
                overlayText.setText(message);
                ImageView overlayIconLogo = overlayView.findViewById(R.id.overlay_icon_logo);
                ImageView overlayIconStop = overlayView.findViewById(R.id.overlay_icon_stop);

                overlayIconLogo.setImageBitmap(getImage("opibiz_logo.png"));
                overlayIconStop.setImageBitmap(getImage("stop_icon.png"));

                overlayIconStop.setOnClickListener(v -> {
                    mediaProjectionHandler.stopMediaProjection();
                });

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );

                params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                windowManager.addView(overlayView, params);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void drawMultipleBoundingBox(List<Rect> allBounds){
        if(boundingBoxView != null){
            windowManager.removeView(boundingBoxView);
        }
        boundingBoxView = new BoundingBoxView(context, allBounds);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        windowManager.addView(boundingBoxView, params);
    }

    public void drawBoundingBox(Rect bounds){
        if(boundingBoxView1 != null){
            windowManager.removeView(boundingBoxView1);
        }

        boundingBoxView1 = new BoundingBoxView(context, bounds);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        windowManager.addView(boundingBoxView1, params);
    }


    private Bitmap getImage(String filename){
        try{
            InputStream inputStream = context.getAssets().open(filename);
            return BitmapFactory.decodeStream(inputStream);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void updateOverlayMessage(String message){
        mainHandler.post(() -> {
            if(overlayView != null){
                TextView toastText = overlayView.findViewById(R.id.overlay_text);
                toastText.setText(message);
            }
        });
    }

    public void removeOverlay(){
        mainHandler.post(() -> {
            if(overlayView != null){
                windowManager.removeView(overlayView);
                overlayView = null;
            }
        });
    }

    public void removeWhiteOverlay(){
        mainHandler.post(() -> {
           if(whiteOverlayView != null){
               windowManager.removeView(whiteOverlayView);
               whiteOverlayView = null;
           }
        });
    }

}
