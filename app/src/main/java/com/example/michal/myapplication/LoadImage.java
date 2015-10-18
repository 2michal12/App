package com.example.michal.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.ByteArrayOutputStream;

public class LoadImage extends AppCompatActivity {

    private static Toolbar toolbar;
    private static final String IMAGE_PATH = "/sdcard/Fingerprints/obrazok.bmp";

    private static Button mLoadImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_image);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.load_image);
        setSupportActionBar(toolbar);

        mLoadImage = (Button) findViewById(R.id.load_image);
        mLoadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap image = BitmapFactory.decodeFile(IMAGE_PATH);
                initSegmentation(image);
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

        switch (id){
            case R.id.home :
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                break;
            case R.id.load_image :
                break;
            case R.id.preprocessing :
                System.out.println("predspracovanie");
                break;
            case R.id.extraction:
                System.out.println("extrakcia");
                break;
            case R.id.information:
                System.out.println("informacie");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initSegmentation(Bitmap image){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Intent i = new Intent(this, Segmentation.class);
        i.putExtra("BitmapImage",byteArray);
        startActivity(i);
    }
}
