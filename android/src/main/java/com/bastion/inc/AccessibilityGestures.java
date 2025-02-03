package com.bastion.inc;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

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
    private final int GESTURE_CLICK_DELAY = 1000;
    private final int GESTURE_CLICK_DURATION = 100;
    private final int GESTURE_CLICK_WAIT_TIME = 3500;
    private static final Map<String, String> MONTH_ABBREVIATIONS = new HashMap<>();

    static {
        MONTH_ABBREVIATIONS.put("JAN", "JANUARY");
        MONTH_ABBREVIATIONS.put("FEB", "FEBRUARY");
        MONTH_ABBREVIATIONS.put("MAR", "MARCH");
        MONTH_ABBREVIATIONS.put("APR", "APRIL");
        MONTH_ABBREVIATIONS.put("MAY", "MAY");
        MONTH_ABBREVIATIONS.put("JUN", "JUNE");
        MONTH_ABBREVIATIONS.put("JUL", "JULY");
        MONTH_ABBREVIATIONS.put("AUG", "AUGUST");
        MONTH_ABBREVIATIONS.put("SEP", "SEPTEMBER");
        MONTH_ABBREVIATIONS.put("OCT", "OCTOBER");
        MONTH_ABBREVIATIONS.put("NOV", "NOVEMBER");
        MONTH_ABBREVIATIONS.put("DEC", "DECEMBER");
    }

    public AccessibilityGestures(){}
    @Override
    public void click(Location location, int times) {
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

        waitFor(GESTURE_CLICK_WAIT_TIME);
    }

    @Override
    public void find(String text) {
        AccessibilityNodeInfo rootNodeInfo  = AutoPayAccessibilityService.getInstance().getRootInActiveWindow();
        if(rootNodeInfo == null){
            return;
        }

        List<AccessibilityNodeInfo> allNodess = new ArrayList<>();
        getAllNodesRecursive(rootNodeInfo, allNodess);

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
            findByText(rootNodeInfo, text);
        }

        waitFor(GESTURE_CLICK_WAIT_TIME);
    }

    private void findByText(AccessibilityNodeInfo rootNodeInfo, String text){
        List<AccessibilityNodeInfo> fileNodes = rootNodeInfo.findAccessibilityNodeInfosByText(text);
        if(fileNodes == null || fileNodes.isEmpty()){
            Log.e("Accessibility", "File with name '" + text + "' not found.");
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
        allNodes.add(node);

        int childCount = node.getChildCount();

        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo childNode = node.getChild(i);
            getAllNodesRecursive(childNode, allNodes);
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

    private LocalDateTime extractDateTime(String text){
        String regex = "(\\w{3}) (\\d{1,2}), (\\d{4}), (\\d{1,2}):(\\d{2}):(\\d{2}) (AM|PM)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        try{
            if(matcher.find()){
                String month = matcher.group(0);
                String monthAbrev = month.substring(0, 3).toUpperCase();
                String fullMonth = MONTH_ABBREVIATIONS.get(monthAbrev);
                int day = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                int hour = Integer.parseInt(matcher.group(4));
                int minute = Integer.parseInt(matcher.group(5));
                int second = Integer.parseInt(matcher.group(6));
                String amPm = matcher.group(7);

                DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM");
                int monthNumber = Month.valueOf(fullMonth.toUpperCase()).getValue();

                if(amPm.equalsIgnoreCase("PM") && hour != 12){
                    hour += 12;
                } else if (amPm.equalsIgnoreCase("AM") && hour == 12) {
                    hour = 0;
                }

                return LocalDateTime.of(year, monthNumber, day, hour, minute, second);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
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
