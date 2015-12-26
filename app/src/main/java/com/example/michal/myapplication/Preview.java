package com.example.michal.myapplication;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Preview extends AppCompatActivity {

    private static Help help;
    private static Toolbar toolbar;

    private static ImageView mLoadedImage;
    private static Bitmap mImage;
    private static Button mPreprocessing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        help = new Help(this);

        mLoadedImage = (ImageView) findViewById(R.id.view_loaded_image);
        mPreprocessing = (Button) findViewById(R.id.preprocessing);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.preview);

        setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayShowHomeEnabled(true);
        //getSupportActionBar().setIcon(R.drawable.button_background_selector);

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        if(byteArray != null) {

            mImage = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            //mImage.recycle();
            mLoadedImage.setImageBitmap(mImage);

            mPreprocessing.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPreprocessing(mImage);
                }
            });
        }else{
            mLoadedImage.setImageResource(R.drawable.ic_menu_report_image);
        }

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

        switch (id){
            case R.id.home :
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                break;
            case R.id.export_image:
                help.saveImageToExternalStorage(mImage, "preview");
                break;
            case R.id.information:
                help.informationDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startPreprocessing(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Intent i = new Intent(this, Segmentation.class);
        i.putExtra("BitmapImage", byteArray);
        //Bundle nextAnimation = ActivityOptions.makeCustomAnimation(getApplicationContext(), R.anim.left_in, R.anim.right_out).toBundle();
        //startActivity(i, nextAnimation);
        startActivity(i);
    }
}
