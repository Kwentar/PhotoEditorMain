package com.ed9m.photoeditor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

public class CurvesActivity extends Activity {
    public void onApplyClick(View view) {
        mainDrawView.bDrawCircles = !mainDrawView.bDrawCircles;
        mainDrawView.invalidate();
    }

    private FileSystemManager fm;
    private MainDrawView mainDrawView;
    private FrameLayout frameLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_curves);
        fm = FileSystemManager.Instance;
        frameLayout = (FrameLayout)findViewById(R.id.curves_frame);
        mainDrawView = (MainDrawView)findViewById(R.id.curves_image_view);
        Intent intent = getIntent();
        if(intent != null) {
            String image_path = intent.getStringExtra("IMAGE_PATH");
            int MaxSizeOfImage = 1536;
            Bitmap btm = fm.GetBitmapFromFile(image_path, MaxSizeOfImage, MaxSizeOfImage);
            mainDrawView.setImageBitmap(btm);
        }
    }


}
