package com.example.michal.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;


public class MainActivity extends AppCompatActivity {

    private static final String IMAGE_PATH = "/sdcard/Fingerprints/obrazok.bmp";

    private static Toolbar toolbar;
    private static Button mLoadImage;
    private static Bitmap image;

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
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        mLoadImage = (Button) findViewById(R.id.load_image);
        mLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                image = BitmapFactory.decodeFile(IMAGE_PATH);
                startPreview(image);
            }
        });

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
            case R.id.preview:
                Intent i = new Intent(this, Preview.class);
                startActivity(i);
            case R.id.information:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startPreview(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Intent i = new Intent(this, Preview.class);
        i.putExtra("BitmapImage", byteArray);
        startActivity(i);
    }
}
