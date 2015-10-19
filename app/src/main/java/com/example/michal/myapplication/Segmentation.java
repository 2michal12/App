package com.example.michal.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class Segmentation extends AppCompatActivity{

    private static Toolbar toolbar;
    private static Bitmap imageAftefSegmentation;
    private static double treshold = 0.0;
    private static double variance = 0.0;
    private static final Integer SEGMENTATION_SIZE = 10;
    private static final Integer SEGMENTATION_CLEANING = 10;

    private static ImageView mSegmentationImage;
    private static Button mStartSegmentation;
    private static TextView priemer;  //docasne
    private static TextView rozptyl;  //docasne

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_segmentation);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.segmentation);
        setSupportActionBar(toolbar);

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        final Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        imageAftefSegmentation = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

        mSegmentationImage = (ImageView) findViewById(R.id.view_segmentation_image);
        mSegmentationImage.setImageBitmap(image);

//        priemer = (TextView) findViewById(R.id.priemerna);
//        rozptyl = (TextView) findViewById(R.id.rozptyl);

        mStartSegmentation = (Button) findViewById(R.id.start_segmentation);
        mStartSegmentation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSegmentation(bitmap2mat(image));
                mSegmentationImage.setImageBitmap(imageAftefSegmentation);
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
        Intent i;

        switch (id){
            case R.id.home :
                i = new Intent(this, MainActivity.class);
                startActivity(i);
                break;
            case R.id.load_image :
                i = new Intent(this, Preview.class);
                startActivity(i);
                break;
            case R.id.information:
                System.out.println("informacie");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startSegmentation(Mat image){
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        treshold = grayscaleTreshold(image, 0, 0, image.width(), image.height());
        variance = grayVariance(image, treshold);

        int blocksWidth = (int)Math.floor(image.width()/SEGMENTATION_SIZE);
        int blocksHeight = (int)Math.floor(image.height()/SEGMENTATION_SIZE);
        int mask[][] = new int[blocksWidth][blocksHeight];

        int padding_x = image.width() - (blocksWidth*SEGMENTATION_SIZE); //padding of image
        int padding_y = image.height() - (blocksHeight*SEGMENTATION_SIZE);

        //calculate mask
        for(int i = 0; i < blocksWidth; i++)
        {
            for(int j = 0; j < blocksHeight; j++)
            {
                if(treshold < grayscaleTreshold(image, i * SEGMENTATION_SIZE, j * SEGMENTATION_SIZE, i * SEGMENTATION_SIZE + SEGMENTATION_SIZE, j * SEGMENTATION_SIZE+SEGMENTATION_SIZE)) {
                    mask[i][j] = 0;
                }
                else {
                    mask[i][j] = 1;
                }
            }
        }

        //clean mask
        for(int i = 0; i < SEGMENTATION_CLEANING; i++)
            cleanMask(mask, blocksWidth, blocksHeight);

        //apply mask
        double[] data = new double[1];
        data[0] = 0;
        for(int i = 0; i < blocksHeight; i++)
        {
            for(int j = 0; j < blocksWidth; j++)
            {
                if(mask[j][i] == 0)
                    for(int k = i*SEGMENTATION_SIZE; k < i*SEGMENTATION_SIZE+SEGMENTATION_SIZE; k++)
                    {
                        for(int l = j*SEGMENTATION_SIZE; l < j*SEGMENTATION_SIZE+SEGMENTATION_SIZE; l++)
                        {
                            image.put(k, l, data);

                        }
                    }
            }
        }

        clearPadding(image, padding_x, padding_y);

        Utils.matToBitmap(image, imageAftefSegmentation);
    }

    private double grayscaleTreshold(Mat image, int startX, int startY, int endX, int endY){
        double[] data;
        double actualTreshold = 0;
        for(int i = startY; i < endY; i++) {
            for (int j = startX; j < endX; j++) {
                data = image.get(i, j);
                actualTreshold += data[0];
            }
        }
        return Math.round( actualTreshold/((endX-startX) * (endY-startY)) );
    }

    private double grayVariance(Mat image, double treshold){
        double[] data;
        for(int i = 0; i < image.height(); i++) {
            for (int j = 0; j < image.width(); j++) {
                data = image.get(i, j);
                variance += ( data[0]/255.0-treshold/255.0)*( data[0]/255.0-treshold/255.0);
            }
        }
        return (double)Math.round( variance/(image.width()*image.height()) *100)/10;
    }

    private int[][] cleanMask(int[][] mask, int blocksWidth, int blocksHeight){
        int temp[][] = new int[blocksWidth][blocksHeight];


        for(int i = 1; i < blocksHeight-1; i++) //clean mask a copy to temporary
        {
            for(int j = 1; j < blocksWidth-1; j++)
            {
                if((mask[j][i] == 0) && ((mask[j-1][i]+mask[j+1][i]+mask[j][i-1]+mask[j][i+1]+mask[j-1][i-1]+mask[j+1][i-1]+mask[j-1][i+1]+mask[j+1][i+1]) > 4))
                {
                    temp[j][i] = 1;
                }
                else
                {
                    temp[j][i] = 0;
                }
            }
        }

        for(int i = 1; i < blocksHeight-1; i++) //copy temporary to original mask
        {
            for(int j = 1; j < blocksWidth-1; j++)
            {
                if(temp[j][i] == 1)
                    mask[j][i] = 1;
            }
        }
        return mask;
    }

    private void clearPadding(Mat image, int padding_x, int padding_y){
        double[] data = new double[1];
        data[0] = 0;
        for(int i = image.width(); i >= image.width() - padding_x; i--) //clear padding column on the right
        {
            for(int j = 0; j < image.height(); j++)
            {
                image.put(j, i, data);
            }
        }
        for(int i = image.height(); i >= image.height() - padding_y; i--) //clear padding row at the bottom
        {
            for(int j = 0; j < image.width(); j++)
            {
                image.put(i, j, data);
            }
        }

        for(int i = image.height(); i >= image.height() - 20; i--) //doplnene kvoli zlemu snimacu
        {
            for(int j = 0; j < image.width(); j++)
            {
                image.put(i, j, data);
            }
        }
    }

    public Mat bitmap2mat(Bitmap src){
        Mat dest = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, dest);
        return dest;
    }

}
