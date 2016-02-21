package com.example.michal.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;

import butterknife.Bind;
import butterknife.ButterKnife;

public class Normalisation extends AppCompatActivity {

    private static Help help;
    private static Bitmap imageBitmap;
    private static Bitmap imageAftefNormalisation;

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.next) Button mNextProcess;
    @Bind(R.id.settings) Button mSettings;
    @Bind(R.id.progress_bar_text) TextView mProgressBarText;
    @Bind(R.id.progress_bar_layout) RelativeLayout mProgresBarLayout;
    @Bind(R.id.progressBar) ProgressBar pb;
    @Bind(R.id.view_normalisation_image) ImageView mNormalisationImage;

    private static int BLOCK_SIZE;
    private static int NORMALISATION_CONTRAST = 10;
    private static int[][] mask;

    private static String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_normalisation);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if( getSupportActionBar() != null )
            getSupportActionBar().setTitle(R.string.normalisation);

        help = new Help(this);
        mNextProcess.setEnabled(false);

        BLOCK_SIZE = help.BLOCK_SIZE;
        type = getIntent().getStringExtra(help.TYPE);
        help.TRESHOLD = getIntent().getDoubleExtra(help.TRESHOLD_NAME, help.TRESHOLD);
        help.VARIANCE = getIntent().getDoubleExtra(help.VARIANCE_NAME, help.VARIANCE);
        mask = null;
        Object[] objectArray = (Object[]) getIntent().getExtras().getSerializable(help.MASK);
        if(objectArray != null){
            mask = new int[objectArray.length][];
            for(int i = 0; i < objectArray.length; i++){
                mask[i] = (int[]) objectArray[i];
            }
        }

        byte[] byteArray = getIntent().getByteArrayExtra(help.BITMAP_IMAGE);
        if(byteArray != null) {

            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefNormalisation = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mNormalisationImage.setImageBitmap(imageBitmap);

            if( type.equals(help.AUTOMATIC) ) {
                //NORMALISATION_CONTRAST = 1; dorobit vypocet automatickeho zvysenia
                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            }else if( type.equals(help.AUTOMATIC_FULL) ){
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
        menu.getItem(1).setVisible(false);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) { //due to finishAffinity(); supported from API 16
            menu.getItem(4).setVisible(false);  //exit app
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        help.menuItemOtherActivities(id, imageAftefNormalisation, help.NORMALISATION);
        return super.onOptionsItemSelected(item);
    }

    private void startPreprocessing(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Bundle mBundle = new Bundle();
        mBundle.putSerializable(help.MASK, mask);

        Intent i = new Intent(this, Filtering.class);
        i.putExtra(help.BITMAP_IMAGE, byteArray);
        i.putExtra(help.TYPE, type);
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
            double variance_local = help.VARIANCE + (help.VARIANCE * NORMALISATION_CONTRAST);

            double[] data = new double[1];
            double[] data_zero = new double[1];
            data_zero[0] = 0;
            double[] data_full = new double[1];
            data_full[0] = 255;
            double[] data_new = new double[1];

            int blocksWidth = (int)Math.floor(image.width()/BLOCK_SIZE);
            int blocksHeight = (int)Math.floor(image.height()/BLOCK_SIZE);
            int progress = 100 / blocksHeight;

            for(int i = 0; i < blocksHeight; i++) {
                for (int j = 0; j < blocksWidth; j++) {
                    if (mask[j][i] == 1) {
                        for (int k = i * BLOCK_SIZE; k < i * BLOCK_SIZE + BLOCK_SIZE; k++) {
                            for (int l = j * BLOCK_SIZE; l < j * BLOCK_SIZE + BLOCK_SIZE; l++) {
                                data = image.get(k, l);
                                if (data[0] > help.TRESHOLD) {
                                    if ((help.TRESHOLD + (Math.sqrt((variance_local * (Math.pow((data[0] - help.TRESHOLD), 2))) / help.VARIANCE))) > 255) {
                                        image.put(k, l, data_full);
                                    } else {
                                        data_new[0] = help.TRESHOLD + (Math.sqrt((variance_local * (Math.pow((data[0] - help.TRESHOLD), 2))) / help.VARIANCE));
                                        image.put(k, l, data_new);
                                    }
                                } else {
                                    if ((help.TRESHOLD - (Math.sqrt((variance_local * (Math.pow((data[0] - help.TRESHOLD), 2))) / help.VARIANCE))) < 0) {
                                        image.put(k, l, data_zero);
                                    } else {
                                        data_new[0] = help.TRESHOLD - (Math.sqrt((variance_local * (Math.pow((data[0] - help.TRESHOLD), 2))) / help.VARIANCE));
                                        image.put(k, l, data_new);
                                    }
                                }
                            }
                        }
                    }
                }
                publishProgress( progress * i );
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

            if( type.equals(help.AUTOMATIC_FULL) )
                startPreprocessing(imageAftefNormalisation);
        }

    }

    public void settingsDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_settings);
        dialog.setTitle(R.string.settings);

        Button dialogButton = (Button) dialog.findViewById(R.id.popUpOK);
        TextView mSettingTitleText = (TextView) dialog.findViewById(R.id.popUpSettingTextTitle);
        TextView mEdittextTitle = (TextView) dialog.findViewById(R.id.textForEdittext);
        final EditText mNormalisationContrast = (EditText) dialog.findViewById(R.id.settingsEdittext);

        mSettingTitleText.setText(R.string.normalisation_settings_title);
        mEdittextTitle.setText(R.string.normalisation_contrast);
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
