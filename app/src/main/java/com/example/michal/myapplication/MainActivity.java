package com.example.michal.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.michal.myapplication.ftrScan.FtrScanDemoUsbHostActivity;

import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    private static final String JPEG = "jpeg";
    private static final String PNG = "png";

    private static final int SELECT_PICTURE = 1;

    private static Help help;
    private static Toolbar toolbar;
    private static Button mLoadImage;
    private static Button mScanImage;
    private static Bitmap image;
    private static TextView version;

    //po testovani vymazat
    private static Button test;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.i("opencv", "opencv failed");
        } else {
            Log.i("opencv", "opencv initialized");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //po stetovani vymazat
        test = (Button) findViewById(R.id.test_extraction);
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image = BitmapFactory.decodeFile("storage/emulated/0/Pictures/DIPLOMOVKA/FtrScan/skeleton3.bmp");
                startPreview(image, 2);
            }
        });

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        help = new Help(this);

        mLoadImage = (Button) findViewById(R.id.load_image);
        mLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Vyber odtlačok"), SELECT_PICTURE);
            }
        });

        mScanImage = (Button) findViewById(R.id.scan_image);

        mScanImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent scan = new Intent(MainActivity.this, FtrScanDemoUsbHostActivity.class);
                startActivityForResult(scan, Activity.RESULT_FIRST_USER);
            }
        });

        Resources res = getResources();
        String app_version = String.format(res.getString(R.string.app_version_text), BuildConfig.VERSION_NAME);
        version = (TextView)findViewById(R.id.name_version);
        version.setText(app_version);
    }



    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            Cursor cursor = this.getContentResolver().query(uri,
                    new String[]{MediaStore.MediaColumns.MIME_TYPE},
                    null, null, null);

            String strMimeType = null;

            if (cursor != null && cursor.moveToNext())
            {
                strMimeType = cursor.getString(0); //format of loaded image
            }

            try {
                image = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                if( strMimeType.contains(JPEG) ){
                    startPreview(image, 0); // 0 = .jpg
                }else if( strMimeType.contains(PNG) ){
                    startPreview(image, 1); // 1 = .png
                }else{
                    startPreview(image, 1); // 1 = .png for .bmp too
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        menu.getItem(0).setVisible(false); //home
        menu.getItem(2).setVisible(false); //export_image

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) { //due to finishAffinity(); supported from API 16
            menu.getItem(4).setVisible(false);  //exit app
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        help.menuItemMainActivity(id);
        return super.onOptionsItemSelected(item);
    }

    private void startPreview(Bitmap image, int format) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if( format == 0 ) {
            image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        }else{
            image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        byte[] byteArray = stream.toByteArray();


        //docasne kym je tlacidlo test potom len Preview.class
        Intent i;
        if(format == 2) { // test
            i = new Intent(this, MaxMin.class);
        }else{
            i = new Intent(this, Preview.class);
        }
        i.putExtra("BitmapImage", byteArray);
        startActivity(i);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(getResources().getString(R.string.exit_question_title));

        alertDialogBuilder
                //.setMessage("Click yes to exit!")
                .setCancelable(false)
                .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        MainActivity.this.finish();
                    }
                })
                .setNegativeButton("No",new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
