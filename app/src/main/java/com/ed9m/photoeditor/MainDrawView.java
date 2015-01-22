package com.ed9m.photoeditor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.sql.DatabaseMetaData;

/**
 * Created by Prothet on 22.01.2015.
 */
public class MainDrawView extends ImageView {
    Paint paint = new Paint();
    boolean drawRect = false;
    public PointF beginPoint = new PointF(0,0);
    public PointF endPoint = new PointF(0,0);
    public MainDrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public MainDrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public MainDrawView(Context context) {
        super(context);
    }
    @Override
    public void onDraw(Canvas canvas) {
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(beginPoint.x,beginPoint.y,endPoint.x,endPoint.y,paint);
    }
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch(e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drawRect = true;
                beginPoint.y = e.getY();
                beginPoint.x = e.getX();
                endPoint.y = e.getY();
                endPoint.x = e.getX();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                endPoint.x = e.getX();
                endPoint.y = e.getY();
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                drawRect = false;
                invalidate();
                break;
        }
        return true;
    }
}
