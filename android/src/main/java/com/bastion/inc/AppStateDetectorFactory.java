package com.bastion.inc;

import android.content.Context;

import com.bastion.inc.Interfaces.AppStateDetector;
import com.bastion.inc.StateDetectors.GCashStateDetector;
import com.bastion.inc.StateDetectors.PayMayaStateDetector;

public class AppStateDetectorFactory {
    public static AppStateDetector getDetector(String appName, Context context){
        switch (appName){
            case "PayMaya":
                return new PayMayaStateDetector(context);
            case "GCash":
                return new GCashStateDetector(context);
            default:
                throw new IllegalArgumentException("Unsupported app: " + appName);
        }
    }
}
