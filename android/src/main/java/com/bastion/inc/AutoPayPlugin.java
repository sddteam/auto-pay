package com.bastion.inc;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.Bridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.util.Objects;

@CapacitorPlugin(name = "AutoPay")
public class AutoPayPlugin extends Plugin implements ApplicationStateListener {
    private static final String TAG = "AutoPay";
    private Bridge bridge;
    private String ENABLE_ACCESSIBILITY_CBID;
    private String ENABLE_OVERLAY_CIBD;
    private String NAVIGATE_GCASH_CIBD;
    private MediaProjectionManager mediaProjectionManager;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;
    private MediaProjectionHandler mediaProjectionHandler;
    private OpenCVHandler openCVHandler;
    private Intent serviceIntent;

    private AutoPay implementation = new AutoPay();

    private void cleanupResources() {
        // Stop the screen capture launcher
        if (screenCaptureLauncher != null) {
            // Handle any additional cleanup related to screen capture
        }

        Log.d("AutoPayPlugin", "Resources cleaned up.");
    }

    @Override
    public void load(){
        super.load();

        bridge = getBridge();

        screenCaptureLauncher = getActivity().registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                PluginCall savedCall = bridge.getSavedCall(NAVIGATE_GCASH_CIBD);
                JSObject ret = new JSObject();

                if(savedCall == null){
                    return;
                }
                if (result.getResultCode() == Activity.RESULT_OK){
                    ret.put("value", true);
                    savedCall.resolve(ret);
                    Intent data = result.getData();

                    if(data != null){
                        try{
                            mediaProjectionHandler.startMediaProjection(getContext(), data);
                        }catch(Exception e){
                            throw new RuntimeException(e);
                        }
                    }
                }else{
                    if(serviceIntent != null){
                        getContext().stopService(serviceIntent);
                    }

                    ret.put("value", false);
                    savedCall.resolve(ret);
                }

                savedCall.release(bridge);
            }
        );
    }

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }
    @PluginMethod
    public void stopNavigation(PluginCall call){
        cleanupResources();

        JSObject ret = new JSObject();
        ret.put("value", "success");
        call.resolve(ret);
    }
    @PluginMethod
    public void performGesture(PluginCall call){
        float coordX = call.getFloat("x");
        float coordY = call.getFloat("y");


        JSObject ret = new JSObject();
        ret.put("value", coordX);
        call.resolve(ret);
    }
    @PluginMethod()
    public void navigateGCash(PluginCall call){
        try {
            Context context = getContext();
            openCVHandler = OpenCVHandler.getInstance(context.getApplicationContext());
            openCVHandler.setApplicationStateListener(this);
            mediaProjectionHandler = MediaProjectionHandler.getInstance(context.getApplicationContext());
            mediaProjectionHandler.setProjectionImageListener(openCVHandler);
            mediaProjectionHandler.setApplicationStateListener(this);

            if (!isAccessibilityServiceEnabled(context)) {
                throw new AutoPayException(AutoPayErrorCodes.ACCESSIBILITY_SERVICE_NOT_ENABLED_ERROR);
            } else if (!Settings.canDrawOverlays(context)) {
                throw new AutoPayException(AutoPayErrorCodes.OVERLAY_PERMISSION_NOT_ENABLED_ERROR);
            } else {
                serviceIntent = new Intent(getContext(), AutoPayScreenCaptureService.class);
                getContext().startForegroundService(serviceIntent);

                mediaProjectionManager = (MediaProjectionManager) getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

                if (mediaProjectionManager != null) {
                    Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                    screenCaptureLauncher.launch(captureIntent);

                    NAVIGATE_GCASH_CIBD = call.getCallbackId();
                    bridge.saveCall(call);
                } else {
                    throw new AutoPayException(AutoPayErrorCodes.MEDIA_PROJECTION_FAILED_TO_INITIALIZE_ERROR);
                }
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
            ret.put("value", isAccessibilityServiceEnabled(getContext()));
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
                    ret.put("value", isAccessibilityServiceEnabled(getContext()));
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

        if(mediaProjectionHandler != null){
            mediaProjectionHandler.stopMediaProjection();
        }
    }

    private boolean isAccessibilityServiceEnabled(Context context){
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(am != null){
            String enabledServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String serviceName = context.getPackageName() + "/" + AutoPayAccessibilityService.class.getName();
            return enabledServices != null && enabledServices.contains(serviceName);
        }

        return false;
    }

    @Override
    public void onCompleted() {
        Log.d(TAG, "STOP NAVIGATION");
        if(mediaProjectionHandler != null){
            mediaProjectionHandler.stopMediaProjection();
        }
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
