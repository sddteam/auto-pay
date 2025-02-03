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
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

@CapacitorPlugin(name = "AutoPay")
public class AutoPayPlugin extends Plugin implements ApplicationStateListener {
    private static final String TAG = "AutoPay";
    private Bridge bridge;
    private String ENABLE_ACCESSIBILITY_CBID;
    private String NAVIGATE_GCASH_CIBD;
    private MediaProjectionManager mediaProjectionManager;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;
    private MediaProjectionHandler mediaProjectionHandler;
    private OpenCVHandler openCVHandler;

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
        openCVHandler = new OpenCVHandler(getContext());
        openCVHandler.setApplicationStateListener(this);
        mediaProjectionHandler = new MediaProjectionHandler(getContext().getSystemService(MediaProjectionManager.class), getContext());
        mediaProjectionHandler.setProjectionImageListener(openCVHandler);
        mediaProjectionHandler.setApplicationStateListener(this);

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
                            e.printStackTrace();
                        }
                    }
                }else{
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
            if (!isAccessibilityServiceEnabled(getContext())) {
                throw new AutoPayException(AutoPayErrorCodes.ACCESSIBILITY_SERVICE_NOT_ENABLED_ERROR);
            } else {
                Intent serviceIntent = new Intent(getContext(), AutoPayScreenCaptureService.class);
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

    @Override
    protected void handleOnResume(){
        super.handleOnResume();

        if(bridge != null){
            PluginCall savedCall = bridge.getSavedCall(ENABLE_ACCESSIBILITY_CBID);

            if(savedCall != null){
                try{
                    JSObject ret = new JSObject();
                    ret.put("value", isAccessibilityServiceEnabled(getContext()));
                    savedCall.resolve(ret);
                }catch (Exception e){
                    savedCall.reject(e.getLocalizedMessage(), null, e);
                }finally{
                    savedCall.release(bridge);
                }
            }
        }
    }

    @Override
    protected  void handleOnDestroy(){
        super.handleOnDestroy();

        mediaProjectionHandler.stopMediaProjection();
    }

    private boolean isAccessibilityServiceEnabled(Context context){
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(am != null){
            String enabledServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String serviceName = context.getPackageName() + "/" + AutoPayAccessibilityService.class.getName();
            Log.d("AccessibilityCheck", "Service name: " + serviceName);
            return enabledServices != null && enabledServices.contains(serviceName);
        }

        return false;
    }

    @Override
    public void onCompleted() {
        Log.d(TAG, "STOP NAVIGATION");
        //mediaProjectionHandler.stopMediaProjection();
    }

    @Override
    public void onWarning(JSObject value) {
        notifyListeners("warning", value);
    }

    @Override
    public void onError(Exception e) {
        JSObject ret = new JSObject();
        ret.put("value", e);

        notifyListeners("error", ret);
    }
}
