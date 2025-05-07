package com.bastion.inc;

import com.bastion.inc.Enums.SupportedApp;

import java.util.HashMap;
import java.util.Map;

public class GatewayUrlProvider {
    private static final Map<SupportedApp, String> gatewayUrls = new HashMap<>();

    static{
        gatewayUrls.put(SupportedApp.GCASH, "com.globe.gcash.android");
        gatewayUrls.put(SupportedApp.MAYA, "com.paymaya");
        gatewayUrls.put(SupportedApp.BDO, "com.bdo.mobilebanking");
        gatewayUrls.put(SupportedApp.METROBANK, "com.metrobank.mobile");
    }

    public static String getUrl(SupportedApp app){
        return gatewayUrls.getOrDefault(app, "default.url");
    }
}
