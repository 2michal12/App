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
import android.widget.EditText;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;

public class Normalisation extends AppCompatActivity {
    private static Toolbar toolbar;
    private static ImageView mNormalisationImage;
    private static EditText mNormalisationContrast;
    private static Button mStartNormalisation;
    private static Button mNextProcess;
    private static Bitmap imageAftefNormalisation;

    private static double treshold = 0.0;
    private static double variance = 0.0;
    private static int NORMALISATION_CONTRAST;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_normalisation);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.normalisation);
        setSupportActionBar(toolbar);

        variance = getIntent().getDoubleExtra("Variance",variance);
        treshold = getIntent().getDoubleExtra("Treshold",treshold);

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        final Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        imageAftefNormalisation = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

        mNormalisationContrast = (EditText) findViewById(R.id.normalisation_contrast_edittext);
        mNormalisationContrast.setText("1");

        mNormalisationImage = (ImageView) findViewById(R.id.view_normalisation_image);
        mNormalisationImage.setImageBitmap(image);

        mStartNormalisation = (Button) findViewById(R.id.start_normalisation);
        mStartNormalisation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NORMALISATION_CONTRAST = Integer.parseInt(mNormalisationContrast.getText().toString());
                if (NORMALISATION_CONTRAST <= 0 || NORMALISATION_CONTRAST > 10) {
                    NORMALISATION_CONTRAST = 1;
                }
                startNormalisation(bitmap2mat(image));
                mNormalisationImage.setImageBitmap(imageAftefNormalisation);

                mNextProcess = (Button) findViewById(R.id.next);
                mNextProcess.setEnabled(true);
                mNextProcess.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startPreprocessing(imageAftefNormalisation);
                    }
                });
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
            case R.id.information:
                System.out.println("informacie");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startPreprocessing(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Intent i = new Intent(this, Filtering.class);
        i.putExtra("BitmapImage", byteArray);
        startActivity(i);
    }

    private void startNormalisation(Mat image){
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        double variance_local = variance + (variance * NORMALISATION_CONTRAST);

        double[] data = new double[1];
        double[] data_zero = new double[1];
        data_zero[0] = 0;
        double[] data_full = new double[1];
        data_full[0] = 255;
        double[] data_new = new double[1];

        for(int i = 0; i < image.width(); i++)
            for(int j = 0; j < image.height(); j++)
            {
                data = image.get(j, i);
                if( data[0] > treshold )
                {
                    if((treshold + (Math.sqrt((variance_local * (Math.pow((data[0] - treshold), 2))) / variance)))>255 ) {
                        image.put(j, i, data_full);
                    }
                    else{
                        data_new[0] = treshold + (Math.sqrt((variance_local * (Math.pow((data[0] - treshold), 2))) / variance));
                        image.put(j, i, data_new);
                    }
                }
                else
                {
                    if((treshold - (Math.sqrt((variance_local * (Math.pow((data[0] - treshold), 2))) / variance)))<0 ) {
                        image.put(j, i, data_zero);
                    }
                    else {
                        data_new[0] = treshold - (Math.sqrt((variance_local * (Math.pow((data[0] - treshold), 2))) / variance));
                        image.put(j, i, data_new);
                    }
                }
            }

        Utils.matToBitmap(image, imageAftefNormalisation);
    }

    public Mat bitmap2mat(Bitmap src){
        Mat dest = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, dest);
        return dest;
    }

    }
