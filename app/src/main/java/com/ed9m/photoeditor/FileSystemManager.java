package com.ed9m.photoeditor;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileSystemManager {
    public static final FileSystemManager Instance = new FileSystemManager();
    private File imageFile;
    private FileSystemManager() {

    }
    public Bitmap GetBitmapFromFile(String imagePath,
                                   int reqWidth, int reqHeight) {
        Bitmap res = decodeSampledBitmapFromFile(imagePath, reqWidth, reqWidth);
        Log.i("OPENING IMAGE", "size of image is " + res.getWidth() + "x" + res.getHeight());
        ExifInterface ei = null;
        try {
            ei = new ExifInterface(imagePath);
        } catch (IOException e) {

            e.printStackTrace();
        }
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                Log.i("OPENING IMAGE", "rotation is 90");
                res = RotateBitmap(res, 90);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                Log.i("OPENING IMAGE", "rotation is transpose");
                res = RotateBitmap(res, 270);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                Log.i("OPENING IMAGE", "rotation is 180");
                res = RotateBitmap(res, 180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                Log.i("OPENING IMAGE", "rotation is 270");
                res = RotateBitmap(res, 270);
                break;
        }
        return res;
    }

    public File SaveImageFromView(Bitmap btm) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        btm.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = Environment.getExternalStorageDirectory() + File.separator + "comely_color_result.jpg";
        try {
            imageFile = new File(path);
            if(!imageFile.exists()) {
                if(!imageFile.createNewFile()) {
                    Log.i("SHARE FILE", "I cannot create share file");
                }
            }
            FileOutputStream fo = new FileOutputStream(imageFile);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageFile;
    }
    public Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        //TODO low: allocate Bitmap
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public Bitmap decodeSampledBitmapFromFile(String picturePath,
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
    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
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
