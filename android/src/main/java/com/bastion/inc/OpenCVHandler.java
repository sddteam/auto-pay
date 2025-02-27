package com.bastion.inc;

import android.app.Notification;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
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
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Rect;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class OpenCVHandler implements ProjectionImageListener {
    private final String TAG = "OPEN_CV";
    private final Context context;
    private ApplicationStateListener applicationStateListener;
    private final Map<ActionState, List<Mat>> preloadedStateTemplates = new HashMap<>();
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
    private final double TEMP_DENSITY_DPI = 480.0;
    private static OpenCVHandler instance;

    public static synchronized OpenCVHandler getInstance(Context context){
        if(instance == null){
            instance = new OpenCVHandler(context);
        }

        return instance;
    }

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
        InputStream inputStream = context.getAssets().open(path);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        if (bitmap == null) {
            throw new IOException("Failed to decode asset: " + path);
        }

        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY);
        return mat;
    }

    private List<Mat> loadTemplateList(String[] paths) throws IOException {
        List<Mat> templates = new ArrayList<>();
        for (String path: paths){
            Mat template = loadTemplate(path);
            templates.add(template);
        }

        return templates;
    }

    private void preloadTemplates(){
        try{
            preloadedStateTemplates.put(ActionState.LOGIN, loadTemplateList(new String[]{
                    "opencv-template/gcash/states/1 login.jpg",
            }));
            preloadedStateTemplates.put(ActionState.HOME, loadTemplateList(new String[]{
                    "opencv-template/gcash/states/2 home.jpg",
                    "opencv-template/gcash/states/2a home.jpg",
            }));
            preloadedStateTemplates.put(ActionState.UPLOAD_QR, loadTemplateList(new String[]{
                    "opencv-template/gcash/states/3 qrscan.jpg"
            }));
            preloadedStateTemplates.put(ActionState.SELECT_QR, loadTemplateList(new String[]{
                    "opencv-template/gcash/states/4 uploadqr.jpg",
                    "opencv-template/gcash/states/4a uploadqr.jpg"
            }));
            preloadedStateTemplates.put(ActionState.PAYMENT, loadTemplateList(new String[]{
                    "opencv-template/gcash/states/5 payment.jpg"
            }));
            preloadedButtonTemplates.put("qrButton", loadTemplate("opencv-template/gcash/buttons/qrbutton.jpg"));
            preloadedButtonTemplates.put("uploadQRButton", loadTemplate("opencv-template/gcash/buttons/uploadqr.jpg"));
        }catch (Exception e){
            Log.e("OPENCV", "Error preloading templates", e);
        }
    }

    @Override
    public void onImage(Bitmap bitmap) {

        //Log.d(TAG, "Screen captured!");
        if (isProcessing.get()) {
            recycleBitmap(bitmap);
            return; // Skip this frame
        }

        /*String currentFrameHash = calculateBitmapHash(bitmap);

        if(currentFrameHash.equals(previousFrameHash.get())){
            recycleBitmap(bitmap);
            return;
        }*/

        //Ads monitoring
        //accessibilityGestures.ads();

        isProcessing.set(true);
        //previousFrameHash.set(currentFrameHash);

        executorService.submit(() -> {
            try{
                Mat screenMat = convertBitmapToMat(bitmap);


                MatchResult matchResult = findBestMatch(screenMat, 0.1);
                Log.d(TAG, String.valueOf(matchResult.templateName));
                tapBestMatch(matchResult, screenMat, MAX_CLICK);

            }catch (Exception e){
                applicationStateListener.onError(e);
                overlayManager.updateOverlayMessage(e.getLocalizedMessage());
            }finally {
                isProcessing.set(false);
                recycleBitmap(bitmap);
            }
        });
    }

    public Mat takeScreenshot(){
        try {
            Bitmap bitmap = null;
            int retries = 5;  // Max number of attempts
            int delayMs = 50; // Delay in milliseconds between retries

            while (retries-- > 0 && bitmap == null) {
                bitmap = MediaProjectionHandler.getInstance(context.getApplicationContext()).captureLatestImage();
                if (bitmap == null) {
                    try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
                }
            }

            return (bitmap != null) ? convertBitmapToMat(bitmap) : null;

        } catch (Exception e) {
            return null;
        }
    }
    private void tapBestMatch(MatchResult matchResult, Mat screenMat, int timeout){
        if(matchResult == null){
            System.out.println("No match found.");
            return;
        }

        Log.d(TAG, "WINNER: " + matchResult.templateName);

        if (matchResult.templateName.equals(lastState)) {
            clickCount++;
        } else {
            clickCount = 1;
            lastState = matchResult.templateName;
        }

        if(clickCount >= timeout){
            JSObject ret = new JSObject();
            String message = "Failed to proceed in " + matchResult.templateName;
            ret.put("value", message);

            overlayManager.updateOverlayMessage(message);

            //TODO: RETURN FOR LOGGING IN CLIENT SIDE
            //applicationStateListener.onWarning(ret);
        }else{
            overlayManager.updateOverlayMessage("AutoPay processing...");
        }

        if(matchResult.templateName.equals(ActionState.HOME)){
            currentState = ActionState.HOME;
            accessibilityGestures.find("QR");
        }
        if(matchResult.templateName.equals(ActionState.UPLOAD_QR)){
            currentState = ActionState.UPLOAD_QR;
            accessibilityGestures.click(findButtonLocation(
                    preloadedButtonTemplates.get("uploadQRButton"),
                    screenMat
            ));

            overlayManager.showWhiteOverlay();
        }
        if(matchResult.templateName.equals(ActionState.SELECT_QR)){
            currentState = ActionState.SELECT_QR;
            String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String fileName = "qr_code_" + currentDate + ".png";
            accessibilityGestures.qr(fileName);
        }
        if(matchResult.templateName.equals(ActionState.PAYMENT)){
            //HANDLE THE STOP EXECUTION WITH PLUGIN METHOD FOR NAVIGATE
            //applicationStateListener.onCompleted();
        }
    }

    public MatchResult findBestMatch(Mat screenMat, double threshold){
        double bestMatchScore = 0;
        ActionState bestMatchTemplate = null;
        Point bestMatchPoint = null;

        for(Map.Entry<ActionState, List<Mat>> entry : preloadedStateTemplates.entrySet()){
            List<Mat> templates = entry.getValue();

            double bestMatchScoreFromState = 0;
            ActionState bestMatchTemplateFromState = null;
            Point bestMatchPointFromState = null;
            for(Mat template: templates){
                Size newSize = normalizeScaling(template);

                Mat resizedTemplate = new Mat();
                Imgproc.resize(template, resizedTemplate, newSize);

                Mat result = new Mat();
                Imgproc.matchTemplate(screenMat, resizedTemplate, result, Imgproc.TM_CCOEFF_NORMED);

                Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

                if(mmr.maxVal > bestMatchScoreFromState){
                    bestMatchScoreFromState = mmr.maxVal;
                    bestMatchTemplateFromState = entry.getKey();
                    bestMatchPointFromState = mmr.maxLoc;
                }

                resizedTemplate.release();
            }

            if(bestMatchScoreFromState > bestMatchScore){
                bestMatchScore = bestMatchScoreFromState;
                bestMatchTemplate = bestMatchTemplateFromState;
                bestMatchPoint = bestMatchPointFromState;
            }

            //Log.d(TAG, "CONFIDENCE: " + bestMatchScoreFromState + " STATE: "+ entry.getKey());
        }

        if(bestMatchScore >= threshold){
            return new MatchResult(bestMatchTemplate, bestMatchPoint, bestMatchScore);
        }

        return null;
    }

    private Location findButtonLocation(Mat template, Mat screenMat){
        try{
            Mat result = new Mat();
            Rect roi = new Rect();

            Size newSize = scaleButtonTemplate(template);

            Mat resizedTemp = new Mat();
            Imgproc.resize(template , resizedTemp, newSize);

            if(currentState == ActionState.HOME){
                int[] roiPath = {1, 1};
                Pair<Mat, Rect> roiResult = locateTemplateInROI(screenMat, resizedTemp, roiPath, false);
                roi = roiResult.second;

                Imgproc.matchTemplate(roiResult.first, resizedTemp, result, Imgproc.TM_SQDIFF_NORMED);
            } else if (currentState == ActionState.SELECT_QR) {
                int[] roiPath = {1};
                Pair<Mat, Rect> roiResult = locateTemplateInROI(screenMat, resizedTemp, roiPath, true);
                roi = roiResult.second;

                Imgproc.matchTemplate(roiResult.first, resizedTemp, result, Imgproc.TM_SQDIFF_NORMED);
            }else{
                Imgproc.matchTemplate(screenMat, resizedTemp, result, Imgproc.TM_SQDIFF_NORMED);
            }

            //Imgproc.matchTemplate(screenMat, template, result, Imgproc.TM_SQDIFF_NORMED);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
            Point matchLoc = new Point(mmr.minLoc.x, mmr.minLoc.y);

            matchLoc.x += roi.x;
            matchLoc.y += roi.y;

            Log.d("CONFIDENCE", "Confidence: " + String.valueOf(mmr.minVal));

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

    private Size normalizeScaling(Mat template){
        if(template.rows() <= 400){
            return scaleButtonTemplate(template);
        }else{
            return scaleStateTemplate(template);
        }
    }

    private Size scaleButtonTemplate(Mat template){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        double scaleX = (double) displayMetrics.widthPixels / 1080;
        double scaleY = (double) displayMetrics.heightPixels / 2340;
        double scaleFactor = Math.min(scaleX, scaleY);

        int newWidth = (int) Math.round(template.cols() * scaleFactor);
        int newHeight = (int) Math.round(template.rows() * scaleFactor);

        return new Size(newWidth, newHeight);
    }

    private Size scaleStateTemplate(Mat template){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        double scaleX = (double) displayMetrics.widthPixels / template.cols();
        double scaleY = (double) displayMetrics.heightPixels / template.rows();

        return new Size(template.cols() * scaleX, template.rows() * scaleY);
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
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2GRAY); // or COLOR_RGBA2GRAY based on the bitmap format
            }

            return mat;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Bitmap convertMatToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }
}
