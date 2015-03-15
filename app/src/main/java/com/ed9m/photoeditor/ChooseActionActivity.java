package com.ed9m.photoeditor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class ChooseActionActivity extends Activity implements SeekBar.OnSeekBarChangeListener {
	private ImageView mImageView;
    private SeekBar mSeekBar;
    private LinearLayout mStrengthLayout;
    private ImageButton mShareBtn;
    private HorizontalScrollView mColorFiltersScroll;
    private ArrayList<ImageButton> mFilterButtons;
    //TODO: transfer to memoryManager
    private ArrayList<Mat> lutMats;
    private Tracker tracker;
    private File fShare;
    private MemoryManager mm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mFilterButtons = new ArrayList<ImageButton>();
        lutMats = new ArrayList<Mat>();

        mm = MemoryManager.Instance;
        Log.i("OpenCV", "Trying to load OpenCV library");
        if(!OpenCVLoader.initDebug())
        {
            Log.e("OpenCV", "Cannot connect to OpenCV Manager");
        }
        else {
            setContentView(R.layout.activity_chooseaction);
            tracker = GoogleAnalytics.getInstance(this).newTracker("UA-60747222-1");
            tracker.setScreenName(getClass().toString());
            tracker.send(new HitBuilders.AppViewBuilder().build());
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
                mm.sourceBtm = decodeSampledBitmapFromFile(image_path, MaxSizeOfImage, MaxSizeOfImage);
                Log.i("OPENING IMAGE", "size of image is " + mm.sourceBtm.getWidth() + "x" + mm.sourceBtm.getHeight());
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
                        mm.sourceBtm = RotateBitmap(mm.sourceBtm, 90);
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        Log.i("OPENING IMAGE", "rotation is transopse");
                        mm.sourceBtm = RotateBitmap(mm.sourceBtm, 270);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        Log.i("OPENING IMAGE", "rotation is 180");
                        mm.sourceBtm = RotateBitmap(mm.sourceBtm, 180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        Log.i("OPENING IMAGE", "rotation is 270");
                        mm.sourceBtm = RotateBitmap(mm.sourceBtm, 270);
                        break;
                }
                if (mm.sourceBtm != null) {
                    mImageView.setImageBitmap(mm.sourceBtm);
                    CreatePreviewColorFilters(mm.sourceBtm);
                    AdView adView = (AdView) this.findViewById(R.id.adView);
                    final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
                    if (activeNetwork != null && activeNetwork.isConnected()) {
                        AdRequest adRequest = new AdRequest.Builder().addTestDevice("6C524DC975C733EB2D79C60C05DF2D01").build();
                        adView.loadAd(adRequest);
                    } else {
                        adView.setVisibility(View.GONE);
                    }
                }

            }
        }
	}
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(fShare != null) {
            if(!fShare.delete()) {
                Log.i("SHARE FILE", "I cannot remove share file");
            }
        }
        //TODO: transfer to mm
        for(Mat lut:lutMats) {
            lut.release();
        }
        mm.Release();
        System.gc();
    }
    //SeekBar section
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        if(mm.resultBtm != null)
        {
            adjustOpacity(mm.resultBtm, mm.alphaBtm, progress);
            overlay(mm.alphaBtm,mm.sourceBtm,mm.tmpBtm);
            mImageView.setImageBitmap(mm.tmpBtm);
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
        Bitmap res = ((BitmapDrawable)mImageView.getDrawable()).getBitmap();
        res.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = Environment.getExternalStorageDirectory() + File.separator + "comely_color_result.jpg";
        try {
            fShare = new File(path);
            if(!fShare.exists()) {
                if(!fShare.createNewFile()) {
                    Log.i("SHARE FILE", "I cannot create share file");
                }
            }
            FileOutputStream fo = new FileOutputStream(fShare);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(fShare.getAbsolutePath()));
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
    }

    public void CreatePreviewColorFilters(Bitmap btm) {

        Field[] all_fields = R.drawable.class.getFields();
        ArrayList<Field> lut_fields = new ArrayList<Field>();
        int ii = 0;
        for(Field field: all_fields) {
            if (field.getName().contains("lut")) {
                lut_fields.add(field);
                Log.i("LUTS", ii + " lut is " + field.getName());
                ii+=1;
            }
        }
        mm.Init(getWindowManager().getDefaultDisplay(), lut_fields.size());
        Utils.bitmapToMat(btm, mm.matSource);

        //Imgproc.cvtColor(srcResize, srcResize, Imgproc.COLOR_RGBA2BGR);
        Imgproc.resize(mm.matSource, mm.matSmallSource, mm.getSmallSize());
        //get lut resources

        LoadLUTs(lut_fields);
        Utils.matToBitmap(mm.matSmallSource, mm.smallSourceBtm);
        Long tsLong = System.currentTimeMillis();

        for(int i = 0; i <lutMats.size(); i++) {
            ImageButton tmp = new ImageButton(this);
            if(Build.VERSION.SDK_INT >=16) {
                tmp.setBackground(null);
            }
            tmp.setOnClickListener(getOnClickImageButton(tmp));
            Lut(mm.matSmallSource, mm.matSmallTmp, lutMats.get(i), mm.smallResultBtm);
            adjustOpacity(mm.smallResultBtm, mm.smallAlphaBtm, 110);
            overlay(mm.smallAlphaBtm, mm.smallSourceBtm, mm.smallTmpBtms.get(i));
            tmp.setImageBitmap(mm.smallTmpBtms.get(i));
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
            //TODO: fix it (use all allocation in mm
            Mat tMat;
            Mat lMat = null;
            int resId = 0;
            try {
                resId = field.getInt(field);
            }
            catch (Exception e) {
                Log.e("Resources", "resId not exist: " + e.getMessage());
            }
            try {
                tMat = Utils.loadResource(this, resId, Highgui.CV_LOAD_IMAGE_COLOR);
                lMat = new Mat(tMat.size(), CvType.CV_8UC4);
                Imgproc.cvtColor(tMat,lMat,Imgproc.COLOR_BGR2RGBA);
                tMat.release();
            }
            catch (IOException e) {
                Log.e("Resources", "load resId = " + Integer.toString(resId)+ ": " + e.getMessage());
            }
            lutMats.add(lMat);
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
                        Lut(mm.matSource,mm.matTmp, lutMats.get(i),mm.resultBtm);
                        Log.i("TIMESTAMP", "time of lut for image is " + (System.currentTimeMillis() - tsLong) + " msec.");
                        adjustOpacity(mm.resultBtm, mm.alphaBtm, mSeekBar.getProgress());
                        overlay(mm.alphaBtm, mm.sourceBtm,mm.tmpBtm);
                        mImageView.setImageBitmap(mm.tmpBtm);
                    }
                }
            }
        };
    }
    private void Lut(Mat src, Mat tmp, Mat lut, Bitmap resBtm) {
        Core.LUT(src, lut, tmp);
        Utils.matToBitmap(tmp, resBtm);
    }
    private void overlay(Bitmap bmp1, Bitmap bmp2, Bitmap res)
    {
        try
        {
            Canvas canvas = new Canvas(res);
            Paint clearPaint = new Paint();
            clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawRect(0, 0, res.getWidth(),res.getHeight(), clearPaint);
            Paint paint = new Paint();
            canvas.drawBitmap(bmp1,0,0,paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));
            canvas.drawBitmap(bmp2, 0, 0, paint);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    /**
     * @param bitmap The source bitmap.
     * @param result The result bitmap.
     * @param opacity a value between 0 (completely transparent) and 255 (completely
     * opaque).
     */
    private void adjustOpacity(Bitmap bitmap, Bitmap result, int opacity)
    {
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(bitmap,0,0,new Paint());
        int colour = (opacity & 0xFF) << 24;
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
    }
    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        //TODO low: allocate Bitmap
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
