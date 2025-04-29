package com.bastion.inc;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.Bridge;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.projection.MediaProjectionManager;
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
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.File;
import java.io.FileOutputStream;
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
    private String NAVIGATE_GCASH_CIBD;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;

    private AutoPay implementation = new AutoPay();

    @Override
    public void load(){
        super.load();

        bridge = getBridge();
    }

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }
    @PluginMethod
    public void startAutoPay(PluginCall call){
        String url = call.getString("url");
        String base64 = call.getString("base64");
        JSObject ret = new JSObject();

        String filename = generateQRFilename();

        try {
            Context context = getContext();
            if(!saveImage(base64, Environment.DIRECTORY_PICTURES, filename)) {
                throw new AutoPayException(AutoPayErrorCodes.FAILED_TO_SAVE_QR_CODE_ERROR);
            }else if(!openApp(url)){
                throw new AutoPayException(AutoPayErrorCodes.GCASH_APP_NOT_INSTALLED_ERROR);
            }else if(!isAccessibilityServiceEnabled(context)){
                throw new AutoPayException(AutoPayErrorCodes.ACCESSIBILITY_SERVICE_NOT_ENABLED_ERROR);
            } else if (!Settings.canDrawOverlays(context)) {
                throw new AutoPayException(AutoPayErrorCodes.OVERLAY_PERMISSION_NOT_ENABLED_ERROR);
            }else{
                IntervalTaskHandler.getInstance(context).startIntervalTask(1000);

                ret.put("value", true);
                call.resolve(ret);
            }
        }catch (AutoPayException e){
            call.reject(e.getLocalizedMessage(), e.getErrorCode());
        }catch (Exception e){
            call.reject(e.getLocalizedMessage(), null, e);
        }
    }
    @PluginMethod
    public void stopNavigation(PluginCall call){
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

            if(!isAccessibilityServiceEnabled(context)){
                throw new AutoPayException(AutoPayErrorCodes.ACCESSIBILITY_SERVICE_NOT_ENABLED_ERROR);
            } else if (!Settings.canDrawOverlays(context)) {
                throw new AutoPayException(AutoPayErrorCodes.OVERLAY_PERMISSION_NOT_ENABLED_ERROR);
            }else{
                IntervalTaskHandler.getInstance(context).startIntervalTask(1000);

                JSObject ret = new JSObject();
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

        IntervalTaskHandler.getInstance(getContext().getApplicationContext()).stopIntervalTask();
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

    private boolean canOpenApp(String packageName){
        Context ctx = getContext().getApplicationContext();
        final PackageManager pm = ctx.getPackageManager();

        try{
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    private boolean openApp(String packageName){
        if(!canOpenApp(packageName)){
            return false;
        }

        final PackageManager manager = getContext().getPackageManager();
        Intent launchIntent = new Intent(Intent.ACTION_VIEW);
        launchIntent.setData(Uri.parse(packageName));
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try{
            getActivity().startActivity(launchIntent);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            launchIntent = manager.getLaunchIntentForPackage(packageName);
            try{
                getActivity().startActivity(launchIntent);
                return true;
            }catch (Exception epgk){
              epgk.printStackTrace();
              return false;
            }
        }
    }

    private boolean saveImage(String base64, String filepath, String filename) {
        try {
            byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);
            ContentResolver resolver = getContext().getContentResolver();
            Uri imageUri = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
                // Check if file already exists in MediaStore
                Uri existingFileUri = findExistingImageUri(resolver, filename);
                if (existingFileUri != null) {
                    resolver.delete(existingFileUri, null, null); // Delete old file
                    Log.d(TAG, "Existing file deleted: " + filename);
                }

                // Insert new file
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, filepath);

                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else { // Android 9 and below
                File imageFile = new File(Environment.getExternalStoragePublicDirectory(filepath), filename);
                if (imageFile.exists()) {
                    boolean deleted = imageFile.delete(); // Delete existing file
                    if (deleted) {
                        Log.d(TAG, "Existing file deleted: " + imageFile.getAbsolutePath());
                    }
                }
                imageUri = Uri.fromFile(imageFile);
            }

            // Save the image
            if (imageUri != null) {
                try (OutputStream outputStream = resolver.openOutputStream(imageUri)) {
                    if (outputStream != null) {
                        outputStream.write(imageBytes);
                        outputStream.flush();
                        Log.d(TAG, "Image saved successfully at: " + filepath);
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                Log.e(TAG, "Error saving image: " + filename);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to save image: " + e.getLocalizedMessage());
            return false;
        }
    }

    private String generateQRFilename(){
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return "qr_code_" + currentDate + ".png";
    }

    private Uri findExistingImageUri(ContentResolver resolver, String filename) {
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.DISPLAY_NAME + "=?";
        String[] selectionArgs = {filename};

        try (Cursor cursor = resolver.query(collection, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
            }
        }
        return null; // File does not exist
    }
}
