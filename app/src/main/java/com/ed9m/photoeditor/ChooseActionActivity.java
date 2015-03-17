package com.ed9m.photoeditor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import android.app.Activity;
import android.content.ContentValues;
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
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class ChooseActionActivity extends Activity implements SeekBar.OnSeekBarChangeListener, Spinner.OnItemSelectedListener {
	private ImageView mImageView;
    private SeekBar mSeekBar;
    private Spinner blend_spinner;
    private LinearLayout mStrengthLayout;
    private LinearLayout mInnerScrollLayout;
    private ImageButton mShareBtn;
    private ArrayList<ImageButton> mFilterButtons;
    private Tracker tracker;
    private PorterDuff.Mode currBlendMode;
    private MemoryManager mm;
    private FileSystemManager fm;
    private int currentLutIndex;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        currentLutIndex = -1;
        mFilterButtons = new ArrayList<ImageButton>();
        mm = MemoryManager.Instance;
        fm = FileSystemManager.Instance;
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
                mInnerScrollLayout = (LinearLayout)findViewById(R.id.inner_scroll_layout);
                mShareBtn = (ImageButton)findViewById(R.id.imgBtnShare);
                blend_spinner = (Spinner) findViewById(R.id.blend_spinner);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                        R.array.blend_modes, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                blend_spinner.setAdapter(adapter);
                blend_spinner.setOnItemSelectedListener(this);
                currBlendMode = PorterDuff.Mode.OVERLAY;
                int MaxSizeOfImage = 1536;
                mm.sourceBtm = fm.GetBitmapFromFile(image_path, MaxSizeOfImage, MaxSizeOfImage);

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
            blend(mm.alphaBtm, mm.sourceBtm, mm.tmpBtm);
            mImageView.setImageBitmap(mm.tmpBtm);
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
    //End SeekBar section
    //Spinner section
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        String currModeStr = adapterView.getItemAtPosition(pos).toString();
        Log.i("SELECTED ITEM",currModeStr);
        boolean bNothingChange = false;
        String[] blendModes = getResources().getStringArray(R.array.blend_modes);
        if(currModeStr.equals(blendModes[0])) {
            if(currBlendMode == PorterDuff.Mode.OVERLAY)
                bNothingChange = true;
            currBlendMode = PorterDuff.Mode.OVERLAY;
        }
        else if(currModeStr.equals(blendModes[1])) {
            currBlendMode = PorterDuff.Mode.LIGHTEN;
        }
        else if(currModeStr.equals(blendModes[2])) {
            currBlendMode = PorterDuff.Mode.DST_OVER;
        }
        else if(currModeStr.equals(blendModes[3])) {
            currBlendMode = PorterDuff.Mode.SCREEN;
        }
        else if(currModeStr.equals(blendModes[4])) {
            currBlendMode = PorterDuff.Mode.DARKEN;
        }
        if(mm.smallSourceBtm != null && !bNothingChange) {
            InitButtonsLayout();
            if(currentLutIndex != -1) {
                Lut(mm.matSource, mm.matTmp, mm.lutMats.get(currentLutIndex), mm.resultBtm);
                adjustOpacity(mm.resultBtm, mm.alphaBtm, mSeekBar.getProgress());
                blend(mm.alphaBtm, mm.sourceBtm, mm.tmpBtm);
                mImageView.setImageBitmap(mm.tmpBtm);
            }
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
    //End Spinner section
    public void onStartCurves(View v) {
        File fShare = fm.SaveImageFromView(((BitmapDrawable)mImageView.getDrawable()).getBitmap());
        Intent intent = new Intent(getApplicationContext(), CurvesActivity.class);
        intent.putExtra("IMAGE_PATH", fShare.getAbsolutePath());
        startActivity(intent);
    }
    public void onShare(View v) {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/jpeg");
        File fShare = fm.SaveImageFromView(((BitmapDrawable)mImageView.getDrawable()).getBitmap());
        //share.putExtra(Intent.EXTRA_STREAM, Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), "file:///" + path to myfile.png", "Sample title", "Sample description")));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(),fShare.getAbsolutePath(),"Comely color", "")));
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
    }


    public void CreatePreviewColorFilters(Bitmap btm) {

        Field[] all_fields = R.drawable.class.getFields();
        ArrayList<Field> lut_fields = new ArrayList<Field>();
        for(Field field: all_fields) {
            if (field.getName().contains("lut")) {
                lut_fields.add(field);
            }
        }
        mm.Init(getWindowManager().getDefaultDisplay(), lut_fields.size());
        Utils.bitmapToMat(btm, mm.matSource);
        Imgproc.resize(mm.matSource, mm.matSmallSource, mm.getSmallSize());

        mm.LoadLUTs(lut_fields, this);
        Utils.matToBitmap(mm.matSmallSource, mm.smallSourceBtm);
        InitButtonsLayout();

    }

    public void InitButtonsLayout() {
        mInnerScrollLayout.removeAllViews();
        mFilterButtons.clear();
        Long tsLong = System.currentTimeMillis();
        for(int i = 0; i <mm.lutMats.size(); i++) {
            ImageButton tmp = new ImageButton(this);
            if(Build.VERSION.SDK_INT >=16) {
                tmp.setBackground(null);
            }
            tmp.setOnClickListener(getOnClickImageButton(tmp));
            Lut(mm.matSmallSource, mm.matSmallTmp, mm.lutMats.get(i), mm.smallResultBtm);
            adjustOpacity(mm.smallResultBtm, mm.smallAlphaBtm, 110);
            blend(mm.smallAlphaBtm, mm.smallSourceBtm, mm.smallTmpBtms.get(i));
            tmp.setImageBitmap(mm.smallTmpBtms.get(i));
            mFilterButtons.add(tmp);
        }
        Long tsLongEnd = System.currentTimeMillis();
        Log.i("TIMESTAMP", "time of lut for buttons is " + (tsLongEnd - tsLong) + " msec." );
        for(ImageButton imageButton : mFilterButtons) {
            mInnerScrollLayout.addView(imageButton);
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
                        Lut(mm.matSource,mm.matTmp, mm.lutMats.get(i),mm.resultBtm);
                        Log.i("TIMESTAMP", "time of lut for image is " + (System.currentTimeMillis() - tsLong) + " msec.");
                        adjustOpacity(mm.resultBtm, mm.alphaBtm, mSeekBar.getProgress());
                        blend(mm.alphaBtm, mm.sourceBtm, mm.tmpBtm);
                        currentLutIndex = i;
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
    private void blend(Bitmap bmp1, Bitmap bmp2, Bitmap res)
    {
        try
        {
            Canvas canvas = new Canvas(res);
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            canvas.drawBitmap(bmp1,0,0,paint);
            paint.setXfermode(new PorterDuffXfermode(currBlendMode));
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

}
