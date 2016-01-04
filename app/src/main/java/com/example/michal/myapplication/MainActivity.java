package com.example.michal.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.michal.myapplication.ftrScan.FtrScanDemoUsbHostActivity;
import com.example.michal.myapplication.ftrScan.SelectFileFormatActivity;

import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;


public class MainActivity extends AppCompatActivity {

    private static final String IMAGE_PATH = "/sdcard/Fingerprints/obrazok.bmp";

    private static final int SELECT_PICTURE = 1;

    private static Help help;
    private static Toolbar toolbar;
    private static Button mLoadImage;
    private static Button mScanImage;
    private static Bitmap image;
    private String selectedImagePath;

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

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        help = new Help(this);

        File extPrivateStorageDirectory = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES); //private directory is erased after uninstall
        final File mDir = new File(extPrivateStorageDirectory, "Fingerprints");

        mLoadImage = (Button) findViewById(R.id.load_image);
        mLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //image = BitmapFactory.decodeFile(IMAGE_PATH);
                //startPreview(image, 1);
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

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                selectedImagePath = getPath(selectedImageUri);
                image = BitmapFactory.decodeFile(selectedImagePath);
                if( selectedImagePath.substring(selectedImagePath.length() - 3).equals("jpg") ) {
                    startPreview(image, 0); // 0 = .jpg
                }else{
                    startPreview(image, 1); // 0 = .png
                }
            }
        }
        if (resultCode == Activity.RESULT_FIRST_USER){
            selectedImagePath =  data.getExtras().getString("fileName");
            image = BitmapFactory.decodeFile(selectedImagePath);
            startPreview(image, 1);
        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.home:
                break;
            case R.id.load_image:
                break;
            case R.id.export_image:
                break;
            case R.id.information:
                help.informationDialog();
                break;
        }

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

        Intent i = new Intent(this, Preview.class);
        i.putExtra("BitmapImage", byteArray);
        startActivity(i);
    }
}
