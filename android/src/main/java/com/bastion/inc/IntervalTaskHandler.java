package com.bastion.inc;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.bastion.inc.Enums.ActionState;
import com.bastion.inc.Enums.SupportedApp;
import com.bastion.inc.Interfaces.AppStateDetector;

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
    private final OverlayManager overlayManager;
    private int timeOutCount = 0;
    private static final int MAX_TIMEOUT_COUNT = 60;

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
        this.overlayManager = OverlayManager.getInstance(context);
    }

    public void startIntervalTask(SupportedApp app, long intervalMillis){
        stopIntervalTask();

        scheduler = Executors.newScheduledThreadPool(1);

        task = () -> {
          mainHandler.post(() -> {
              AccessibilityNodeInfo rootNodeInfo = AutoPayAccessibilityService.getInstance().getRootInActiveWindow();
              AppStateDetector detector = AppStateDetectorFactory.getDetector(app, context);

              ActionState state  = detector.detectState(rootNodeInfo);

              Log.d(TAG, state.toString());

              if(state == ActionState.UNKNOWN){
                  timeOutCount++;

                  if(timeOutCount >= MAX_TIMEOUT_COUNT){
                      stopIntervalTask();
                      overlayManager.removeWhiteOverlay();
                      return;
                  }
              }else{
                  timeOutCount = 0;
              }

              detector.handleState(state);
          });
        };

        scheduler.scheduleWithFixedDelay(task, 0, intervalMillis, TimeUnit.MILLISECONDS);
    }

    public void stopIntervalTask(){
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public void exitActivity(){
        Activity activity = (Activity) context;
        activity.finishAffinity();
        activity.finishAndRemoveTask();
        System.exit(0);
    }


}
