package com.example.michal.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class Segmentation extends AppCompatActivity {

    private static Bitmap imageAftefSegmentation;
    private static double treshold = 0;
    private static double variance = 0;

    private static ImageView mSegmentationImage;
    private static Button mStartSegmentation;
    private static TextView priemer;
    private static TextView rozptyl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_segmentation);

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        final Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        mSegmentationImage = (ImageView) findViewById(R.id.view_segmentation_image);
        mSegmentationImage.setImageBitmap(image);

        priemer = (TextView) findViewById(R.id.priemerna);
        rozptyl = (TextView) findViewById(R.id.rozptyl);

        mStartSegmentation = (Button) findViewById(R.id.button_start_segmentation);
        mStartSegmentation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSegmentation(bitmap2mat(image));
                //mSegmentationImage.setImageBitmap(imageAftefSegmentation);
            }
        });

    }

    private void startSegmentation(Mat image){
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        treshold = grayscaleTreshold(image);
        variance = grayVariance(image, treshold);

        priemer.setText("Priemerna farba: "+treshold);
        rozptyl.setText("Rozptyl: "+variance);

        //Utils.matToBitmap(image, imageAftefSegmentation);
    }

    private double grayscaleTreshold(Mat image){
        double[] data;
        for(int i = 0; i < image.height(); i++) {
            for (int j = 0; j < image.width(); j++) {
                data = image.get(i, j);
                treshold += data[0];
            }
        }
        return Math.round( treshold/(image.height()*image.width()) );
    }

    private double grayVariance(Mat image, double treshold){
        double[] data;
        for(int i = 0; i < image.height(); i++) {
            for (int j = 0; j < image.width(); j++) {
                data = image.get(i, j);
                variance += ((data[0]/255.0) - (treshold/255.0)) * ((data[0]/255.0) - (treshold/255.0));
            }
        }
        return (double)Math.round( variance/(image.width()*image.height()) *100)/100;
    }

    public Mat bitmap2mat(Bitmap src){
        Mat dest = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, dest);
        return dest;
    }

}
