package com.bastion.inc;

import android.app.Notification;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IntervalTaskHandler {
    private final String TAG = "IntervalTask";
    private static IntervalTaskHandler instance;
    private final Context context;
    private ScheduledExecutorService scheduler;
    private final Handler mainHandler;
    private Runnable task;
    private AccessibilityGestures accessibilityGestures;
    private OverlayManager overlayManager;

    public static synchronized IntervalTaskHandler getInstance(Context context){
        if(instance == null){
            instance = new IntervalTaskHandler(context);
        }

        return instance;
    }

    public IntervalTaskHandler(Context context){
        this.context = context;
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.accessibilityGestures = new AccessibilityGestures(context);
        this.overlayManager = OverlayManager.getInstance(context);
    }

    public void startIntervalTask(long intervalMillis){
        stopIntervalTask();

        scheduler = Executors.newScheduledThreadPool(1);

        task = () -> {
          mainHandler.post(() -> {
              ActionState state = accessibilityGestures.checkState();
              Log.d(TAG, "Task executed at: " + System.currentTimeMillis() + " State: " + state);

              if(state == ActionState.HOME) {
                  overlayManager.showWhiteOverlay();
                  mainHandler.postDelayed(() -> accessibilityGestures.find("QR"), 1000);
              } else if (state == ActionState.UPLOAD_QR){
                  accessibilityGestures.find("Upload QR");
              } else if (state == ActionState.SELECT_QR) {
                  String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                  String fileName = "qr_code_" + currentDate + ".png";
                  accessibilityGestures.qr(fileName);
              } else if (state == ActionState.PAYMENT) {
                  overlayManager.removeWhiteOverlay();
                  stopIntervalTask();
              } else if (state == ActionState.ADS) {
                  accessibilityGestures.find("Remind me later");
              }
          });
        };

        scheduler.scheduleWithFixedDelay(task, 0, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void stopIntervalTask(){
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

}
