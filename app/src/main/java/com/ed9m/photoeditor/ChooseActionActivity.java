package com.ed9m.photoeditor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ChooseActionActivity extends Activity implements SeekBar.OnSeekBarChangeListener {
	private ImageView mImageView;
    private SeekBar mSeekBar;
    private LinearLayout mStrengthLayout;
    private ImageButton mShareBtn;
    private Bitmap sourceBtm;
    private Bitmap resultBtm;
    private HorizontalScrollView mColorFiltersScroll;
    private ArrayList<ImageButton> mFilterButtons;
    private ArrayList<Mat> lutMats;
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mFilterButtons = new ArrayList<ImageButton>();
        lutMats = new ArrayList<Mat>();
        Log.i("OpenCV", "Trying to load OpenCV library");
        if(!OpenCVLoader.initDebug())
        {
            Log.e("OpenCV", "Cannot connect to OpenCV Manager");
        }
        else {
            setContentView(R.layout.activity_chooseaction);

            Intent intent = getIntent();
            if (null != intent) {
                String image_path = intent.getStringExtra("IMAGE_PATH");
                mImageView = (ImageView) findViewById(R.id.image_preview);
                mSeekBar = (SeekBar)findViewById(R.id.seekBar);
                mSeekBar.setOnSeekBarChangeListener(this);
                mStrengthLayout = (LinearLayout)findViewById(R.id.layout_strength);
                mColorFiltersScroll = (HorizontalScrollView)findViewById(R.id.color_filter_scroll);
                mShareBtn = (ImageButton)findViewById(R.id.imgBtnShare);
                int MaxSizeOfImage = 1536;
                sourceBtm = decodeSampledBitmapFromFile(image_path, MaxSizeOfImage, MaxSizeOfImage);
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
                    CreatePreviewColorFilters(sourceBtm);
                    // Поиск AdView как ресурса и отправка запроса.
                    AdView adView = (AdView)this.findViewById(R.id.adView);
                    AdRequest adRequest = new AdRequest.Builder().build();
                    adView.loadAd(adRequest);
                }

            }
        }
	}
    //SeekBar section
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if(resultBtm != null)
        {
            Bitmap tmp = adjustOpacity(resultBtm, progress).copy(resultBtm.getConfig(),false);
            mImageView.setImageBitmap(overlay(tmp,sourceBtm));
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
    //End Seekbar section
    public void onShare(View v) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/jpeg");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        resultBtm.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = Environment.getExternalStorageDirectory() + File.separator + "autoRetouch_result.jpg";
        File f = new File(path);
        try {
            if(f.createNewFile()) {
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(bytes.toByteArray());
                fo.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(path));
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));

    }

    public void CreatePreviewColorFilters(Bitmap btm) {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int maxSize = size.x/4;


        Field[] all_fields = R.drawable.class.getFields();
        ArrayList<Field> lut_fields = new ArrayList<Field>();
        Size goalSize = new Size();
        if(btm.getWidth() > btm.getHeight()) {
            goalSize.width = maxSize;
            goalSize.height = btm.getHeight() * maxSize / btm.getWidth();
        } else {
            goalSize.height = maxSize;
            goalSize.width = btm.getWidth() * maxSize / btm.getHeight();
        }
        Mat srcResize = new Mat();
        Utils.bitmapToMat(btm, srcResize);
        Imgproc.cvtColor(srcResize, srcResize, Imgproc.COLOR_RGBA2BGR);
        Imgproc.resize(srcResize, srcResize, goalSize);
        //get lut resources
        int ii = 0;
        for(Field field: all_fields) {
            if (field.getName().contains("lut")) {
                lut_fields.add(field);
                Log.i("LUTS", ii + " lut is " + field.getName());
                ii+=1;
            }
        }
        LoadLUTs(lut_fields);

        Mat srcRGBA = new Mat();
        Imgproc.cvtColor(srcResize,srcRGBA,Imgproc.COLOR_BGR2RGBA);
        Bitmap srcBtm = Bitmap.createBitmap(srcResize.width(), srcResize.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(srcRGBA, srcBtm);
        Long tsLong = System.currentTimeMillis();
        for(Mat lut: lutMats) {
            ImageButton tmp = new ImageButton(this);
            if(Build.VERSION.SDK_INT >=16) {
                tmp.setBackground(null);
            }
            tmp.setOnClickListener(getOnClickImageButton(tmp));
            Bitmap resBtm = Lut(srcBtm, lut);
            resBtm = adjustOpacity(resBtm,110);

            tmp.setImageBitmap(overlay(resBtm,srcBtm));
            mFilterButtons.add(tmp);
        }
        Long tsLongEnd = System.currentTimeMillis();
        Log.i("TIMESTAMP", "time of lut for buttons is " + (tsLongEnd - tsLong) + " msec." );
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        for(ImageButton imageButton : mFilterButtons) {
            layout.addView(imageButton);
        }

        mColorFiltersScroll.addView(layout);

    }

    private void LoadLUTs(ArrayList<Field> lut_fields) {
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
    }

    View.OnClickListener getOnClickImageButton(final ImageButton button)  {
        return new View.OnClickListener() {
            public void onClick(View v) {
                for(int i = 0; i < mFilterButtons.size(); i++) {
                    if(mStrengthLayout.getVisibility() == View.GONE ||
                            mShareBtn.getVisibility() == View.GONE) {
                        mStrengthLayout.setVisibility(View.VISIBLE);
                        mShareBtn.setVisibility(View.VISIBLE);
                    }
                    if(mFilterButtons.get(i) == button) {
                        Long tsLong = System.currentTimeMillis();
                        resultBtm = Lut(sourceBtm, lutMats.get(i));
                        Log.i("TIMESTAMP", "time of lut for image is " + (System.currentTimeMillis() - tsLong) + " msec." );
                        Bitmap tmp = adjustOpacity(resultBtm, mSeekBar.getProgress()).copy(resultBtm.getConfig(), true);
                        mImageView.setImageBitmap(overlay(tmp,sourceBtm));
                    }
                }
            }
        };
    }
    private Bitmap Lut(Bitmap src, Mat lut) {
        Mat resMat = new Mat();
        Mat srcMat = new Mat();
        Utils.bitmapToMat(src, srcMat);
        Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2BGR);
        Core.LUT(srcMat, lut, resMat);
        Imgproc.cvtColor(resMat,resMat,Imgproc.COLOR_BGR2RGBA);

        Bitmap resBtm = Bitmap.createBitmap(srcMat.width(), srcMat.height(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(resMat, resBtm);
        return resBtm;
    }
    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2)
    {
        try
        {
            Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(),  bmp1.getConfig());
            Canvas canvas = new Canvas(bmOverlay);
            Paint paint = new Paint();
            canvas.drawBitmap(bmp1, 0, 0, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));

            canvas.drawBitmap(bmp2, 0, 0, paint);
            return bmOverlay;

        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * @param bitmap The source bitmap.
     * @param opacity a value between 0 (completely transparent) and 255 (completely
     * opaque).
     * @return The opacity-adjusted bitmap.  If the source bitmap is mutable it will be
     * adjusted and returned, otherwise a new bitmap is created.
     */
    private Bitmap adjustOpacity(Bitmap bitmap, int opacity)
    {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        int colour = (opacity & 0xFF) << 24;
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
        return mutableBitmap;
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
