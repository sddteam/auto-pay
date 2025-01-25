package com.bastion.inc;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.accessibility.AccessibilityManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@CapacitorPlugin(name = "AutoPay")
public class AutoPayPlugin extends Plugin {
    private int captureInterval = 10000; // Start with 10 seconds
    private final int MAX_INTERVAL = 30000; // Maximum interval (30 seconds)
    private final int MIN_INTERVAL = 5000;  // Minimum interval (5 seconds)
    private AutoPayAccessibilityService autoAccessibilityService;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection _mediaProjection;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Surface mSurface;
    private Runnable captureRunnable;
    private Handler handler;
    private Set<String> clickedButtons = new HashSet<>();
    private enum ActionState {
        WAITING_FOR_CONTINUE,
        WAITING_FOR_QR,
        WAITING_FOR_UPLOAD_QR
    }

    private ActionState currentState = ActionState.WAITING_FOR_CONTINUE;

    private AutoPay implementation = new AutoPay();

    private void cleanupResources() {
        // Stop the media projection if active
        if (_mediaProjection != null) {
            _mediaProjection.stop();
            _mediaProjection = null;
        }

        // Release virtual display
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        // Release ImageReader
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        // Stop the screen capture launcher
        if (screenCaptureLauncher != null) {
            // Handle any additional cleanup related to screen capture
        }

        // Reset any ongoing tasks or handlers
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        Log.d("AutoPayPlugin", "Resources cleaned up.");
    }

