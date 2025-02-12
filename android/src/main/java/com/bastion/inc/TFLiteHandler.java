package com.bastion.inc;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class TFLiteHandler implements ProjectionImageListener{
    private final String TAG = "TFLite";
    private final Context context;
    private Interpreter tflite;
    private List<String> labels;


    public TFLiteHandler(Context context){
        this.context = context;

        try{
            this.tflite = new Interpreter(loadModelFile("gcash_tflite/model_unquant.tflite"));
            this.labels = loadLabels("gcash_tflite/labels.txt");
        }catch(IOException e){
            Log.e(TAG, "Error loading model or labels", e);
        }
    }
    @Override
    public void onImage(Bitmap bitmap) {
        try{
            bitmap = preprocessBitmap(bitmap);


            String result = runInference(bitmap);
            Log.d(TAG, result);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private Bitmap preprocessBitmap(Bitmap src) {
        int targetSize = 224; // Ensure model's expected input size
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(src, targetSize, targetSize, true);

        Bitmap grayscaleBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(grayscaleBitmap);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(resizedBitmap, 0, 0, paint);

        return grayscaleBitmap;
    }

    private MappedByteBuffer loadModelFile(String path) throws IOException{
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(path);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private List<String> loadLabels(String labelPath) throws IOException {
        List<String> labels = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelPath)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
        }
        return labels;
    }

    private String runInference(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Prepare input array
        float[][][][] input = new float[1][width][height][3];
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[y * width + x];
                input[0][x][y][0] = ((pixel >> 16) & 0xFF) / 255.0f; // Red
                input[0][x][y][1] = ((pixel >> 8) & 0xFF) / 255.0f;  // Green
                input[0][x][y][2] = (pixel & 0xFF) / 255.0f;         // Blue
            }
        }

        // Run inference
        float[][] output = new float[1][labels.size()];
        tflite.run(input, output);

        // Find the best prediction
        int maxIndex = 0;
        float maxConfidence = 0;
        for (int i = 0; i < output[0].length; i++) {
            Log.d(TAG, labels.get(i) + " (" + output[0][i] + ")");
            if (output[0][i] > maxConfidence) {
                maxConfidence = output[0][i];
                maxIndex = i;
            }
        }

        return labels.get(maxIndex) + " (" + maxConfidence + ")";
    }
}
