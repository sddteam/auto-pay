package com.bastion.inc;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.List;

public class BoundingBoxView extends View {
    private final List<Rect> boundsList;
    private final Paint paint;
    public BoundingBoxView(Context context, List<Rect> boundsList) {
        super(context);

        this.boundsList = boundsList;
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas){
        super.onDraw(canvas);
        for(Rect rect : boundsList){
            canvas.drawRect(rect, paint);
        }
    }
}
