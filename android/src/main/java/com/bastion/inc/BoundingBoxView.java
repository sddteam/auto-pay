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
    public BoundingBoxView(Context context, List<Rect> boundsList) {
        super(context);

        if(boundsList != null){
            this.boundsList.addAll(boundsList);
        }

        paint = createPaint();

    }

    public BoundingBoxView(Context context, Rect singleRect){
        super(context);
        if(singleRect != null){
            this.boundsList.add(singleRect);
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas){
        super.onDraw(canvas);
        for(Rect rect : boundsList){
            canvas.drawRect(rect, paint);
        }
    }

    private Paint createPaint(){
        Paint p = new Paint();
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);

        return p;
    }
}
