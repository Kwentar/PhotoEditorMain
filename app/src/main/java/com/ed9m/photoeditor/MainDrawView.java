package com.ed9m.photoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import org.opencv.core.Rect;
import android.widget.ImageView;


public class MainDrawView extends ImageView {
    Paint paint = new Paint();
    boolean drawRect = false;
    public Rect roi = new Rect(0,0,0,0);
    private PointF beginPoint = new PointF(0,0);
    private PointF endPoint = new PointF(0,0);
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
    @SuppressWarnings("deprecation")
    public void setImageBitmap(Bitmap btm) {

        super.setImageBitmap(btm);

        BitmapDrawable btm_draw = new BitmapDrawable(getResources(), btm);
        btm_draw.setGravity(Gravity.CENTER);
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            setBackground(btm_draw);
        } else {
            setBackgroundDrawable(btm_draw);
        }
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
                roi.x = (int)beginPoint.x;
                roi.y = (int)beginPoint.y;
                roi.width = (int)(endPoint.x - beginPoint.x);
                roi.height = (int)(endPoint.y - beginPoint.y);

                invalidate();
                break;
        }
        return true;
    }
}