    @Override
    public void load(){
      super.load();
      autoAccessibilityService = AutoPayAccessibilityService.getInstance();

      screenCaptureLauncher = getActivity().registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
          if (result.getResultCode() == Activity.RESULT_OK){
            Intent data = result.getData();
            if(data != null){
              // Handle the screen capture data here

              try{
                  DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
                  int width = metrics.widthPixels;
                  int height = metrics.heightPixels;
                  int density = metrics.densityDpi;

                MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), data);
                startMediaProjection(mediaProjection, width, height, density);
                startIntervalCapture(width, height);
                Log.d("Auto", "Screen capture intent success");
              }catch(Exception e){
                e.printStackTrace();
              }
            }
          }else{
            Log.e("Auto", "Screen capture intent canceled or failed");
          }
        }
      );
    }

    private void startIntervalCapture(int width, int height){
        AtomicReference<String> previousFrameHash = new AtomicReference<>();

        Runnable captureAndProcess = new Runnable() {
            @Override
            public void run() {
                captureFrame(width, height, bitmap -> {
                    if(bitmap == null){
                        Log.e("Capture", "Bitmap is null, skipping frame capture");
                        restartCapture(this);
                        return;
                    }

                    String currentFrameHash = calculateBitmapHash(bitmap);

                    if(!currentFrameHash.equals(previousFrameHash.get())){
                        previousFrameHash.set(currentFrameHash);


                        ExecutorService executorService = Executors.newFixedThreadPool(4);
                        executorService.submit(() -> {
                           try{
                               processTextRecognition(bitmap);
                           }finally {
                               restartCapture(this);
                           }
                        });
                    }else{
                        recycleBitmap(bitmap);
                        Log.d("Capture", "Frame unchanged, skipping OCR");
                        restartCapture(this);
                    }
                });
            }
        };

        captureAndProcess.run();
    }

    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private void restartCapture(Runnable captureTask) {
        new Handler(Looper.getMainLooper()).postDelayed(captureTask, 10000); // Restart after a delay
    }

    private void processTextRecognition(Bitmap bitmap){
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(this::processTextRecognitionResult)
                .addOnFailureListener(e -> {
                    Log.e("MLKit", "Text recognition failed", e);
                })
                .addOnCompleteListener(task -> {
                    recycleBitmap(bitmap);
                });
    }

    private String calculateBitmapHash(Bitmap bitmap){
        int hash = 0;
        for(int y = 0; y < bitmap.getHeight(); y += 10){
            for(int x = 0; x < bitmap.getWidth(); x += 10){
                hash += bitmap.getPixel(x, y);
            }
        }

        return Integer.toHexString(hash);
    }

    private void processTextRecognitionResult(Text visionText){
        for(Text.TextBlock block : visionText.getTextBlocks()){
            for(Text.Line line : block.getLines()){
                String recognizedText = line.getText();
                Rect boundingBox = line.getBoundingBox();

                Log.d("OCR", "Current State: " + currentState);

                // Check the current state and act accordingly
                switch (currentState) {
                    case WAITING_FOR_CONTINUE:
                        if (recognizedText.equalsIgnoreCase("Continue")) {
                            String buttonId = recognizedText + boundingBox.flattenToString();

                            if (!clickedButtons.contains(buttonId)) {
                                clickedButtons.add(buttonId);

                                int x = boundingBox.centerX();
                                int y = boundingBox.centerY() + 150;  // Adjust y-coordinate if needed
                                Log.d("OCR", "Perform Gesture on Continue");
                                performTap(x, y);

                                // After clicking "Continue", change the state to WAITING_FOR_QR
                                currentState = ActionState.WAITING_FOR_QR;
                            } else {
                                Log.d("TextRecognition", "Button already clicked: " + recognizedText);
                            }
                        }
                        break;

                    case WAITING_FOR_QR:
                        if (recognizedText.equalsIgnoreCase("QR")) {
                            String buttonId = recognizedText + boundingBox.flattenToString();

                            if (!clickedButtons.contains(buttonId)) {
                                clickedButtons.add(buttonId);

                                int x = boundingBox.centerX() + 200;
                                int y = boundingBox.centerY();  // Adjust y-coordinate if needed
                                Log.d("OCR", "Perform Gesture on QR");
                                performTap(x, y);

                                // After clicking "QR", you can reset the state or continue further actions
                                // For now, resetting to WAITING_FOR_CONTINUE (or any other desired next state)
                                currentState = ActionState.WAITING_FOR_UPLOAD_QR;
                            } else {
                                Log.d("TextRecognition", "Button already clicked: " + recognizedText);
                            }
                        }
                        break;
                    case WAITING_FOR_UPLOAD_QR:
                        if (recognizedText.equalsIgnoreCase("Upload QR")) {
                            String buttonId = recognizedText + boundingBox.flattenToString();

                            if (!clickedButtons.contains(buttonId)) {
                                clickedButtons.add(buttonId);

                                int x = boundingBox.centerX();
                                int y = boundingBox.centerY();  // Adjust y-coordinate if needed
                                Log.d("OCR", "Perform Gesture on QR");
                                performTap(x, y);

                                // After clicking "QR", you can reset the state or continue further actions
                                // For now, resetting to WAITING_FOR_CONTINUE (or any other desired next state)
                                currentState = ActionState.WAITING_FOR_CONTINUE;
                            } else {
                                Log.d("TextRecognition", "Button already clicked: " + recognizedText);
                            }
                        }
                        break;
                }
            }
        }
    }

    public interface OnBitmapCapturedCallback{
        void onBitmapCaptured(Bitmap bitmap);
    }

    public void captureFrame(int width, int height, OnBitmapCapturedCallback callback){
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            try{
                image = reader.acquireLatestImage();
                if(image != null){
                    Bitmap bitmap = convertImageToBitmap(image, width, height);
                    callback.onBitmapCaptured(bitmap);
                }
            }finally{
                if(image != null){
                    image.close();
                }
            }
        }, new Handler(Looper.getMainLooper()));
    }

    private Bitmap convertImageToBitmap(Image image, int width, int height){
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
        );
        bitmap.copyPixelsFromBuffer(buffer);
        return bitmap;
    }

    private void startMediaProjection(MediaProjection mediaProjection, int width, int height, int density){
        this._mediaProjection = mediaProjection;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                null
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


        if(!isAccessibilityServiceEnabled(getContext())){
            showAccessibilitySettingsDialog();
        }else{
            Intent serviceIntent = new Intent(getContext(), AutoPayScreenCaptureService.class);
            getContext().startForegroundService(serviceIntent);

            mediaProjectionManager = (MediaProjectionManager) getContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

            if(mediaProjectionManager != null){
                Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                screenCaptureLauncher.launch(captureIntent);

                JSObject ret = new JSObject();
                ret.put("value", coordX);
                call.resolve(ret);
            }else{
                Log.e("Auto", "MediaProjectionManager is null. Could not start capture intent.");
                JSObject ret = new JSObject();
                ret.put("error", "Failed to initialize MediaProjectionManager");
                call.reject("Error", ret);
            }
        }
        /*JSObject ret = new JSObject();
        ret.put("value", coordX);
        call.resolve(ret);*/
    }

    private void performTap(float x, float y) {
        if (autoAccessibilityService != null) {
            autoAccessibilityService.performTap(x, y);
        } else {
            Log.e("AUTOPLUGIN", "Accessibility service is not initialized.");
            autoAccessibilityService = AutoPayAccessibilityService.getInstance();
            autoAccessibilityService.performTap(x, y);
        }
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

    private void showAccessibilitySettingsDialog() {
        new Handler(Looper.getMainLooper()).post(() -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Accessibility Required")
                    .setMessage("To use this feature, please enable the accessibility service in your settings.")
                    .setPositiveButton("Go to Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Redirect to accessibility settings
                            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                            getContext().startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setCancelable(false)
                    .show();
        });
    }


}
