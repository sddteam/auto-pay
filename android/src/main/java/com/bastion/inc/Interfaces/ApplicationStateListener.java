package com.bastion.inc.Interfaces;

import com.getcapacitor.JSObject;

public interface ApplicationStateListener {
    void onCompleted();
    void onWarning(JSObject value);
    void onError(Exception e);
}
