package com.bastion.inc;

import android.content.Context;

import com.bastion.inc.Enums.SupportedApp;
import com.bastion.inc.Interfaces.AppStateDetector;
import com.bastion.inc.StateDetectors.GCashStateDetector;
import com.bastion.inc.StateDetectors.PayMayaStateDetector;

public class AppStateDetectorFactory {
    public static AppStateDetector getDetector(SupportedApp appName, Context context){
        switch (appName){
            case MAYA:
                return new PayMayaStateDetector(context);
            case GCASH:
                return new GCashStateDetector(context);
            default:
                throw new IllegalArgumentException("Unsupported app: " + appName);
        }
    }
}
