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

public class GCashStateDetector extends BaseAppStateDetector{
    private final OverlayManager overlayManager;
    private final Context context;

    public GCashStateDetector(Context context){
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

        List<AccessibilityNodeInfo> loginNode = getNodeByText("Enter your MPIN");
        List<AccessibilityNodeInfo> homeNode = getNodeByText("Bills");
        List<AccessibilityNodeInfo> uploadNode = getNodeByText("Upload QR");
        List<AccessibilityNodeInfo> selectNode = getNodeByText("Photos");
        List<AccessibilityNodeInfo> paymentNode = getNodeByText("Amount Due");
        List<AccessibilityNodeInfo> adsNode = getNodeByText("Remind me later");
        List<AccessibilityNodeInfo> errorNode = getNodeByText("Please make sure code is clear.");

        if(!loginNode.isEmpty()){
            return ActionState.LOGIN;
        } else if (!homeNode.isEmpty()) {
            return ActionState.HOME;
        } else if (!uploadNode.isEmpty()) {
            return ActionState.UPLOAD_QR;
        } else if (!selectNode.isEmpty()) {
            return ActionState.SELECT_QR;
        } else if (!paymentNode.isEmpty()) {
            return ActionState.PAYMENT;
        } else if (!adsNode.isEmpty()) {
            return ActionState.ADS;
        } else if (!errorNode.isEmpty()) {
            return ActionState.ERROR;
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
                    click(NodeInfoAttribute.Text, "QR");
                }, 1000);
                break;
            case ActionState.UPLOAD_QR:
                click(NodeInfoAttribute.Text, "Upload QR");
                break;
            case ActionState.SELECT_QR:
                String filename = generateQRFilename();
                click(NodeInfoAttribute.DateTime, filename);
                break;
            case ActionState.PAYMENT:
                Log.d("GCashStateDetector", "STOP TASK");
                overlayManager.removeWhiteOverlay();
                IntervalTaskHandler.getInstance(context).stopIntervalTask();
                IntervalTaskHandler.getInstance(context).exitActivity();

                deleteFile(generateQRFilename(), Environment.DIRECTORY_PICTURES);
                break;
            case ActionState.ADS:
                click(NodeInfoAttribute.Text, "Remind me later");
                break;
            case  ActionState.ERROR:
                Log.d("GCashStateDetector", "STOP TASK: ERROR");
                overlayManager.removeWhiteOverlay();
                IntervalTaskHandler.getInstance(context).stopIntervalTask();
                IntervalTaskHandler.getInstance(context).exitActivity();
                break;
            default:
                Log.d("GCashStateDetector", "Unhandled state: " + state);
        }
    }
}
