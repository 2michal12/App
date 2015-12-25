package com.example.michal.myapplication;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Michal on 10.11.2015.
 */
public class Help {

    Activity context;

    Help(){}

    Help(Activity context){
        this.context = context;
    }

    public Mat bitmap2mat(Bitmap src){
        Mat dest = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, dest);
        return dest;
    }

    public void saveImageToExternalStorage(Bitmap finalBitmap, String name) {
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/DIPLOMOVKA/Output");
        myDir.mkdirs();

        File file = new File(myDir, name+".jpg");
        if (file.exists())
            file.delete();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // Tell the media scanner about the new file so that it is immediately available to the user.
        MediaScannerConnection.scanFile(context, new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });

        Toast.makeText(context.getApplicationContext(), R.string.image_saved, Toast.LENGTH_SHORT).show();
    }
}
