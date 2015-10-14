package com.example.michal.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Segmentation extends AppCompatActivity {

    private static Bitmap imageAftefSegmentation;

    private static ImageView mSegmentationImage;
    private static Button mStartSegmentation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_segmentation);

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        final Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        mSegmentationImage = (ImageView) findViewById(R.id.view_segmentation_image);
        mSegmentationImage.setImageBitmap(image);

        mStartSegmentation = (Button) findViewById(R.id.button_start_segmentation);
        mStartSegmentation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSegmentation(bitmap2mat(image));
                mSegmentationImage.setImageBitmap(imageAftefSegmentation);
            }
        });
    }

    private void startSegmentation(Mat image){
        grayscaleTreshold(image);

        Utils.matToBitmap(image, imageAftefSegmentation);
    }

    private double grayscaleTreshold(Mat image){

    }

    public Mat bitmap2mat(Bitmap src){
        Mat dest = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, dest);
        return dest;
    }

}
