package com.bastion.inc;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class AutoPayAccessibilityService extends AccessibilityService {

    private static AutoPayAccessibilityService instance;
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent){}

    @Override
    public void onInterrupt() {

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