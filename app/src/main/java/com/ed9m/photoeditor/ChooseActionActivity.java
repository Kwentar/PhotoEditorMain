package com.ed9m.photoeditor;

import java.io.IOException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class ChooseActionActivity extends Activity {
	private MainDrawView mImageView;
    private Button pbtApplyRect;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chooseaction);
		Intent intent = getIntent();
		if (null != intent) {
			String image_path = intent.getStringExtra("IMAGE_PATH");
			mImageView = (MainDrawView) findViewById(R.id.image_preview);
			Bitmap btm = decodeSampledBitmapFromFile(image_path, 2048, 2048);
            Log.i("OPENING IMAGE", "size of image is " + btm.getWidth() +"x" + btm.getHeight());
            ExifInterface ei = null;
			try {
				ei = new ExifInterface(image_path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch(orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                	Log.i("OPENING IMAGE", "rotation is 90");
                	btm = RotateBitmap(btm, 90);
                    break;
                case ExifInterface.ORIENTATION_TRANSPOSE:
                	Log.i("OPENING IMAGE", "rotation is transopse");
                	btm = RotateBitmap(btm, 270);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                	Log.i("OPENING IMAGE", "rotation is 180");
                	btm = RotateBitmap(btm, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                	Log.i("OPENING IMAGE", "rotation is 270");
                	btm = RotateBitmap(btm, 270);
                    break;
            }
            if(btm != null) {
                if (android.os.Build.VERSION.SDK_INT >= 16){
                    setBackgroundV16Plus(mImageView, btm);
                }
                else{
                    setBackgroundV16Minus(mImageView, btm);
                }
                /*pbtApplyRect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        pbtApplyRectClick();
                    }
                });*/
            }

		}
	}
    public void pbtApplyRectClick() {

    }
    @TargetApi(16)
    private void setBackgroundV16Plus(View view, Bitmap bitmap) {
        view.setBackground(new BitmapDrawable(getResources(), bitmap));

    }

    @SuppressWarnings("deprecation")
    private void setBackgroundV16Minus(View view, Bitmap bitmap) {
        view.setBackgroundDrawable(new BitmapDrawable(bitmap));
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
