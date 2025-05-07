package com.bastion.inc.StateDetectors;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.bastion.inc.Enums.ActionState;
import com.bastion.inc.IntervalTaskHandler;
import com.bastion.inc.Enums.NodeInfoAttribute;
import com.bastion.inc.OverlayManager;

import java.util.ArrayList;
import java.util.List;

public class PayMayaStateDetector extends BaseAppStateDetector{

    private final OverlayManager overlayManager;
    private final Context context;

    public PayMayaStateDetector(Context context){
        this.overlayManager = OverlayManager.getInstance(context);
        this.context = context;
    }
    @Override
    public ActionState detectState(AccessibilityNodeInfo rootNodeInfo) {
        if(rootNodeInfo == null){
            return ActionState.UNKNOWN;
        }

        List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
        getAllNodesRecursive(rootNodeInfo, allNodes);

        //drawBoundingBox(allNodes);

        List<AccessibilityNodeInfo> homeNode = getNodeByText("Wallet");
        List<AccessibilityNodeInfo> servicesNode = getNodeByText("Services");
        List<AccessibilityNodeInfo> uploadNode = getNodeByText("Scan a QR code");
        List<AccessibilityNodeInfo> selectNode = getNodeByText("Photos");
        List<AccessibilityNodeInfo> paymentNode = getNodeByTitlePane("Pay using");

        if (!homeNode.isEmpty()) {
            return ActionState.HOME;
        } else if (!servicesNode.isEmpty()) {
            return ActionState.SERVICES;
        } else if (!uploadNode.isEmpty()) {
            return ActionState.UPLOAD_QR;
        } else if (!selectNode.isEmpty()) {
            return ActionState.SELECT_QR;
        } else if (!paymentNode.isEmpty()) {
            return ActionState.PAYMENT;
        } else if (allNodes.size() == 3) {
            return ActionState.ADS;
        } else {
            return ActionState.UNKNOWN;
        }
    }

    @Override
    public void handleState(ActionState state) {
        switch (state){
            case ActionState.HOME:
                overlayManager.showWhiteOverlay();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    click(NodeInfoAttribute.Text, "More");
                }, 1000);
                break;
            case ActionState.SERVICES:
                click(NodeInfoAttribute.Text, "Pay with QR");
                break;
            case ActionState.UPLOAD_QR:
                click(NodeInfoAttribute.Text, "Upload QR");
                break;
            case ActionState.SELECT_QR:
                String filename = generateQRFilename();
                click(NodeInfoAttribute.DateTime, filename);
                break;
            case ActionState.PAYMENT:
                Log.d("PayMayaStateDetector", "STOP TASK");
                overlayManager.removeWhiteOverlay();
                IntervalTaskHandler.getInstance(context).stopIntervalTask();
                IntervalTaskHandler.getInstance(context).exitActivity();

                deleteFile(generateQRFilename(), Environment.DIRECTORY_PICTURES);
                break;
            case ActionState.ADS:
                click(NodeInfoAttribute.DrawingOrder, "2");
                break;
            case  ActionState.ERROR:
                Log.d("PayMayaStateDetector", "STOP TASK: ERROR");
                overlayManager.removeWhiteOverlay();
                IntervalTaskHandler.getInstance(context).stopIntervalTask();
                IntervalTaskHandler.getInstance(context).exitActivity();
                break;
            default:
                Log.d("PayMayaStateDetector", "Unhandled state: " + state);
        }
    }
}
