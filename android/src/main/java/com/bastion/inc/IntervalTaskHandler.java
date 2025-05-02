package com.bastion.inc;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
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
            ActionState state = accessibilityGestures.mayaState();
            Log.d(TAG, "Task executed at: " + System.currentTimeMillis() + " State: " + state);

            if(state == ActionState.HOME) {
              overlayManager.showWhiteOverlay();
              mainHandler.postDelayed(() -> accessibilityGestures.find("More"), 1000);
            } else if (state == ActionState.SERVICES){
              accessibilityGestures.find("Pay with QR");
            } else if (state == ActionState.UPLOAD_QR){
              accessibilityGestures.find("Upload QR");
            } else if (state == ActionState.SELECT_QR) {
              String filename = generateQRFilename();
              accessibilityGestures.qr(filename);
            } else if (state == ActionState.PAYMENT) {
              Log.d(TAG, "STOP TASK");
              overlayManager.removeWhiteOverlay();
              stopIntervalTask();
              exitActivity();

              //TODO: REMOVE QR IMAGE
              String filename = generateQRFilename();
              deleteFile(filename, Environment.DIRECTORY_PICTURES);
            } else if (state == ActionState.ADS) {
              accessibilityGestures.find("Remind me later");
            } else if(state == ActionState.ERROR){
              overlayManager.removeWhiteOverlay();
              stopIntervalTask();
              exitActivity();
            }
          });
        };

//        task = () -> {
//          mainHandler.post(() -> {
//              ActionState state = accessibilityGestures.checkState();
//              Log.d(TAG, "Task executed at: " + System.currentTimeMillis() + " State: " + state);
//
//              if(state == ActionState.HOME) {
//                  //overlayManager.showWhiteOverlay();
//                  mainHandler.postDelayed(() -> accessibilityGestures.find("QR"), 1000);
//              } else if (state == ActionState.UPLOAD_QR){
//                  accessibilityGestures.find("Upload QR");
//              } else if (state == ActionState.SELECT_QR) {
//                  String filename = generateQRFilename();
//                  accessibilityGestures.qr(filename);
//              } else if (state == ActionState.PAYMENT) {
//                  overlayManager.removeWhiteOverlay();
//                  stopIntervalTask();
//                  exitActivity();
//
//                  //TODO: REMOVE QR IMAGE
//                  String filename = generateQRFilename();
//                  deleteFile(filename, Environment.DIRECTORY_PICTURES);
//              } else if (state == ActionState.ADS) {
//                  accessibilityGestures.find("Remind me later");
//              } else if(state == ActionState.ERROR){
//                  overlayManager.removeWhiteOverlay();
//                  stopIntervalTask();
//                  exitActivity();
//              }
//          });
//        };

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

    private String generateQRFilename(){
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return "qr_code_" + currentDate + ".png";
    }

    private void deleteFile(String filename, String filepath){
        try{
            String documentsDir = Environment.getExternalStoragePublicDirectory(filepath).getAbsolutePath();
            File qrFile = new File(documentsDir, filename);

            if(qrFile.exists()){
                boolean deleted = qrFile.delete();
                if(deleted){
                    Log.d(TAG, "QR file deleted: " + filename);
                }else{
                    Log.d(TAG, "Failed to delete QR file: " + filename);
                }
            }else{
                Log.d(TAG, "QR file not found: "+ filename);
            }
        }catch (Exception e){
            Log.e(TAG, "Error deleting QR file: " + e.getMessage());
        }
    }
}
