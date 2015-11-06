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

public class Binarisation extends AppCompatActivity {
    private static Toolbar toolbar;
    private static ImageView mBinarisationImage;
    private static EditText mBinarisationBlock;
    private static Button mStartBinarisation;
    private static Button mNextProcess;
    private static Bitmap imageAftefBinarisation;

    private static double treshold = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binarisation);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.binarisation);
        setSupportActionBar(toolbar);

        mBinarisationImage = (ImageView) findViewById(R.id.view_binarisation_image);

        treshold = getIntent().getDoubleExtra("Treshold",treshold);
        System.out.println(treshold);

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        if(byteArray != null) {

            final Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefBinarisation = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

            mBinarisationImage.setImageBitmap(image);

            mStartBinarisation = (Button) findViewById(R.id.start_binarisation);
            mStartBinarisation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startBinarisation(bitmap2mat(image));
                    mBinarisationImage.setImageBitmap(imageAftefBinarisation);

                    mNextProcess = (Button) findViewById(R.id.next);
                    mNextProcess.setEnabled(true);
                    mNextProcess.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //startPreprocessing(imageAftefBinarisation);
                        }
                    });
                }
            });
        }else{
            mBinarisationImage.setImageResource(R.mipmap.ic_menu_report_image);
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

        Intent i = new Intent(this, Binarisation.class);
        i.putExtra("BitmapImage", byteArray);
        startActivity(i);
    }

    private void startBinarisation(Mat image){
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

        double[] data;

        for(int i = 0; i < image.rows(); i++){
            for(int j = 0; j < image.cols(); j++) {
                data = image.get(i, j);
                if(data[0] < treshold){
                    data[0] = 0;
                    image.put(i, j, data);
                }else{
                    data[0] = 255;
                    image.put(i, j, data);
                }
            }
        }

        Utils.matToBitmap(image, imageAftefBinarisation);
    }

    public Mat bitmap2mat(Bitmap src){
        Mat dest = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, dest);
        return dest;
    }

}
