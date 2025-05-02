package com.bastion.inc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class BoundingBoxView extends View {
    private final List<Rect> boundsList = new ArrayList<>();
    private Paint paint;
    private Paint textPaint;
    public BoundingBoxView(Context context, List<Rect> boundsList) {
        super(context);

        if(boundsList != null){
            this.boundsList.addAll(boundsList);
        }

        paint = createPaint();

        textPaint = createTextPaint();

    }

    public BoundingBoxView(Context context, Rect singleRect){
        super(context);
        if(singleRect != null){
            this.boundsList.add(singleRect);
        }

        paint = createPaint();
      textPaint = createTextPaint();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas){
        super.onDraw(canvas);
        for(Rect rect : boundsList){
            canvas.drawRect(rect, paint);

            String rectText = rect.left + "," + rect.top + "," + rect.right + "," + rect.bottom;

            float textX = rect.left;
            float textY = rect.top - 10;
            canvas.drawText(rectText, textX, textY, textPaint);
        }
    }

    private Paint createPaint(){
        Paint p = new Paint();
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);

        return p;
    }

    private Paint createTextPaint(){
      Paint p = new Paint();
      p.setColor(Color.GREEN);
      p.setTextSize(18);
      p.setStyle(Paint.Style.FILL);

      return p;
    }
}
