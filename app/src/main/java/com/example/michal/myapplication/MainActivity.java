package com.example.michal.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String IMAGE_PATH = "/sdcard/Fingerprints/obrazok.jpg";
    private static Button mSelectImage;

    static{
        if(!OpenCVLoader.initDebug()){
            Log.i("opencv","opencv failed");
        }else{
            Log.i("opencv","opencv initialized");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSelectImage = (Button) findViewById(R.id.button_select_image);
        mSelectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap image = BitmapFactory.decodeFile(IMAGE_PATH);
                initSegmentation(image);
            }
        });
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
