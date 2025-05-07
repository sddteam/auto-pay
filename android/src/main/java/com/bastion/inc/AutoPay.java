package com.bastion.inc;

import android.app.Activity;
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
import android.util.Base64;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;

import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AutoPay {
    private static final String TAG = "AutoPayImplementation";

    public String generateQRFilename(){
        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        return "qr_code_" + currentDate + ".png";
    }

    public boolean saveImage(String base64, String filepath, String filename, Context context) {
        try {
            byte[] imageBytes = Base64.decode(base64, Base64.DEFAULT);
            ContentResolver resolver = context.getContentResolver();
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

    public boolean openApp(String packageName, Context context, Activity activity){
        if(!canOpenApp(packageName, context)){
            return false;
        }

        final PackageManager manager = context.getPackageManager();
        Intent launchIntent = new Intent(Intent.ACTION_VIEW);
        launchIntent.setData(Uri.parse(packageName));
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try{
            activity.startActivity(launchIntent);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            launchIntent = manager.getLaunchIntentForPackage(packageName);
            try{
                activity.startActivity(launchIntent);
                return true;
            }catch (Exception epgk){
                epgk.printStackTrace();
                return false;
            }
        }
    }

    public boolean isAccessibilityServiceEnabled(Context context){
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(am != null){
            String enabledServices = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String serviceName = context.getPackageName() + "/" + AutoPayAccessibilityService.class.getName();
            return enabledServices != null && enabledServices.contains(serviceName);
        }

        return false;
    }

    private boolean canOpenApp(String packageName, Context context){
        Context ctx = context.getApplicationContext();
        final PackageManager pm = ctx.getPackageManager();

        try{
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
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
