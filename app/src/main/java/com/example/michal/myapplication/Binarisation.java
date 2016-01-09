package com.example.michal.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
    private static Button mNextProcess;
    private static Bitmap imageAftefBinarisation;
    private static EditText mBinarisationBlock;

    private static double treshold = 0.0;
    private static int[][] mask;

    private static int BINARISATION_BLOCK = 10;
    private static int GAUSS_SIZE = 3;
    private static int GAUSS_STRENGTH = 5;

    private static RelativeLayout mProgresBarLayout;
    private static ProgressBar pb;
    private static Bitmap imageBitmap ;
    private static String type;
    private static Button dialogButton;
    private static Button mSettings;
    private static TextView mProgressBarText;
    private static TextView mEdittextTitle;
    private static TextView mSettingTitleText;
    private static int BLOCK_SIZE = 0; //velkost pouzita ako v segmentacii

    private static String AUTOMATIC = "automatic";
    private static String AUTOMATIC_FULL = "automatic_full";
    private static String MANUAL = "manual";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binarisation);
        help = new Help(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pb = (ProgressBar) findViewById(R.id.progressBar);
        mProgresBarLayout = (RelativeLayout) findViewById(R.id.progress_bar_layout);
        mProgressBarText = (TextView) findViewById(R.id.progress_bar_text);
        mNextProcess = (Button) findViewById(R.id.next);
        mNextProcess.setEnabled(false);
        mSettings = (Button) findViewById(R.id.settings);
        mBinarisationImage = (ImageView) findViewById(R.id.view_binarisation_image);

        type = getIntent().getStringExtra("Type");
        treshold = getIntent().getDoubleExtra("Treshold",treshold);
        BLOCK_SIZE = getIntent().getIntExtra("SegmentationBlock", BLOCK_SIZE);

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

            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefBinarisation = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mBinarisationImage.setImageBitmap(imageBitmap);

            if( type.equals(AUTOMATIC) ) {
                //BINARISATION_BLOCK = 10; dorobit vypocet automaticky

                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            }else if( type.equals(AUTOMATIC_FULL) ){
                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            }else{
                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
// zakomentovane zatial nepotrebne ziadne manualne nastavenia pre binarizaciu
//                mSettings.setVisibility(View.VISIBLE);
//                mSettings.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        settingsDialog();
//                    }
//                });
            }
        }else{
            mBinarisationImage.setImageResource(R.drawable.ic_menu_report_image);
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
                help.informationDialog();
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
        i.putExtra("SegmentationBlock", BLOCK_SIZE);
        i.putExtra("Type", type);
        i.putExtras(mBundle);
        startActivity(i);
    }

    private void gaussianFilter(Mat image){
        Size kernel = new Size(GAUSS_SIZE, GAUSS_SIZE);
        Imgproc.GaussianBlur(image, image, kernel, GAUSS_STRENGTH, GAUSS_STRENGTH);
    }

    class AsyncTaskSegmentation extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onProgressUpdate(0);
            mProgressBarText.setText(R.string.binarisation_running);
        }

        @Override
        protected String doInBackground(String... params) {

            Mat image = help.bitmap2mat(imageBitmap);

            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

            double[] data;
            int mod = image.width() / 100;
            int progress = 0;

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
                if( i % mod == 0 ) {
                    progress++;
                }
                publishProgress( progress );
            }
            /*
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
            }*/

            Utils.matToBitmap(image, imageAftefBinarisation);

            return "binarisation_finished";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            pb.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mProgressBarText.setText(R.string.binarisation_finished);
            mBinarisationImage.setImageBitmap(imageAftefBinarisation);

            mProgresBarLayout.setVisibility(View.GONE);

            mNextProcess.setEnabled(true);
            mNextProcess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPreprocessing(imageAftefBinarisation);
                }
            });

            if( type.equals(AUTOMATIC_FULL) )
                startPreprocessing(imageAftefBinarisation);
        }

    }


    public void settingsDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_settings);
        dialog.setTitle(R.string.settings);

        dialogButton = (Button) dialog.findViewById(R.id.popUpOK);
        mSettingTitleText = (TextView) dialog.findViewById(R.id.popUpSettingTextTitle);
        mSettingTitleText.setText(R.string.binarisation_settings_title);
        mEdittextTitle = (TextView) dialog.findViewById(R.id.text_for_edittext);
        mEdittextTitle.setText(R.string.binarisation_block);
        mBinarisationBlock = (EditText) dialog.findViewById(R.id.settings_edittext);
        mBinarisationBlock.setText(String.valueOf(BINARISATION_BLOCK));

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( !mBinarisationBlock.getText().toString().isEmpty() )
                    BINARISATION_BLOCK = Integer.valueOf(mBinarisationBlock.getText().toString());

                if( BINARISATION_BLOCK > 0 && BINARISATION_BLOCK < 100 ){
                    dialog.dismiss();

                    mProgresBarLayout.setVisibility(View.VISIBLE);
                    new AsyncTaskSegmentation().execute();
                }

            }
        });

        dialog.show();
    }

}
