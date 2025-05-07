package com.bastion.inc;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.bastion.inc.Enums.ActionState;
import com.bastion.inc.Enums.NodeInfoAttribute;
import com.bastion.inc.Interfaces.AppStateDetector;
import com.bastion.inc.Interfaces.GestureService;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private OverlayManager overlayManager;

    public AccessibilityGestures(Context context){
        this.context = context;
        this.overlayManager = OverlayManager.getInstance(context);
    }
    @Override
    public void click(NodeInfoAttribute attribute, String value) {
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
                }else{
                    AccessibilityNodeInfo parent = button.getParent();
                    if(parent != null){
                        parent.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                        parent.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                        parent.performAction(AccessibilityNodeInfo.ACTION_SELECT);
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }


                    /*int statusBarHeight = getStatusBarHeight(context);
                    Rect bounds = new Rect();
                    button.getBoundsInScreen(bounds);

                    bounds.top -= statusBarHeight;
                    bounds.bottom -= statusBarHeight;

                    click(getLocation(bounds));*/
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

    private List<AccessibilityNodeInfo> getNodeByDrawingOrder(int drawingOrder){
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

    private List<AccessibilityNodeInfo> getNodeByDateTime(String value){
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

    private List<AccessibilityNodeInfo> getNodeByText(String text){
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

    private List<AccessibilityNodeInfo> getNodeByTitlePane(String text){
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

    @Override
    public void qr(String filename) {
        AccessibilityNodeInfo rootNodeInfo  = AutoPayAccessibilityService.getInstance().getRootInActiveWindow();
        if(rootNodeInfo == null){
            return;
        }

        List<Map.Entry<LocalDateTime, AccessibilityNodeInfo>> allNodes = new ArrayList<>();
        getAllDatesRecursive(rootNodeInfo, allNodes);

        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        getAllNodesRecursive(rootNodeInfo, nodes);

        if(!allNodes.isEmpty()){
            allNodes.stream()
                .max(Map.Entry.comparingByKey()).ifPresent(latestNode -> {
                    AccessibilityNodeInfo node = latestNode.getValue();
                if (node != null) {
                  if (!tryClick(node)) {
                    AccessibilityNodeInfo parent = node.getParent();
                    while (parent != null && !tryClick(parent)) {
                      parent = parent.getParent(); // Move up the hierarchy
                    }
                  }
                }
              });
        }else{
            findByText(rootNodeInfo, filename);
        }

        OverlayManager overlayManager = OverlayManager.getInstance(context);
        overlayManager.removeWhiteOverlay();

        waitFor(GESTURE_CLICK_WAIT_TIME);
    }

    private boolean tryClick(AccessibilityNodeInfo node) {
      return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    @Override
    public void close() throws Exception {

    }

    private void drawBoundingBox(List<AccessibilityNodeInfo> nodes){
        List<Rect> boundsList = new ArrayList<>();
        int statusBarHeight = getStatusBarHeight(context);

        for (AccessibilityNodeInfo node :
                nodes) {
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);

            bounds.top -= statusBarHeight;
            bounds.bottom -= statusBarHeight;

            if (bounds.width() > 0 && bounds.height() > 0) {
                boundsList.add(bounds);
            }
        }

        overlayManager.drawMultipleBoundingBox(boundsList);
    }

    private int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
        }
        return statusBarHeight;
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
}
