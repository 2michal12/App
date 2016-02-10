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

public class Segmentation extends AppCompatActivity{

    private static Help help;
    private static Toolbar toolbar;
    private static Bitmap imageAftefSegmentation;
    private static double treshold = 0.0;
    private static double variance = 0.0;
    private static int SEGMENTATION_SIZE = 7;
    private static final Integer SEGMENTATION_CLEANING = 10;
    private static boolean clearOnceEdges = false;

    private static ImageView mSegmentationImage;
    private static EditText mSegmentationBlockSize;
    private static Button mNextProcess;
    private static Button mSettings;
    private static TextView mProgressBarText;

    private static int mask[][];
    private static RelativeLayout mProgresBarLayout;
    private static ProgressBar pb;
    private static Bitmap imageBitmap ;
    private static String type;
    private static Button dialogButton;
    private static TextView mSettingTitleText;
    private static TextView mEdittextTitle;

    private static String AUTOMATIC = "automatic";
    private static String AUTOMATIC_FULL = "automatic_full";
    private static String MANUAL = "manual";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_segmentation);
        help = new Help(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if( getSupportActionBar() != null )
            getSupportActionBar().setTitle(R.string.segmentation);

        pb = (ProgressBar) findViewById(R.id.progressBar);
        mProgresBarLayout = (RelativeLayout) findViewById(R.id.progress_bar_layout);
        mProgressBarText = (TextView) findViewById(R.id.progress_bar_text);
        mNextProcess = (Button) findViewById(R.id.next);
        mNextProcess.setEnabled(false);
        mSettings = (Button) findViewById(R.id.settings);
        mSegmentationImage = (ImageView) findViewById(R.id.view_segmentation_image);

        type = getIntent().getStringExtra("Type");
        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        if(byteArray != null) {

            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefSegmentation = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mSegmentationImage.setImageBitmap(imageBitmap);

            if( type.equals(AUTOMATIC) ) {
                //SEGMENTATION_SIZE = 7; dorobit vypocet automatickej velkosti bloku

                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            }else if( type.equals(AUTOMATIC_FULL) ){
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
            mSegmentationImage.setImageResource(R.drawable.ic_menu_report_image);
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
        help.menuItemOtherActivities(id, imageAftefSegmentation, help.SEGMENTATION);
        return super.onOptionsItemSelected(item);
    }

    private void startPreprocessing(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Bundle mBundle = new Bundle();
        mBundle.putSerializable("Mask", mask);

        Intent i = new Intent(this, Normalisation.class);
        i.putExtra("BitmapImage", byteArray);
        i.putExtra("SegmentationBlock", SEGMENTATION_SIZE);
        i.putExtra("Treshold",treshold);
        i.putExtra("Variance",variance);
        i.putExtra("Type",type);
        i.putExtras(mBundle);

        startActivity(i);
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
                variance += ( data[0]-treshold)*( data[0]-treshold);
            }
        }
        return (double)Math.round( variance/(image.width()*image.height()));
    }

    private int[][] cleanMask(int[][] mask, int blocksWidth, int blocksHeight){
        int temp[][] = new int[blocksWidth][blocksHeight];


        for(int i = 1; i < blocksHeight-1; i++) //clean mask a copy to temporary
        {
            for(int j = 1; j < blocksWidth-1; j++)
            {
                if((mask[j][i] == 0) && ((mask[j-1][i]+mask[j+1][i]+mask[j][i-1]+mask[j][i+1]+mask[j-1][i-1]+mask[j+1][i-1]+mask[j-1][i+1]+mask[j+1][i+1]) > 3))
                {
                    temp[j][i] = 1;
                }
                else
                {
                    temp[j][i] = 0;
                }
            }
        }

        for(int count = 0; count < 2; count++) //erase single point
            for (int j = 1; j < blocksWidth - 1; j++)
                for (int i = 1; i < blocksHeight - 1; i++)
                    if (mask[j][i] == 1 && mask[j - 1][i] == 0 && mask[j + 1][i] == 0 && mask[j][i - 1] == 0 && mask[j][i + 1] == 0 && mask[j - 1][i - 1] == 0 && mask[j + 1][i - 1] == 0 && mask[j - 1][i + 1] == 0 && mask[j + 1][i + 1] == 0)
                        mask[j][i] = 0;

        if( !clearOnceEdges ) {
            for (int i = 0; i < blocksWidth; i++)  //erase edges of fingerprint
                for (int j = 0; j < blocksHeight; j++) {
                    if (i == 0)
                        mask[i][j] = 0;
                    if (i == blocksWidth - 1)
                        mask[i][j] = 0;
                    if (j == 0)
                        mask[i][j] = 0;
                    if (j == blocksHeight - 1)
                        mask[i][j] = 0;
                }
            clearOnceEdges = true;
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

        for(int i = image.height(); i >= image.height() - 40; i--) //doplnene kvoli zlemu snimacu (robil cierny okraj zo spodu)
        {
            for(int j = 0; j < image.width(); j++)
            {
                image.put(i, j, data);
            }
        }
    }

    class AsyncTaskSegmentation extends AsyncTask<String, Integer, String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onProgressUpdate(0);
            mProgressBarText.setText(R.string.segmentation_running);
        }

        @Override
        protected String doInBackground(String... params) {

            Mat image = help.bitmap2mat(imageBitmap);

            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
            treshold = grayscaleTreshold(image, 0, 0, image.width(), image.height());
            variance = grayVariance(image, treshold);

            int blocksWidth = (int)Math.floor(image.width()/SEGMENTATION_SIZE);
            int blocksHeight = (int)Math.floor(image.height()/SEGMENTATION_SIZE);
            mask = new int[blocksWidth][blocksHeight];

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
            publishProgress(33);

            //clean mask
            for(int i = 0; i < SEGMENTATION_CLEANING; i++)
                cleanMask(mask, blocksWidth, blocksHeight);

            publishProgress(66);

            //apply mask520184
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

            publishProgress(100);
            return "segmentation_finished";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            pb.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mProgressBarText.setText(R.string.segmentation_finished);
            mSegmentationImage.setImageBitmap(imageAftefSegmentation);

            mProgresBarLayout.setVisibility(View.GONE);

            mNextProcess.setEnabled(true);
            mNextProcess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPreprocessing(imageAftefSegmentation);
                }
            });

            if( type.equals(AUTOMATIC_FULL) )
                startPreprocessing(imageAftefSegmentation);

        }

    }


    public void settingsDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_settings);
        dialog.setTitle(R.string.settings);

        dialogButton = (Button) dialog.findViewById(R.id.popUpOK);
        mSettingTitleText = (TextView) dialog.findViewById(R.id.popUpSettingTextTitle);
        mSettingTitleText.setText(R.string.segmentation_settings_title);
        mEdittextTitle = (TextView) dialog.findViewById(R.id.textForEdittext);
        mEdittextTitle.setText(R.string.segmentation_block);
        mSegmentationBlockSize = (EditText) dialog.findViewById(R.id.settingsEdittext);
        mSegmentationBlockSize.setText(String.valueOf(SEGMENTATION_SIZE));

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( !mSegmentationBlockSize.getText().toString().isEmpty() )
                    SEGMENTATION_SIZE = Integer.valueOf(mSegmentationBlockSize.getText().toString());

                if( SEGMENTATION_SIZE > 0 && SEGMENTATION_SIZE < 100 ){
                    dialog.dismiss();

                    mProgresBarLayout.setVisibility(View.VISIBLE);
                    new AsyncTaskSegmentation().execute();
                }

            }
        });

        dialog.show();
    }

}
