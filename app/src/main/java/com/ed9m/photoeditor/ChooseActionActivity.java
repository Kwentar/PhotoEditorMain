package com.ed9m.photoeditor;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ChooseActionActivity extends Activity {
	private ImageView mImageView;
    private Bitmap sourceBtm;
    private HorizontalScrollView colorFiltersScroll;
    private ArrayList<ImageButton> filterButtons;
    private ArrayList<Mat> lutMats;
    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    // Create and set View

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        filterButtons = new ArrayList<ImageButton>();
        lutMats = new ArrayList<Mat>();
        Log.i("OpenCV", "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mOpenCVCallBack))
        {
            Log.e("OpenCV", "Cannot connect to OpenCV Manager");
        }

        else {
            setContentView(R.layout.activity_chooseaction);
            Intent intent = getIntent();
            if (null != intent) {
                String image_path = intent.getStringExtra("IMAGE_PATH");
                mImageView = (ImageView) findViewById(R.id.image_preview);
                colorFiltersScroll = (HorizontalScrollView)findViewById(R.id.color_filter_scroll);
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int width = size.x;
                int height = size.y;
                sourceBtm = decodeSampledBitmapFromFile(image_path, width, height);
                Log.i("OPENING IMAGE", "size of image is " + sourceBtm.getWidth() + "x" + sourceBtm.getHeight());
                ExifInterface ei = null;
                try {
                    ei = new ExifInterface(image_path);
                } catch (IOException e) {

                    e.printStackTrace();
                }
                int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        Log.i("OPENING IMAGE", "rotation is 90");
                        sourceBtm = RotateBitmap(sourceBtm, 90);
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        Log.i("OPENING IMAGE", "rotation is transopse");
                        sourceBtm = RotateBitmap(sourceBtm, 270);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        Log.i("OPENING IMAGE", "rotation is 180");
                        sourceBtm = RotateBitmap(sourceBtm, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        Log.i("OPENING IMAGE", "rotation is 270");
                        sourceBtm = RotateBitmap(sourceBtm, 270);
                        break;
                }
                if (sourceBtm != null) {
                    mImageView.setImageBitmap(sourceBtm);

                }

            }
        }
	}
    public void testClick(View view) {
        CreatePreviewColorFilters(sourceBtm);
    }
    public void CreatePreviewColorFilters(Bitmap btm) {
        int maxSize = 200;
        Field[] all_fields = R.drawable.class.getFields();
        ArrayList<Field> lut_fields = new ArrayList<Field>();
        Size goalSize = new Size();
        if(btm.getWidth() > btm.getHeight()) {
            goalSize.width = 200;
            goalSize.height = btm.getHeight() * maxSize / btm.getWidth();
        } else {
            goalSize.height = 200;
            goalSize.width = btm.getWidth() * maxSize / btm.getHeight();
        }
        Mat srcResize = new Mat((int)goalSize.height, (int)goalSize.width, CvType.CV_8UC4);
        Utils.bitmapToMat(btm, srcResize);
        Imgproc.cvtColor(srcResize, srcResize, Imgproc.COLOR_RGBA2BGR);
        Imgproc.resize(srcResize, srcResize, goalSize);
        //get lut resources
        for(Field field: all_fields) {
            Log.i("Raw Asset: ", field.getName());
            if (field.getName().contains("lut")) {
                lut_fields.add(field);
            }
        }
        //load lut resources
        for(Field field : lut_fields) {
            Mat tMat = null;
            int resId = 0;
            try {
                resId = field.getInt(field);
            }
            catch (Exception e) {
                Log.e("Resources", "resId not exist: " + e.getMessage());
            }
            try {
                tMat = Utils.loadResource(this, resId);
            }
            catch (IOException e) {
                Log.e("Resources", "load resId = " + Integer.toString(resId)+ ": " + e.getMessage());
            }
            //Imgproc.cvtColor(tMat, tMat, Imgproc.COLOR_BGR2RGBA);
            lutMats.add(tMat);
        }
        Mat resMat = new Mat();

        for(Mat lut: lutMats) {
            ImageButton tmp = new ImageButton(this);
            Core.LUT(srcResize, lut, resMat);
            resMat = resMat.mul(srcResize);
            Imgproc.cvtColor(resMat,resMat,Imgproc.COLOR_BGR2RGBA);
            Bitmap resBtm = Bitmap.createBitmap(srcResize.width(), srcResize.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(resMat, resBtm);
            tmp.setImageBitmap(resBtm);
            filterButtons.add(tmp);
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        for(ImageButton imageButton : filterButtons) {
            layout.addView(imageButton);
        }

        //добавили кнопку в GridLayout, применив к ней параметры gridLayoutParam (кнопка появится в ячейке row;column)


        colorFiltersScroll.addView(layout);

    }
    public void pbtApplyRectClick() {

        /*if(mImageView.roi.width == 0 || mImageView.roi.height == 0) {
            Toast.makeText(getApplicationContext(),R.string.draw_rect,Toast.LENGTH_LONG).show();
        }
        else {
            Mat bgModel = new Mat();
            Mat fgModel = new Mat();
            Mat sourceMat = new Mat();
            Mat bg = new Mat();
            int max_size = 256;
            int w,h;
            float w_coef, h_coef;
            Utils.bitmapToMat(sourceBtm,sourceMat);
            if(sourceMat.height() > sourceMat.width()) {
                h = max_size;
                w = max_size * sourceMat.width() / sourceMat.height();
            }
            else {
                w = max_size;
                h = max_size * sourceMat.height() / sourceMat.width();

            }
            w_coef = (float)sourceMat.width() / w;
            h_coef = (float)sourceMat.height() / h;
            mImageView.roi.x = (int)((float)mImageView.roi.x / w_coef);
            mImageView.roi.width = (int)((float)mImageView.roi.width / w_coef);
            mImageView.roi.y = (int)((float)mImageView.roi.y / h_coef);
            mImageView.roi.height = (int)((float)mImageView.roi.height / h_coef);
            if (mImageView.roi.height + mImageView.roi.y > h) {
                mImageView.roi.height = h - mImageView.roi.y - 1;
            }
            if (mImageView.roi.width + mImageView.roi.x > w) {
                mImageView.roi.width = w - mImageView.roi.x - 1;
            }
            Imgproc.resize(sourceMat,sourceMat,new Size(w,h));
            Imgproc.cvtColor(sourceMat, sourceMat, Imgproc.COLOR_BGRA2BGR);
            Imgproc.grabCut(sourceMat, bg, mImageView.roi, bgModel, fgModel, 1, Imgproc.GC_INIT_WITH_RECT);
            bg = bg.mul(bg,10.);
            Imgproc.resize(bg,bg,new Size(sourceBtm.getWidth(),sourceBtm.getHeight()));
            Imgproc.cvtColor(bg, bg, Imgproc.COLOR_GRAY2BGRA);
            Utils.matToBitmap(bg,sourceBtm);
        }*/
    }


    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
          Matrix matrix = new Matrix();
          matrix.postRotate(angle);
          return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap decodeSampledBitmapFromFile(String picturePath,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picturePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(picturePath, options);
    }
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((height / inSampleSize) > reqHeight
                    || (width / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
