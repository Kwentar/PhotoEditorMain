package com.ed9m.photoeditor;

import java.io.IOException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class ChooseActionActivity extends Activity {
	private MainDrawView mImageView;
    private Button pbtApplyRect;
    private Bitmap sourceBtm;
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
                mImageView = (MainDrawView) findViewById(R.id.image_preview);
                pbtApplyRect = (Button) findViewById(R.id.pbtApplyRect);
                sourceBtm = decodeSampledBitmapFromFile(image_path, 2048, 2048);
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
                    pbtApplyRect.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pbtApplyRectClick();
                        }
                    });
                }

            }
        }
	}
    public void pbtApplyRectClick() {
        if(mImageView.roi.width == 0 || mImageView.roi.height == 0) {
            Toast.makeText(getApplicationContext(),R.string.draw_rect,Toast.LENGTH_LONG).show();
        }
        else {
            Mat bgModel = new Mat();
            Mat fgModel = new Mat();
            Mat sourceMat = new Mat();
            Mat bg = new Mat();
            Utils.bitmapToMat(sourceBtm,sourceMat);
            Imgproc.cvtColor(sourceMat, sourceMat, Imgproc.COLOR_BGRA2BGR);
            Imgproc.grabCut(sourceMat, bg, mImageView.roi, bgModel, fgModel, 1, Imgproc.GC_INIT_WITH_RECT);
            bg = bg.mul(bg,10.);
            Imgproc.cvtColor(bg, bg, Imgproc.COLOR_GRAY2BGRA);
            Utils.matToBitmap(bg,sourceBtm);
        }
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

            final int halfHeight = height;
            final int halfWidth = width;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    || (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
