package com.ed9m.photoeditor;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


@SuppressLint("InlinedApi")
public class FullscreenActivity extends Activity {
	private LinearLayout main_layout;
	private String 	mCurrentPhotoPath;
	private ImageButton ibtGallery;
	private ImageButton ibtCamera;
	private static final int SELECT_PICTURE_GALLERY = 1;
    private static final int SELECT_PICTURE_CAMERA = 2;
    
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fullscreen);
        // Поиск AdView как ресурса и отправка запроса.
        AdView adView = (AdView)this.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().addTestDevice("6C524DC975C733EB2D79C60C05DF2D01").build();
        adView.loadAd(adRequest);
		main_layout = (LinearLayout)findViewById(R.id.main_layout);
		main_layout.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
					hideSystemUI();				
				} 
				switch (event.getAction()) {	
			    case MotionEvent.ACTION_UP:
			        v.performClick();
			        break;
				}
				return false;
			}
		});
		ibtGallery = (ImageButton)findViewById(R.id.ibt_gallery);
		ibtGallery.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ChooseImageFromGallery();
			}
		});
		ibtCamera = (ImageButton)findViewById(R.id.ibt_camera);
		ibtCamera.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TakePhotoFromCamera();
			}
		});
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			hideSystemUI();				
		} 
	}

	@Override
    protected void onDestroy() {
        super.onDestroy();
    }

	private void hideSystemUI() {
		getWindow().getDecorView().setSystemUiVisibility(
	            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
	            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
	            | View.SYSTEM_UI_FLAG_IMMERSIVE);
	}
	
	public void ChooseImageFromGallery() {
	    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
		photoPickerIntent.setType("image/*");
		startActivityForResult(photoPickerIntent, SELECT_PICTURE_GALLERY);
	}
	
	public void TakePhotoFromCamera() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
	        File photoFile = null;
	        try {
	            photoFile = createImageFile();
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	        if (photoFile != null) {
	            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(photoFile));
	            startActivityForResult(takePictureIntent, SELECT_PICTURE_CAMERA);
	        }
	    }
	}
    //TODO set from FileSystemManager
	@SuppressLint("SimpleDateFormat")
	private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
	    
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE_GALLERY) {
            	Uri selectedImage = data.getData();
                String[] filePathColumn = { MediaStore.Images.Media.DATA };
                Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                mCurrentPhotoPath = cursor.getString(columnIndex);
                cursor.close();
            }
            Intent intent = new Intent(getApplicationContext(), ChooseActionActivity.class);
            intent.putExtra("IMAGE_PATH", mCurrentPhotoPath);
            startActivity(intent);
        }
	}
 
}
