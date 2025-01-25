package com.bastion.inc;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
public class AutoPayAccessibilityService extends AccessibilityService {

    private static AutoPayAccessibilityService instance;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

    public void performTap(float x, float y) {
        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();

                // Define the path for the gesture
                Path path = new Path();
                path.moveTo(x, y); // Example coordinates
                gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));

                // Dispatch the gesture
                dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        super.onCompleted(gestureDescription);
                        Log.d("Gesture", "Gesture completed successfully");
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        super.onCancelled(gestureDescription);
                        Log.d("Gesture", "Gesture was cancelled");
                    }
                }, null);
            } else {
                Log.e("Gesture", "Gesture API requires Android Nougat or higher");
            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

        instance = this;

        Log.d("MyAccessibilityService", "Service connected");
    }

    public static AutoPayAccessibilityService getInstance(){
        return instance;
    }
}