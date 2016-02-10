package com.example.michal.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

    public final String PREVIEW = "Preview";
    public final String SEGMENTATION = "Segmentation";
    public final String NORMALISATION = "Normalisation";
    public final String FILTERING = "Filtering";
    public final String BINARISATION = "Binarisation";
    public final String THINNING = "Thinning";
    public final String EXTRACTION = "Extraction";

    //type of preprocessing
    public final String AUTOMATIC = "automatic";
    public final String AUTOMATIC_FULL = "automatic_full";
    public final String MANUAL = "manual";

    //menuItem
    private static final int SELECT_PICTURE = 1;

    //intent putExtra names
    public final String BITMAP_IMAGE = "BitmapImage";
    public final String TYPE = "Type";

    //image format
    public final String JPEG = "jpeg";
    public final String PNG = "png";


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
        catch (Exception e) {
            Toast.makeText(context.getApplicationContext(), R.string.sdcard_unmounted, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }


    public void informationDialog(){
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.popup_info);
        dialog.setTitle(R.string.information);

        TextView popUpText = (TextView)dialog.findViewById(R.id.popUpText);
        Resources res = context.getResources();
        String showText = String.format(res.getString(R.string.info_text), BuildConfig.VERSION_NAME);
        popUpText.setText(showText);

        Button dialogButton = (Button) dialog.findViewById(R.id.popUpOK);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void menuItemMainActivity(int id){
        switch (id) {
            case R.id.load_image_menu:
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                context.startActivityForResult(Intent.createChooser(intent, "Vyber odtlačok"), SELECT_PICTURE);
                break;
            case R.id.information:
                informationDialog();
                break;
            case R.id.exit:
                context.finish();
                break;
        }
    }

    public void menuItemOtherActivities(int id, Bitmap mImage, String name){

        switch (id){
            case R.id.home :
                Intent i = new Intent(context, MainActivity.class);
                context.startActivity(i);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) { //due to finishAffinity(); supported from API 16
                    context.finishAffinity();
                }
                break;
            case R.id.export_image:
                saveImageToExternalStorage(mImage, name);
                break;
            case R.id.information:
                informationDialog();
                break;
            case R.id.exit:
                context.finishAffinity();
                context.finish();
                break;
        }

    }



}
