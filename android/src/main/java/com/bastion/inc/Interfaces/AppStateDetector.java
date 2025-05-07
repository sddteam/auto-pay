package com.bastion.inc.Interfaces;

import android.view.accessibility.AccessibilityNodeInfo;

import com.bastion.inc.Enums.ActionState;

public interface AppStateDetector {
    ActionState detectState(AccessibilityNodeInfo rootNodeInfo);
    void handleState(ActionState state);
}
