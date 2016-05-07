package com.example.michal.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
                overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left);
            }
        });

        Resources res = getResources();
        String app_version = String.format(res.getString(R.string.app_version_text), BuildConfig.VERSION_NAME);
        version.setText(app_version);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 2){        //for testing of extraction
            requestCode = SELECT_PICTURE;
            help.FORMAT_BMP = 2;
            help.FORMAT_JPEG = 2;
        }

        if( data != null && data.getData() == null ) { //condition for loading image directly from scanning process
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
                    startPreview(image, help.FORMAT_JPEG);
                }else if( strMimeType.contains(help.PNG) ){
                    startPreview(image, help.FORMAT_BMP);
                }else{
                    startPreview(image, help.FORMAT_BMP);
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
        menu.getItem(0).setVisible(false);
        menu.getItem(2).setVisible(false);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) { //due to finishAffinity(); supported from API 16
            menu.getItem(4).setVisible(false);
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

        Intent i;
        i = new Intent(this, Preview.class);
        i.putExtra(help.BITMAP_IMAGE, byteArray);
        startActivity(i);
        overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setTitle(getResources().getString(R.string.exit_question_title));

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MainActivity.this.finish();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}
