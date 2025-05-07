package com.bastion.inc.StateDetectors;

import android.os.Environment;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.bastion.inc.AutoPayAccessibilityService;
import com.bastion.inc.Interfaces.AppStateDetector;
import com.bastion.inc.Enums.NodeInfoAttribute;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseAppStateDetector implements AppStateDetector {
    private final String TAG = "AppStateDetector";
    private final int GESTURE_CLICK_WAIT_TIME = 3000;

    protected void click(NodeInfoAttribute attribute, String value) {
        List<AccessibilityNodeInfo> nodes = new ArrayList<>();

        if(attribute.equals(NodeInfoAttribute.Text)){
            nodes = getNodeByText(value);
        } else if (attribute.equals(NodeInfoAttribute.ContentDescription)) {
            nodes = getNodeByText(value);
        } else if (attribute.equals(NodeInfoAttribute.DrawingOrder)){
            int order = Integer.parseInt(value);
            nodes = getNodeByDrawingOrder(order);
        }else if (attribute.equals(NodeInfoAttribute.DateTime)){
            nodes = getNodeByDateTime(value);
        }

        assert nodes != null;

        if(nodes.isEmpty()){
            return;
        }

        for (AccessibilityNodeInfo node :
                nodes) {
            if(!tryClick(node)){
                AccessibilityNodeInfo parent = node.getParent();
                while(parent != null && !tryClick(parent)){
                    parent = parent.getParent();
                }
            }
        }

        waitFor(GESTURE_CLICK_WAIT_TIME);
    }
    protected List<AccessibilityNodeInfo> getNodeByDrawingOrder(int drawingOrder){
        AccessibilityNodeInfo rootNodeInfo  = AutoPayAccessibilityService.getInstance().getRootInActiveWindow();
        if(rootNodeInfo == null){
            return null;
        }

        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        List<AccessibilityNodeInfo> foundNodes = new ArrayList<>();

        getAllNodesRecursive(rootNodeInfo, allNodes);

        for (AccessibilityNodeInfo node :
                allNodes) {
            if(node.getDrawingOrder() == drawingOrder){
                foundNodes.add(node);
            }
        }

        return foundNodes;

    }

    protected List<AccessibilityNodeInfo> getNodeByDateTime(String value){
        AccessibilityNodeInfo rootNodeInfo = AutoPayAccessibilityService.getInstance().getRootInActiveWindow();
        if(rootNodeInfo == null){
            return null;
        }

        List<Map.Entry<LocalDateTime, AccessibilityNodeInfo>> allNodes = new ArrayList<>();
        getAllDatesRecursive(rootNodeInfo, allNodes);

        if(allNodes.isEmpty()){
            return getNodeByText(value);
        }

        return allNodes.stream()
                .max(Map.Entry.comparingByKey()) // Find the entry with the latest LocalDateTime
                .map(entry -> List.of(entry.getValue())) // Extract the AccessibilityNodeInfo
                .orElse(null);
    }

    protected List<AccessibilityNodeInfo> getNodeByText(String text){
        AccessibilityNodeInfo rootNodeInfo  = AutoPayAccessibilityService.getInstance().getRootInActiveWindow();

        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        List<AccessibilityNodeInfo> foundNodes = new ArrayList<>();

        getAllNodesRecursive(rootNodeInfo, allNodes);

        for (AccessibilityNodeInfo node :
                allNodes) {
            String name = node.getText() != null && !node.getText().toString().isEmpty()
                    ? node.getText().toString()
                    : (node.getContentDescription() != null ? node.getContentDescription().toString() : "");

            if(name.equals(text)){
                foundNodes.add(node);
            }
        }

        return foundNodes;
    }

    protected List<AccessibilityNodeInfo> getNodeByTitlePane(String text){
        AccessibilityNodeInfo rootNodeInfo  = AutoPayAccessibilityService.getInstance().getRootInActiveWindow();

        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        List<AccessibilityNodeInfo> foundNodes = new ArrayList<>();

        getAllNodesRecursive(rootNodeInfo, allNodes);

        for (AccessibilityNodeInfo node :
                allNodes) {
            String name = node.getPaneTitle() != null ? node.getPaneTitle().toString() : "";

            if(name.equals(text)){
                foundNodes.add(node);
            }
        }

        return foundNodes;
    }

    protected void getAllNodesRecursive(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> allNodes) {
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
        String regex1 = "Photo taken on (\\w{3}) (\\d{1,2}), (\\d{4}) (\\d{1,2}):(\\d{2}) ?(AM|PM)";
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
        Map<String, Integer> MONTH_ABBREVIATIONS = new HashMap<>() {{
            put("JAN", 1); put("FEB", 2); put("MAR", 3); put("APR", 4);
            put("MAY", 5); put("JUN", 6); put("JUL", 7); put("AUG", 8);
            put("SEP", 9); put("OCT", 10); put("NOV", 11); put("DEC", 12);
        }};

        String monthAbbrev = format1 ? matcher.group(1).toUpperCase() : matcher.group(2).toUpperCase();
        int day = format1 ? Integer.parseInt(matcher.group(2)) : Integer.parseInt(matcher.group(1));
        int year = Integer.parseInt(matcher.group(3));
        int hour = Integer.parseInt(matcher.group(4));
        int minute = Integer.parseInt(matcher.group(5));
        String amPm = matcher.group(6);

        int monthNumber = MONTH_ABBREVIATIONS.get(monthAbbrev);

        // Convert 12-hour format to 24-hour format
        if (amPm.equalsIgnoreCase("PM") && hour != 12) {
            hour += 12;
        } else if (amPm.equalsIgnoreCase("AM") && hour == 12) {
            hour = 0;
        }

        return LocalDateTime.of(year, monthNumber, day, hour, minute);
    }

    private boolean tryClick(AccessibilityNodeInfo node) {
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }
    private void waitFor(long waitTime) {
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected String generateQRFilename(){
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return "qr_code_" + currentDate + ".png";
    }

    protected void deleteFile(String filename, String filepath){
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
