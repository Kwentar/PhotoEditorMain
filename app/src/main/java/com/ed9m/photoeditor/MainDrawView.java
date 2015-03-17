package com.ed9m.photoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import org.opencv.core.Rect;
import android.widget.ImageView;


public class MainDrawView extends ImageView {

    public class Splayn {

        public  SplineTuple[] splines ; // Сплайн


        // Построение сплайна
        // x - узлы сетки, должны быть упорядочены по возрастанию, кратные узлы запрещены
        // y - значения функции в узлах сетки
        // n - количество узлов сетки
        public  void BuildSpline(PointF[] ps, int n) {
            // Инициализация массива сплайнов
            splines = new SplineTuple[n];
            for(int i=0;i<n;i++){
                splines[i] = new SplineTuple();
            }

            for (int i = 0; i < n; ++i) {
                splines[i].x = ps[i].x;
                splines[i].a = ps[i].y;
            }
            splines[0].c = splines[n - 1].c = 0.0;

            // Решение СЛАУ относительно коэффициентов сплайнов c[i] методом прогонки для трехдиагональных матриц
            // Вычисление прогоночных коэффициентов - прямой ход метода прогонки
            double[] alpha = new double[n - 1];
            double[] beta = new double[n - 1];
            alpha[0] = beta[0] = 0.0;
            for (int i = 1; i < n - 1; ++i) {
                double h_i = ps[i].x - ps[i-1].x, h_i1 = ps[i+1].x - ps[i].x;
                double A = h_i;
                double C = 2.0 * (h_i + h_i1);
                double B = h_i1;
                double F = 6.0 * ((ps[i+1].y - ps[i].y) / h_i1 - (ps[i].y - ps[i-1].y) / h_i);
                double z = (A * alpha[i - 1] + C);
                alpha[i] = -B / z;
                beta[i] = (F - A * beta[i - 1]) / z;
            }

            // Нахождение решения - обратный ход метода прогонки
            for (int i = n - 2; i > 0; --i)
                splines[i].c = alpha[i] * splines[i + 1].c + beta[i];

            // Освобождение памяти, занимаемой прогоночными коэффициентами
            beta = null;
            alpha = null;

            // По известным коэффициентам c[i] находим значения b[i] и d[i]
            for (int i = n - 1; i > 0; --i) {
                double h_i = ps[i].x - ps[i-1].x;
                splines[i].d = (splines[i].c - splines[i - 1].c) / h_i;
                splines[i].b = h_i * (2.0 * splines[i].c + splines[i - 1].c) / 6.0 + (ps[i].y - ps[i-1].y) / h_i;
            }
        }

        // Вычисление значения интерполированной функции в произвольной точке
        public double f(double x) {

            SplineTuple s;
            int n= splines.length;
            //BuildSpline(myx,y,n);
            if (x <= splines[0].x) // Если x меньше точки сетки x[0] - пользуемся первым эл-тов массива
                s = splines[1];
            else if (x >= splines[n - 1].x) // Если x больше точки сетки x[n - 1] - пользуемся последним эл-том массива
                s = splines[n - 1];
            else // Иначе x лежит между граничными точками сетки - производим бинарный поиск нужного эл-та массива
            {
                int i = 0, j = n - 1;
                while (i + 1 < j) {
                    int k = i + (j - i) / 2;
                    if (x <= splines[k].x)
                        j = k;
                    else
                        i = k;
                }
                s = splines[j];
            }

            double dx = (x - s.x);
            // Вычисляем значение сплайна в заданной точке по схеме Горнера (в принципе, "умный" компилятор применил бы схему Горнера сам, но ведь не все так умны, как кажутся)
            return s.a + (s.b + (s.c / 2.0 + s.d * dx / 6.0) * dx) * dx;
        }

    }
    public class SplineTuple {
        public double a, b, c, d, x;
    }


