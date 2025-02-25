package com.bastion.inc;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import org.opencv.core.Mat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccessibilityGestures implements GestureService {
    private final String TAG = "GESTURE";
    private final Context context;
    private final int GESTURE_CLICK_DELAY = 1000;
    private final int GESTURE_CLICK_DURATION = 100;
    private final int GESTURE_CLICK_WAIT_TIME = 3000;
    private static final Map<String, Integer> MONTH_ABBREVIATIONS = new HashMap<>() {{
        put("JAN", 1); put("FEB", 2); put("MAR", 3); put("APR", 4);
        put("MAY", 5); put("JUN", 6); put("JUL", 7); put("AUG", 8);
        put("SEP", 9); put("OCT", 10); put("NOV", 11); put("DEC", 12);
    }};

    public AccessibilityGestures(Context context){
        this.context = context;
    }
    @Override
    public void click(Location location, int times, ActionState state) {
        if(location == null){
            return;
        }
        Path path = new Path();
        path.moveTo(location.getX(), location.getY());
        GestureDescription.StrokeDescription strokeDescription = new GestureDescription.StrokeDescription(
                path,
                GESTURE_CLICK_DELAY,
                GESTURE_CLICK_DURATION
        );

        for (int i = 0; i < times; i++) {
            performGesture(strokeDescription);
        }
       /* OpenCVHandler openCVHandler = OpenCVHandler.getInstance(context.getApplicationContext());
        Mat screenMat = openCVHandler.takeScreenshot();

        if(screenMat != null){
            MatchResult matchResult;

            do{
                matchResult = openCVHandler.findBestMatch(screenMat, 0.1);

                Log.d(TAG, "RECHECK SCREEN - lastScreen " +  state + "currentScreen" + matchResult.templateName);

                if(matchResult.templateName.equals(state)){
                    waitFor(1000);
                    screenMat = openCVHandler.takeScreenshot();
                }
            }while (!matchResult.templateName.equals(state));


        }*/


        waitFor(GESTURE_CLICK_WAIT_TIME);
    }

    @Override
    public void find(String text) {
        AccessibilityNodeInfo rootNodeInfo  = AutoPayAccessibilityService.getInstance().getRootInActiveWindow();
        if(rootNodeInfo == null){
            return;
        }

        List<AccessibilityNodeInfo> buttons = rootNodeInfo.findAccessibilityNodeInfosByText(text);

        if(buttons != null && !buttons.isEmpty()){
            for (AccessibilityNodeInfo button :
                    buttons) {
                if(button.isClickable()){
                    button.performAction(AccessibilityNodeInfo.ACTION_CLICK);

                    //TODO: Draw bounding box to the button(debugging)
                    /*OverlayManager overlayManager = OverlayManager.getInstance(context);
                    Rect bounds = new Rect();
                    button.getBoundsInScreen(bounds);
                    if(bounds.width() > 0 && bounds.height() > 0){
                        overlayManager.drawBoundingBox(bounds);
                    }*/

                    return;
                }
            }
        }

        //TODO: Draw bounding box to the button(debugging)

       /* List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        getAllNodesRecursive(rootNodeInfo, allNodes);

        List<Rect> boundsList = new ArrayList<>();

        for (AccessibilityNodeInfo node :
                allNodes) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            if (bounds.width() > 0 && bounds.height() > 0) {
                boundsList.add(bounds);
            }
        }

        OverlayManager overlayManager = OverlayManager.getInstance(context);
        overlayManager.drawMultipleBoundingBox(boundsList);*/


        waitFor(GESTURE_CLICK_WAIT_TIME);
    }

    @Override
    public void ads() {
        AccessibilityNodeInfo rootNodeInfo  = AutoPayAccessibilityService.getInstance().getRootInActiveWindow();
        if(rootNodeInfo == null){
            return;
        }

        findByText(rootNodeInfo, "Remind me later");

        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        getAllNodesRecursive(rootNodeInfo, allNodes);

        for(AccessibilityNodeInfo node: allNodes){
            String text = node.getText() != null ? node.getText().toString() : "";

            if(text.contains("Remind me later")){
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

        waitFor(GESTURE_CLICK_WAIT_TIME);
    }

    @Override
    public void qr(String filename) {
        AccessibilityNodeInfo rootNodeInfo  = AutoPayAccessibilityService.getInstance().getRootInActiveWindow();
        if(rootNodeInfo == null){
            return;
        }

        List<Map.Entry<LocalDateTime, AccessibilityNodeInfo>> allNodes = new ArrayList<>();
        getAllDatesRecursive(rootNodeInfo, allNodes);


        if(!allNodes.isEmpty()){
            allNodes.stream()
                    .max(Map.Entry.comparingByKey()).ifPresent(latestNode -> {
                        AccessibilityNodeInfo node = latestNode.getValue();
                        if(node != null){
                            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        }
                    });
        }else{
            findByText(rootNodeInfo, filename);
        }

        waitFor(GESTURE_CLICK_WAIT_TIME);
    }

    private void findByText(AccessibilityNodeInfo rootNodeInfo, String text){
        List<AccessibilityNodeInfo> fileNodes = rootNodeInfo.findAccessibilityNodeInfosByText(text);
        if(fileNodes == null || fileNodes.isEmpty()){
            return;
        }

        for(AccessibilityNodeInfo fileNode : fileNodes){
            String contentDescription = fileNode.getContentDescription() != null ? fileNode.getContentDescription().toString() : "";
            boolean isPreviewButton = contentDescription.contains("Preview the file");

            if(!isPreviewButton){
                boolean clickResult = fileNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (clickResult) {
                    Log.i("Accessibility", "Node clicked successfully.");
                } else {
                    Log.e("Accessibility", "Failed to click the node.");
                }
            }
        }
    }

    @Override
    public void close() throws Exception {

    }

    private void waitFor(long waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void getAllNodesRecursive(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> allNodes) {
        try{
            if(node == null){
                return;
            }
            allNodes.add(node);

            int childCount = node.getChildCount();

            for (int i = 0; i < childCount; i++) {
                AccessibilityNodeInfo childNode = node.getChild(i);
                getAllNodesRecursive(childNode, allNodes);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    private void getAllDatesRecursive(AccessibilityNodeInfo node, List<Map.Entry<LocalDateTime, AccessibilityNodeInfo>> allNodes){
        String contentDesc = node.getContentDescription() != null ? node.getContentDescription().toString() : "";
        LocalDateTime localDateTime = extractDateTime(contentDesc);
        if(localDateTime != null){
            allNodes.add(new AbstractMap.SimpleEntry<>(localDateTime, node));
        }

        int childCount = node.getChildCount();

        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            getAllDatesRecursive(childNode, allNodes);
        }

    }

    private LocalDateTime extractDateTime(String text) {
        if(text.isEmpty()){
            return null;
        }
        text = text.replaceAll("[\u00A0\u202F\u200B]", " ");

        // Patterns for both formats
        String regex1 = "Photo taken on (\\w{3}) (\\d{1,2}), (\\d{4}), (\\d{1,2}):(\\d{2}):(\\d{2}) ?(AM|PM)";
        String regex2 = "Photo taken on (\\d{1,2}) (\\w{3}) (\\d{4}), (\\d{1,2}):(\\d{2}):(\\d{2}) ?(am|pm)";

        Pattern pattern1 = Pattern.compile(regex1);
        Pattern pattern2 = Pattern.compile(regex2);

        Matcher matcher1 = pattern1.matcher(text);
        Matcher matcher2 = pattern2.matcher(text);

        try {
            if (matcher1.find()) {
                return parseDateTime(matcher1, true);  // Format 1: "Jan 29, 2025"
            } else if (matcher2.find()) {
                return parseDateTime(matcher2, false); // Format 2: "20 Feb 2025"
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private LocalDateTime parseDateTime(Matcher matcher, boolean format1) {
        String monthAbbrev = format1 ? matcher.group(1).toUpperCase() : matcher.group(2).toUpperCase();
        int day = format1 ? Integer.parseInt(matcher.group(2)) : Integer.parseInt(matcher.group(1));
        int year = Integer.parseInt(matcher.group(3));
        int hour = Integer.parseInt(matcher.group(4));
        int minute = Integer.parseInt(matcher.group(5));
        int second = Integer.parseInt(matcher.group(6));
        String amPm = matcher.group(7);

        int monthNumber = MONTH_ABBREVIATIONS.get(monthAbbrev);

        // Convert 12-hour format to 24-hour format
        if (amPm.equalsIgnoreCase("PM") && hour != 12) {
            hour += 12;
        } else if (amPm.equalsIgnoreCase("AM") && hour == 12) {
            hour = 0;
        }

        return LocalDateTime.of(year, monthNumber, day, hour, minute, second);
    }

    private void performGesture(GestureDescription.StrokeDescription strokeDescription){
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(strokeDescription);

        AccessibilityService.GestureResultCallback callback = new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
            }
        };

        AutoPayAccessibilityService.getInstance().dispatchGesture(gestureBuilder.build(), callback, null);

    }
}
