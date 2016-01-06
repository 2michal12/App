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
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;

public class Normalisation extends AppCompatActivity {

    private static Help help;
    private static Toolbar toolbar;
    private static ImageView mNormalisationImage;
    private static EditText mNormalisationContrast;
    private static Button mNextProcess;
    private static Bitmap imageAftefNormalisation;

    private static double treshold = 0.0;
    private static double variance = 0.0;
    private static int NORMALISATION_CONTRAST = 10;
    private static int[][] mask;

    private static RelativeLayout mProgresBarLayout;
    private static ProgressBar pb;
    private static Bitmap imageBitmap ;
    private static String type;
    private static Button dialogButton;
    private static Button mSettings;
    private static TextView mProgressBarText;
    private static TextView mEdittextTitle;
    private static TextView mSettingTitleText;

    private static String AUTOMATIC = "automatic";
    private static String MANUAL = "manual";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_normalisation);
        help = new Help(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pb = (ProgressBar) findViewById(R.id.progressBar);
        mProgresBarLayout = (RelativeLayout) findViewById(R.id.progress_bar_layout);
        mProgressBarText = (TextView) findViewById(R.id.progress_bar_text);
        mNextProcess = (Button) findViewById(R.id.next);
        mNextProcess.setEnabled(false);
        mSettings = (Button) findViewById(R.id.settings);
        mNormalisationImage = (ImageView) findViewById(R.id.view_normalisation_image);

        type = getIntent().getStringExtra("Type");

        variance = getIntent().getDoubleExtra("Variance",variance);
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

            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefNormalisation = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mNormalisationImage.setImageBitmap(imageBitmap);

            if( type.equals(AUTOMATIC) ) {
                //NORMALISATION_CONTRAST = 1; dorobit vypocet automatickeho zvysenia

                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            }else{
                mSettings.setVisibility(View.VISIBLE);
                mSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settingsDialog();
                    }
                });
            }
        }else{
            mNormalisationImage.setImageResource(R.drawable.ic_menu_report_image);
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
                help.saveImageToExternalStorage(imageAftefNormalisation, "normalisation");
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

        Intent i = new Intent(this, Filtering.class);
        i.putExtra("BitmapImage", byteArray);
        i.putExtra("Treshold",treshold);
        i.putExtra("Type", type);
        i.putExtras(mBundle);
        startActivity(i);
    }

    class AsyncTaskSegmentation extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onProgressUpdate(0);
            mProgressBarText.setText(R.string.normalisation_running);
        }

        @Override
        protected String doInBackground(String... params) {

            Mat image = help.bitmap2mat(imageBitmap);

            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
            double variance_local = variance + (variance * NORMALISATION_CONTRAST);

            double[] data = new double[1];
            double[] data_zero = new double[1];
            data_zero[0] = 0;
            double[] data_full = new double[1];
            data_full[0] = 255;
            double[] data_new = new double[1];
            int mod = image.width() / 100;
            int progress = 0;

            for(int i = 0; i < image.width(); i++) {
                for (int j = 0; j < image.height(); j++) {
                    data = image.get(j, i);
                    if (data[0] > treshold) {
                        if ((treshold + (Math.sqrt((variance_local * (Math.pow((data[0] - treshold), 2))) / variance))) > 255) {
                            image.put(j, i, data_full);
                        } else {
                            data_new[0] = treshold + (Math.sqrt((variance_local * (Math.pow((data[0] - treshold), 2))) / variance));
                            image.put(j, i, data_new);
                        }
                    } else {
                        if ((treshold - (Math.sqrt((variance_local * (Math.pow((data[0] - treshold), 2))) / variance))) < 0) {
                            image.put(j, i, data_zero);
                        } else {
                            data_new[0] = treshold - (Math.sqrt((variance_local * (Math.pow((data[0] - treshold), 2))) / variance));
                            image.put(j, i, data_new);
                        }
                    }
                }
                if( i % mod == 0 ) {
                    progress++;
                }
                publishProgress( progress );
            }

            Utils.matToBitmap(image, imageAftefNormalisation);

            return "normalisation_finished";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            pb.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mProgressBarText.setText(R.string.normalisation_finished);
            mNormalisationImage.setImageBitmap(imageAftefNormalisation);

            mProgresBarLayout.setVisibility(View.GONE);

            mNextProcess.setEnabled(true);
            mNextProcess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPreprocessing(imageAftefNormalisation);
                }
            });
        }

    }


    public void settingsDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_settings);
        dialog.setTitle(R.string.settings);

        dialogButton = (Button) dialog.findViewById(R.id.popUpOK);
        mSettingTitleText = (TextView) dialog.findViewById(R.id.popUpSettingTextTitle);
        mSettingTitleText.setText(R.string.normalisation_settings_title);
        mEdittextTitle = (TextView) dialog.findViewById(R.id.text_for_edittext);
        mEdittextTitle.setText(R.string.normalisation_contrast);
        mNormalisationContrast = (EditText) dialog.findViewById(R.id.settings_edittext);
        mNormalisationContrast.setText(String.valueOf(NORMALISATION_CONTRAST));

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( !mNormalisationContrast.getText().toString().isEmpty() )
                    NORMALISATION_CONTRAST = Integer.valueOf(mNormalisationContrast.getText().toString());

                if( NORMALISATION_CONTRAST > 0 && NORMALISATION_CONTRAST < 100 ){
                    dialog.dismiss();

                    mProgresBarLayout.setVisibility(View.VISIBLE);
                    new AsyncTaskSegmentation().execute();
                }

            }
        });

        dialog.show();
    }

}