    boolean drawRect = false;
    public Rect roi = new Rect(0,0,0,0);
    private PointF beginPoint = new PointF(0,0);
    private PointF endPoint = new PointF(0,0);
    private PointF[] curvesPoints = new PointF[5];
    int h,w;
    float cRadius = 0.f;
    PointF currentPoint = null;
    private boolean bWasInit = false;
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
        drawGrid(canvas);
        drawCurve(canvas);
        drawCircles(canvas);
    }
    private void Init() {
        bWasInit = true;
        h = this.getHeight();
        w = this.getWidth();
        cRadius = w/13;
        for(int i = 0; i < 5; i++) {
            curvesPoints[i] = new PointF(w * i / 4, w + (h-w)/2 - w * i / 4);
        }
        Log.i("MAIN_DRAW_VIEW","Init successful");
    }
    public void drawCircles(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        for(int i = 0; i < 5; i++) {
            canvas.drawCircle(curvesPoints[i].x, curvesPoints[i].y,cRadius,paint);
        }
    }
    public void drawGrid(Canvas canvas) {
        if(!bWasInit) Init();
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        for(int i = 0; i < 5; i++) {
            canvas.drawLine(w * i / 4 , (h-w)/2, w * i / 4, w + (h-w)/2, paint);
        }
        for(int i = 0; i < 5; i++) {
            canvas.drawLine(0 , (h-w)/2+w * i / 4, w, (h-w)/2+w * i / 4, paint);
        }
    }
    // interpolate three points with second point at specified parameter value
    int[] interpolate(int x0, int y0, int x1, int y1, int x2, int y2, double t) {
        double t1 = 1.0 -t;
        double tSq = t * t;
        double denom = 2.0 * t * t1;
        int cx = (int) ((x1 - t1 * t1 * x0 - tSq * x2) / denom);
        int cy = (int) ((y1 - t1 * t1 * y0 - tSq * y2) / denom);
        return new int[] {cx, cy};
    }
    public void drawCurve(Canvas canvas) {
        Paint paint = new Paint();
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        Path path = new Path();
        int[] dp = interpolate(0,w+(h-w)/2, (int)endPoint.x,(int)endPoint.y,w,(h-w)/2, 0.5);
        path.moveTo(curvesPoints[0].x, curvesPoints[0].y);
        //path.cubicTo(curvesPoints[1].x, curvesPoints[1].y,curvesPoints[2].x, curvesPoints[2].y,curvesPoints[3].x, curvesPoints[3].y);
        //path.cubicTo(curvesPoints[2].x, curvesPoints[2].y,curvesPoints[3].x, curvesPoints[3].y,curvesPoints[4].x, curvesPoints[4].y);
        Splayn splayn = new Splayn();

        splayn.BuildSpline(curvesPoints, 5);
        for(int i = (int)curvesPoints[0].x; i < curvesPoints[4].x; i++) {
            int y = (int)splayn.f(i);
            path.lineTo(i,y);
        }

        //path.quadTo(dp[0], dp[1],w, (h-w)/2);

        canvas.drawPath(path,paint);
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
                int index = GetNearestPoint(new PointF(e.getX(), e.getY()));
                if(index != -1) {
                    currentPoint = curvesPoints[index];
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(currentPoint != null) {
                    currentPoint.x = e.getX();
                    currentPoint.y = e.getY();
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                currentPoint = null;
                invalidate();
                break;
        }
        return true;
    }
    private float getDistance(PointF p1, PointF p2) {
        return (float)Math.sqrt((p1.x - p2.x)*(p1.x-p2.x) + (p1.y - p2.y)*(p1.y-p2.y));
    }
    private int GetNearestPoint(PointF p) {
        int index = -1;
        float minDistance = h*w;
        for(int i = 0; i < 5; i++) {
            float currDistance = getDistance(curvesPoints[i], p);
            if(currDistance < minDistance) {
                minDistance = currDistance;
                index = i;
            }
        }
        if(minDistance < cRadius) {
            return index;
        }
        return -1;
    }
}
