package com.bastion.inc;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class AutoPayAccessibilityService extends AccessibilityService {
    public static final String CHANNEL_ID = "paymate_accessibility_service_channel";
    public static final int NOTIFICATION_ID = 101;
    private static final String TAG = "PayMateAccessibilityService";

    private static AutoPayAccessibilityService instance;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent){}

    private final Handler overlayHandler = new Handler(Looper.getMainLooper());
    private final Runnable overlayCheckRunnable = new Runnable() {
      @Override
      public void run() {
        if(Settings.canDrawOverlays(AutoPayAccessibilityService.this)){

          ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
          List<ActivityManager.RunningTaskInfo> taskList = activityManager.getRunningTasks(Integer.MAX_VALUE);

          if (taskList != null && !taskList.isEmpty()) {
            for (ActivityManager.RunningTaskInfo task : taskList) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (task.baseIntent != null && task.baseIntent.getComponent() != null) {
                  String packageName = task.baseIntent.getComponent().getPackageName();
                  Log.d("TASK", "Base Intent Package: " + packageName);

                  if (packageName.equals(getPackageName())) {
                    // Move app to front
                    Intent launchIntent = new Intent(task.baseIntent);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(launchIntent);
                    return;
                  }
                }
              }
            }
          }

          overlayHandler.removeCallbacks(this);
        }else{
          overlayHandler.postDelayed(this, 1000);
        }
      }
    };

    @Override
    public void onInterrupt() {

    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

        instance = this;

        if(!Settings.canDrawOverlays(this)){
            Intent overlayIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getPackageName()));
            overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(overlayIntent);

            Toast.makeText(AutoPayAccessibilityService.this, "Now enable 'Draw over other apps' permission", Toast.LENGTH_LONG).show();
            overlayHandler.postDelayed(overlayCheckRunnable, 1000);
        }else{
          ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
          List<ActivityManager.RunningTaskInfo> taskList = activityManager.getRunningTasks(Integer.MAX_VALUE);

          if (taskList != null && !taskList.isEmpty()) {
            for (ActivityManager.RunningTaskInfo task : taskList) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (task.baseIntent != null && task.baseIntent.getComponent() != null) {
                  String packageName = task.baseIntent.getComponent().getPackageName();
                  Log.d("TASK", "Base Intent Package: " + packageName);

                  if (packageName.equals(getPackageName())) {
                    // Move app to front
                    Intent launchIntent = new Intent(task.baseIntent);
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(launchIntent);
                    return;
                  }
                }
              }
            }
          }
        }

        instance = this;

        Log.d("MyAccessibilityService", "Service connected");
    }

    public static AutoPayAccessibilityService getInstance(){
        return instance;
    }

    private void createNotificataionChannel(){
        CharSequence name = "Accessibility Service Channel";
        String description = "Notifiactions from the accessibility service";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
}
