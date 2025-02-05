package com.bastion.inc;

import android.app.Notification;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.Pair;

import com.getcapacitor.JSObject;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Rect;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class OpenCVHandler implements ProjectionImageListener {
    private final String TAG = "OPEN_CV";
    private final Context context;
    private ApplicationStateListener applicationStateListener;
    private final Map<ActionState, Mat> preloadedStateTemplates = new HashMap<>();
    private final Map<String, Mat> preloadedButtonTemplates = new HashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    AtomicReference<String> previousFrameHash = new AtomicReference<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final AccessibilityGestures accessibilityGestures;
    private ActionState currentState = ActionState.HOME;
    private int clickCount = 0;
    private ActionState lastState = null;
    private static final int MAX_CLICK = 3;
    private OverlayManager overlayManager;

    public void setApplicationStateListener(ApplicationStateListener listener){
        this.applicationStateListener = listener;
    }
    public OpenCVHandler(Context context){
        this.context = context;

        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed!");
        } else {
            Log.d(TAG, "OpenCV initialized successfully.");
        }

        accessibilityGestures = new AccessibilityGestures(context);
        overlayManager = OverlayManager.getInstance(context);
        preloadTemplates();
    }

    private Mat loadTemplate(String path) throws IOException {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(path);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (bitmap == null) {
            throw new IOException("Failed to decode asset: " + path);
        }

        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY);
        return mat;
    }

    private void preloadTemplates(){
        try{
            preloadedStateTemplates.put(ActionState.HOME, loadTemplate("public/assets/opencv-template/gcash/states/2 home.jpg"));
            preloadedStateTemplates.put(ActionState.UPLOAD_QR, loadTemplate("public/assets/opencv-template/gcash/states/3 qrscan.jpg"));
            preloadedStateTemplates.put(ActionState.PAYMENT, loadTemplate("public/assets/opencv-template/gcash/states/5 payment.jpg"));
            preloadedStateTemplates.put(ActionState.SELECT_QR, loadTemplate("public/assets/opencv-template/gcash/states/4 uploadqr.jpg"));
            preloadedButtonTemplates.put("qrButton", loadTemplate("public/assets/opencv-template/gcash/buttons/qrbutton.jpg"));
            preloadedButtonTemplates.put("uploadQRButton", loadTemplate("public/assets/opencv-template/gcash/buttons/uploadqr.jpg"));
            preloadedButtonTemplates.put("qrCodeButton", loadTemplate("public/assets/opencv-template/gcash/buttons/qrcode.jpg"));
        }catch (Exception e){
            Log.e("OPENCV", "Error preloading templates", e);
        }
    }

    @Override
    public void onImage(Bitmap bitmap) {
        if (isProcessing.get()) {
            recycleBitmap(bitmap);
            return; // Skip this frame
        }

        String currentFrameHash = calculateBitmapHash(bitmap);

        if(currentFrameHash.equals(previousFrameHash.get())){
            recycleBitmap(bitmap);
            return;
        }

        //TODO: Ads monitoring
        accessibilityGestures.ads();

        isProcessing.set(true);
        previousFrameHash.set(currentFrameHash);

        executorService.submit(() -> {
            try{
                ActionState bestState = null;
                double highestMaxValue = Double.NEGATIVE_INFINITY;

                Mat screenMat = convertBitmapToMat(bitmap);
                for(Map.Entry<ActionState, Mat> entry : preloadedStateTemplates.entrySet()){
                    ActionState state = entry.getKey();
                    Mat template = entry.getValue();

                    Mat matchResult = loadAssetAndMatch(template, screenMat);
                    double maxVal = getMaxVal(matchResult);

                    Log.d(TAG, "CONFIDENCE: " + maxVal + " STATE: " + state);

                    if (maxVal > highestMaxValue) {
                        highestMaxValue = maxVal;
                        bestState = state;
                    }
                }

                if(highestMaxValue <= 0.1){
                    return;
                }

                if(bestState != null){
                    Log.d(TAG, "WINNER: " + bestState);

                    if (bestState.equals(lastState)) {
                        clickCount++;
                    } else {
                        clickCount = 1;
                        lastState = bestState;
                    }

                    if(clickCount >= MAX_CLICK){
                        JSObject ret = new JSObject();
                        String message = "Failed to proceed in " + bestState;
                        ret.put("value", message);

                        overlayManager.updateOverlayMessage(message);
                        //applicationStateListener.onWarning(ret);
                    }else{
                        overlayManager.updateOverlayMessage("AutoPay processing...");
                    }

                    if(bestState.equals(ActionState.HOME)){
                        currentState = ActionState.HOME;
                        Location location = findButtonLocation(
                                preloadedButtonTemplates.get("qrButton"),
                                screenMat);
                        accessibilityGestures.click(location);
                    }
                    if(bestState.equals(ActionState.UPLOAD_QR)){
                        currentState = ActionState.UPLOAD_QR;
                        accessibilityGestures.click(findButtonLocation(
                                preloadedButtonTemplates.get("uploadQRButton"),
                                screenMat
                        ));
                    }
                    if(bestState.equals(ActionState.SELECT_QR)){
                        currentState = ActionState.SELECT_QR;
                        accessibilityGestures.find("qr_code.png");
                    }
                    if(bestState.equals(ActionState.PAYMENT)){
                        //TODO: HANDLE THE STOP EXECUTION WITH PLUGIN METHOD FOR NAVIGATE
                        applicationStateListener.onCompleted();
                    }
                }
            }catch (Exception e){
                applicationStateListener.onError(e);
            }finally {
                isProcessing.set(false);

                recycleBitmap(bitmap);
            }
        });
    }

    private double getMaxVal(Mat result){
        if (result.empty()) {
            return -1;
        }
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        return mmr.maxVal;
    }

    private Location findButtonLocation(Mat template, Mat screenMat){
        try{
            Mat result = new Mat();
            Rect roi = new Rect();

            if(currentState == ActionState.HOME){
                int[] roiPath = {1, 1};
                Pair<Mat, Rect> roiResult = locateTemplateInROI(screenMat, template, roiPath, false);
                roi = roiResult.second;

                Imgproc.matchTemplate(roiResult.first, template, result, Imgproc.TM_CCOEFF_NORMED);
            } else if (currentState == ActionState.SELECT_QR) {
                int[] roiPath = {1};
                Pair<Mat, Rect> roiResult = locateTemplateInROI(screenMat, template, roiPath, true);
                roi = roiResult.second;

                Imgproc.matchTemplate(roiResult.first, template, result, Imgproc.TM_CCOEFF_NORMED);
            }else{
                Imgproc.matchTemplate(screenMat, template, result, Imgproc.TM_CCOEFF_NORMED);
            }

            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            Point matchLoc = new Point(mmr.maxLoc.x, mmr.maxLoc.y);

            matchLoc.x += roi.x;
            matchLoc.y += roi.y;

            Log.d("CONFIDENCE", "Confidence: " + String.valueOf(mmr.maxVal));

            float scale = 1.15F;

            // Get the size of the template
            int templateWidth = template.cols();
            int templateHeight = template.rows();

            float x = (float) (matchLoc.x * scale);
            float y = (float) (matchLoc.y * scale);

            Imgproc.rectangle(screenMat, matchLoc,
                    new Point(matchLoc.x + templateWidth, matchLoc.y + templateHeight),
                    new Scalar(0, 255, 0), 2);

            Bitmap updatedScreenBitmap = convertMatToBitmap(screenMat);

            Log.d("MATCHLOC", "x: " + matchLoc.x + " y: " + matchLoc.y);
            Log.d("CENTER", "x: " + x + " y: " + y);

            return new Location((int) x, (int) y);
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    private Pair<Mat, Rect> locateTemplateInROI(Mat screenMat, Mat template, int[] roiPath, boolean splitVertically) {
        // Initialize the region of interest as the full screenMat
        org.opencv.core.Rect currentROI = new org.opencv.core.Rect(0, 0, screenMat.cols(), screenMat.rows());
        Mat currentMat = screenMat;

        // Traverse the ROI path
        for (int i = 0; i < roiPath.length; i++) {
            int rows = currentMat.rows();
            int cols = currentMat.cols();
            int roiX = 0;
            int roiY = 0;
            int roiWidth = cols;
            int roiHeight = rows;

            if (splitVertically) {
                // Split into left (0) or right (1) for vertical division
                if (roiPath[i] == 0) {
                    roiWidth = cols / 2; // Left half
                } else if (roiPath[i] == 1) {
                    roiX = cols / 2; // Right half
                    roiWidth = cols / 2; // Adjust to right
                } else {
                    throw new IllegalArgumentException("Invalid ROI path value. Must be 0 (left) or 1 (right) for vertical splitting.");
                }
            } else {
                // Split into top (0) or bottom (1) for horizontal division
                if (roiPath[i] == 0) {
                    roiHeight = rows / 2; // Top half
                } else if (roiPath[i] == 1) {
                    roiY = rows / 2; // Bottom half
                    roiHeight = rows / 2; // Adjust to bottom
                } else {
                    throw new IllegalArgumentException("Invalid ROI path value. Must be 0 (top) or 1 (bottom) for horizontal splitting.");
                }
            }

            // Update the current ROI
            currentROI = new org.opencv.core.Rect(currentROI.x + roiX, currentROI.y + roiY, roiWidth, roiHeight);
            currentMat = new Mat(screenMat, currentROI);
        }

        // Return the resulting Mat and ROI
        return new Pair<>(currentMat, currentROI);
    }

    private void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
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

    private Mat convertBitmapToMat(Bitmap bitmap){
        try {
            Mat mat = new Mat();
            // Convert Bitmap to Mat
            Utils.bitmapToMat(bitmap, mat);

            // Convert to grayscale if the image has more than one channel (i.e., color)
            if (mat.channels() > 1) {
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY); // or COLOR_RGBA2GRAY based on the bitmap format
            }

            return mat;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Mat loadAssetAndMatch(Mat template, Mat screenMat){
        if (screenMat.dims() > 2 || template.dims() > 2) {
            throw new IllegalArgumentException("Both images must be 2D");
        }

        if (template.rows() > screenMat.rows() || template.cols() > screenMat.cols()) {
            Size size = new Size(Math.min(screenMat.cols(), template.cols()), Math.min(screenMat.rows(), template.rows()));
            Imgproc.resize(template, template, size);
        }

        Bitmap tempBit = convertMatToBitmap(template);
        Bitmap screenBit = convertMatToBitmap(screenMat);

        Mat result = new Mat();
        Imgproc.matchTemplate(screenMat, template, result, Imgproc.TM_CCOEFF_NORMED);

        return result;
    }
    public Bitmap convertMatToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }
}
