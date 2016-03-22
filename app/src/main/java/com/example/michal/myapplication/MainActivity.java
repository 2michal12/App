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
import java.io.File;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;


public class MainActivity extends AppCompatActivity {

    private static final int SELECT_PICTURE = 1;
    private static Help help;
    private static Bitmap image;

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.load_image)
    Button mLoadImage;

    @Bind(R.id.scan_image)
    Button mScanImage;

    @Bind(R.id.name_version)
    TextView version;

    @Bind(R.id.test_extraction)//po testovani vymazat
    Button test;

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
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        if( getSupportActionBar() != null )
            getSupportActionBar().setTitle(R.string.app_name);

        //po stetovani vymazat
        test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //image = BitmapFactory.decodeFile("storage/emulated/0/Pictures/DIPLOMOVKA/FtrScan/skeleton13.bmp");
                //image = BitmapFactory.decodeResource(getResources(), R.drawable.skeleton);
                //startPreview(image, 2);
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Vyber odtlačok"), 2);
            }
        });


        help = new Help(this);

        mLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Vyber odtlačok"), SELECT_PICTURE);
            }
        });

        mScanImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent scan = new Intent(MainActivity.this, FtrScanDemoUsbHostActivity.class);
                startActivityForResult(scan, Activity.RESULT_FIRST_USER);
            }
        });

        Resources res = getResources();
        String app_version = String.format(res.getString(R.string.app_version_text), BuildConfig.VERSION_NAME);
        version.setText(app_version);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //for testing of extraction
        if(requestCode == 2){
            requestCode = SELECT_PICTURE;
            help.FORMAT_BMP = 2;
            help.FORMAT_JPEG = 2;
        }

        if( data.getData() == null) { //condition for loading image directly from scanning process
            Uri uriCreated = Uri.fromFile(new File(data.getStringExtra("fileName")));
            try {
                image = MediaStore.Images.Media.getBitmap(getContentResolver(), uriCreated);
            } catch (IOException e) {
                e.printStackTrace();
            }
            startPreview(image, help.FORMAT_BMP);
        }

        if (requestCode == SELECT_PICTURE && resultCode == RESULT_OK && data.getData() != null) {

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

                if( strMimeType.contains(help.JPEG) ){
                    startPreview(image, help.FORMAT_JPEG); // 0 = .jpg
                }else if( strMimeType.contains(help.PNG) ){
                    startPreview(image, help.FORMAT_BMP); // 1 = .png
                }else{
                    startPreview(image, help.FORMAT_BMP); // 1 = .png and .bmp too
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

        i.putExtra(help.BITMAP_IMAGE, byteArray);
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
