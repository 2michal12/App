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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;

public class Binarisation extends AppCompatActivity {

    private static Help help;
    private static Toolbar toolbar;
    private static ImageView mBinarisationImage;
    private static Button mStartBinarisation;
    private static Button mNextProcess;
    private static Bitmap imageAftefBinarisation;

    private static double treshold = 0.0;
    private static int[][] mask;

    private static int GAUSS_SIZE = 3;
    private static int GAUSS_STRENGTH = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binarisation);
        help = new Help(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.binarisation);
        setSupportActionBar(toolbar);

        mBinarisationImage = (ImageView) findViewById(R.id.view_binarisation_image);

        treshold = getIntent().getDoubleExtra("Treshold",treshold);

        mask = null;
        Object[] objectArray = (Object[]) getIntent().getExtras().getSerializable("Mask");
        if(objectArray != null){
            mask = new int[objectArray.length][];
            for(int i = 0; i < objectArray.length; i++){
                mask[i] = (int[]) objectArray[i];
            }
        }

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        if(byteArray != null) {

            final Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefBinarisation = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

            mBinarisationImage.setImageBitmap(image);

            mStartBinarisation = (Button) findViewById(R.id.start_binarisation);
            mStartBinarisation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startBinarisation(help.bitmap2mat(image));
                    mBinarisationImage.setImageBitmap(imageAftefBinarisation);

                    mNextProcess = (Button) findViewById(R.id.next);
                    mNextProcess.setEnabled(true);
                    mNextProcess.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startPreprocessing(imageAftefBinarisation);
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
            case R.id.export_image:
                help.saveImageToExternalStorage(imageAftefBinarisation, "binarisation");
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

        Bundle mBundle = new Bundle();
        mBundle.putSerializable("Mask", mask);

        Intent i = new Intent(this, Thinning.class);
        i.putExtra("BitmapImage", byteArray);
        i.putExtras(mBundle);
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

        gaussianFilter(image);

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

    private void gaussianFilter(Mat image){
        Size kernel = new Size(GAUSS_SIZE, GAUSS_SIZE);
        Imgproc.GaussianBlur(image, image, kernel, GAUSS_STRENGTH, GAUSS_STRENGTH);
    }
}
