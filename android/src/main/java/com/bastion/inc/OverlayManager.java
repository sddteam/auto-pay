package com.bastion.inc;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class OverlayManager {
    private static OverlayManager instance;
    private WindowManager windowManager;
    private View overlayView;
    private Context context;
    private final Handler mainHandler;

    public OverlayManager(Context context){
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized OverlayManager getInstance(Context context){
        if(instance == null){
            instance = new OverlayManager(context);
        }

        return instance;
    }

    public void showOverlay(String message){
        mainHandler.post(() -> {
            if(overlayView != null){
                removeOverlay();
            }

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            overlayView = inflater.inflate(R.layout.overlay_toast, null);

            TextView toastText = overlayView.findViewById(R.id.toast_text);
            toastText.setText(message);

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
            );

            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            windowManager.addView(overlayView, params);
        });
    }

    public void updateOverlayMessage(String message){
        mainHandler.post(() -> {
            if(overlayView != null){
                TextView toastText = overlayView.findViewById(R.id.toast_text);
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
}
