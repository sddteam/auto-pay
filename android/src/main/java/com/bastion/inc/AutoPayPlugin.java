package com.bastion.inc;

import com.bastion.inc.Enums.SupportedApp;
import com.bastion.inc.Interfaces.ApplicationStateListener;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.Bridge;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@CapacitorPlugin(name = "AutoPay")
public class AutoPayPlugin extends Plugin implements ApplicationStateListener {
    private static final String TAG = "AutoPay";
    private Bridge bridge;
    private String ENABLE_ACCESSIBILITY_CBID;
    private String ENABLE_OVERLAY_CIBD;

    private final AutoPay implementation = new AutoPay();

    @Override
    public void load(){
        super.load();

        bridge = getBridge();
    }

    @PluginMethod
    public void startAutoPay(PluginCall call){
        String gateway = call.getString("url");
        String base64 = call.getString("base64");

        assert gateway != null;
        SupportedApp app = SupportedApp.valueOf(gateway.toUpperCase());
        String url = GatewayUrlProvider.getUrl(app);

        JSObject ret = new JSObject();
        String filename = implementation.generateQRFilename();

        try {
            Context context = getContext();
            if(!implementation.saveImage(base64, Environment.DIRECTORY_PICTURES, filename, context)) {
                throw new AutoPayException(AutoPayErrorCodes.FAILED_TO_SAVE_QR_CODE_ERROR);
            }else if(!implementation.openApp(url, context, getActivity())){
                throw new AutoPayException(AutoPayErrorCodes.GCASH_APP_NOT_INSTALLED_ERROR);
            }else if(!implementation.isAccessibilityServiceEnabled(context)){
                throw new AutoPayException(AutoPayErrorCodes.ACCESSIBILITY_SERVICE_NOT_ENABLED_ERROR);
            } else if (!Settings.canDrawOverlays(context)) {
                throw new AutoPayException(AutoPayErrorCodes.OVERLAY_PERMISSION_NOT_ENABLED_ERROR);
            }else{
                IntervalTaskHandler.getInstance(context).startIntervalTask(app, 1000);

                ret.put("value", true);
                call.resolve(ret);
            }
        }catch (AutoPayException e){
            call.reject(e.getLocalizedMessage(), e.getErrorCode());
        }catch (Exception e){
            call.reject(e.getLocalizedMessage(), null, e);
        }
    }

    @PluginMethod()
    public void checkAccessibility(PluginCall call){
        JSObject ret = new JSObject();
        try{
            ret.put("value", implementation.isAccessibilityServiceEnabled(getContext()));
            call.resolve(ret);
        }catch (Exception e){
            call.reject(e.getLocalizedMessage(), null, e);
        }

    }

    @PluginMethod()
    public void enableAccessibility(PluginCall call){
        try{
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            getContext().startActivity(intent);

            Toast.makeText(getContext(), "Please enable accessibility service", Toast.LENGTH_LONG).show();

            ENABLE_ACCESSIBILITY_CBID = call.getCallbackId();
            bridge.saveCall(call);
        }catch (Exception e){
            call.reject(e.getLocalizedMessage(), null, e);
        }
    }

    @PluginMethod()
    public void checkOverlayPermission(PluginCall call){
        JSObject ret = new JSObject();

        try{
            ret.put("value", Settings.canDrawOverlays(getContext()));
            call.resolve(ret);
        }catch (Exception e){
            call.reject(e.getLocalizedMessage(), null, e);
        }
    }

    @PluginMethod()
    public void enableOverlayPermission(PluginCall call){
        Context context = getContext();
        try{
            if(!Settings.canDrawOverlays(context)){
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }

            ENABLE_OVERLAY_CIBD = call.getCallbackId();
            bridge.saveCall(call);
        }catch (Exception e){
            call.reject(e.getLocalizedMessage(), null, e);
        }
    }

    @Override
    protected void handleOnResume(){
        super.handleOnResume();

        if(bridge != null){
            PluginCall accessibilityCall = bridge.getSavedCall(ENABLE_ACCESSIBILITY_CBID);
            PluginCall overlayCall = bridge.getSavedCall(ENABLE_OVERLAY_CIBD);

            if(accessibilityCall != null){
                try{
                    JSObject ret = new JSObject();
                    ret.put("value", implementation.isAccessibilityServiceEnabled(getContext()));
                    accessibilityCall.resolve(ret);
                }catch (Exception e){
                    accessibilityCall.reject(e.getLocalizedMessage(), null, e);
                }finally{
                    accessibilityCall.release(bridge);
                }
            }

            if(overlayCall != null){
                try{
                    JSObject ret = new JSObject();
                    ret.put("value", Settings.canDrawOverlays(getContext()));
                    overlayCall.resolve(ret);
                }catch (Exception e){
                    overlayCall.reject(e.getLocalizedMessage(), null, e);
                }finally {
                    overlayCall.release(bridge);
                }
            }
        }
    }

    @Override
    protected  void handleOnDestroy(){
        super.handleOnDestroy();

        IntervalTaskHandler.getInstance(getContext().getApplicationContext()).stopIntervalTask();
    }

    @Override
    public void onCompleted() {
        Log.d(TAG, "STOP NAVIGATION");

        IntervalTaskHandler.getInstance(getContext().getApplicationContext()).stopIntervalTask();
    }

    @Override
    public void onWarning(JSObject value) {
        notifyListeners("warning", value);
    }

    @Override
    public void onError(Exception e) {
        JSObject ret = new JSObject();
        ret.put("value", e);

        Log.d(TAG, Objects.requireNonNull(e.getLocalizedMessage()));

        notifyListeners("error", ret);
    }




}
