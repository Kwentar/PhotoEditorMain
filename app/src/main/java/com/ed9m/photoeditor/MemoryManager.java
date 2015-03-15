package com.ed9m.photoeditor;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.view.Display;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;

import java.util.ArrayList;


public class MemoryManager {
    public static final MemoryManager Instance = new MemoryManager();
    public Bitmap sourceBtm;
    public Bitmap resultBtm;
    public Bitmap tmpBtm;
    public Bitmap alphaBtm;
    public Bitmap smallSourceBtm; //small bitmap for buttons
    public Bitmap smallResultBtm;
    public Bitmap smallAlphaBtm;
    public ArrayList<Bitmap> smallTmpBtms;
    public Mat matSource;
    public Mat matResult;
    public Mat matTmp;
    public Mat matSmallSource;
    public Mat matSmallResult;
    public Mat matSmallTmp;
    private boolean bWasInit = false;
    private Size smallSize;
    public void Init(Display display, int countFilters) {
        if(!bWasInit) {
            Size normalSize = new Size(sourceBtm.getWidth(), sourceBtm.getHeight());
            smallTmpBtms = new ArrayList<Bitmap>();

            smallSize = new Size();
            Point size = new Point();
            display.getSize(size);
            int maxSize = size.x / 4;
            if (sourceBtm.getWidth() > sourceBtm.getHeight()) {
                smallSize.width = maxSize;
                smallSize.height = sourceBtm.getHeight() * maxSize / sourceBtm.getWidth();
            } else {
                smallSize.height = maxSize;
                smallSize.width = sourceBtm.getWidth() * maxSize / sourceBtm.getHeight();
            }
            resultBtm = Bitmap.createBitmap(sourceBtm.getWidth(),sourceBtm.getHeight(), Bitmap.Config.ARGB_8888);
            tmpBtm = Bitmap.createBitmap(sourceBtm.getWidth(),sourceBtm.getHeight(), Bitmap.Config.ARGB_8888);
            alphaBtm = Bitmap.createBitmap(sourceBtm.getWidth(),sourceBtm.getHeight(), Bitmap.Config.ARGB_8888);
            smallSourceBtm = Bitmap.createBitmap((int) smallSize.width, (int) smallSize.height, Bitmap.Config.ARGB_8888);
            smallResultBtm = Bitmap.createBitmap((int) smallSize.width, (int) smallSize.height, Bitmap.Config.ARGB_8888);
            smallAlphaBtm = Bitmap.createBitmap((int) smallSize.width, (int) smallSize.height, Bitmap.Config.ARGB_8888);

            for(int i = 0; i < countFilters; i++) {
                Bitmap smallTmpBtm = Bitmap.createBitmap((int) smallSize.width, (int) smallSize.height, Bitmap.Config.ARGB_8888);
                smallTmpBtms.add(smallTmpBtm);
            }
            matSource = new Mat(normalSize, CvType.CV_8UC4);
            matResult = new Mat(normalSize, CvType.CV_8UC4);
            matTmp = new Mat(normalSize, CvType.CV_8UC4);
            matSmallSource = new Mat(smallSize, CvType.CV_8UC4);
            matSmallResult = new Mat(smallSize, CvType.CV_8UC4);
            matSmallTmp = new Mat(smallSize, CvType.CV_8UC4);
        }
        bWasInit = true;
    }
    public Size getSmallSize() {
        return smallSize;
    }
    public void Release() {
        bWasInit = false;
        matResult.release();
        matSource.release();
        matTmp.release();
        matSmallSource.release();
        matSmallResult.release();
        matSmallTmp.release();
        if(!sourceBtm.isRecycled())
            sourceBtm.recycle();
        if(!resultBtm.isRecycled())
            resultBtm.recycle();
        if(!tmpBtm.isRecycled())
            tmpBtm.recycle();
        if(!alphaBtm.isRecycled())
            alphaBtm.recycle();
        if(!smallSourceBtm.isRecycled())
            smallSourceBtm.recycle();
        if(!smallResultBtm.isRecycled())
            smallResultBtm.recycle();
        if(!smallAlphaBtm.isRecycled())
            smallAlphaBtm.recycle();
        for(Bitmap smallTmpBtm: smallTmpBtms) {
            if(!smallTmpBtm.isRecycled())
                smallTmpBtm.recycle();
        }
    }
    private MemoryManager() {

    }

}
