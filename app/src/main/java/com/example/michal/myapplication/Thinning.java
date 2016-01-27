package com.example.michal.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Thinning extends AppCompatActivity {

    private static Help help;
    private static Toolbar toolbar;
    private static ImageView mThinningImage;
    private static Button mNextProcess;
    private static Bitmap imageAftefThinning;
    private static EditText mThinningBlock;
    private static int BLOCK_SIZE = 0; //velkost pouzita ako v segmentacii

    private static int[][] mask;
    int blocksWidth, blocksHeight;
    double[] pC, p2, p3, p4, p5, p6, p7, p8, p9;

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
    private static String AUTOMATIC_FULL = "automatic_full";
    private static String MANUAL = "manual";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thinning);
        help = new Help(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pb = (ProgressBar) findViewById(R.id.progressBar);
        mProgresBarLayout = (RelativeLayout) findViewById(R.id.progress_bar_layout);
        mProgressBarText = (TextView) findViewById(R.id.progress_bar_text);
        mNextProcess = (Button) findViewById(R.id.next);
        mNextProcess.setEnabled(false);
        mSettings = (Button) findViewById(R.id.settings);
        mThinningImage = (ImageView) findViewById(R.id.view_thinning_image);

        type = getIntent().getStringExtra("Type");
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
        if (byteArray != null) {

            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefThinning = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mThinningImage.setImageBitmap(imageBitmap);

            if( type.equals(AUTOMATIC) ) {
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
        } else {
            mThinningImage.setImageResource(R.drawable.ic_menu_report_image);
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

        switch (id) {
            case R.id.home:
                i = new Intent(this, MainActivity.class);
                startActivity(i);
                break;
            case R.id.export_image:
                help.saveImageToExternalStorage(imageAftefThinning, "thinning");
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

        Intent i = new Intent(this, Extraction.class);
        i.putExtra("BitmapImage", byteArray);
        startActivity(i);
    }

    public void thinningIteration(Mat image, int iter){
        Mat marker = Mat.zeros(image.size(), CvType.CV_8UC1);

        double[] data_input = new double[1];
        data_input[0] = 1;
        int A = 0, B, m1, m2;

        for(int i = 0; i < blocksHeight-1; i++){
            for(int j = 0; j < blocksWidth-1; j++){
                if(mask[j][i] == 1)
                    for(int k = i*BLOCK_SIZE; k < i*BLOCK_SIZE+BLOCK_SIZE; k++){
                        for(int l = j*BLOCK_SIZE; l < j*BLOCK_SIZE+BLOCK_SIZE; l++){

                            p2 = image.get(k-1, l);
                            p3 = image.get(k-1, l+1);
                            p4 = image.get(k, l+1);
                            p5 = image.get(k+1, l+1);
                            p6 = image.get(k+1, l);
                            p7 = image.get(k+1, l-1);
                            p8 = image.get(k, l-1);
                            p9 = image.get(k-1, l-1);

                            if((int)p2[0] == 0 && (int)p3[0] == 1){
                                A++;
                            }
                            if(((int)p3[0] == 0 && (int)p4[0] == 1)){
                                A++;
                            }
                            if(((int)p4[0] == 0 && (int)p5[0] == 1)){
                                A++;
                            }
                            if(((int)p5[0] == 0 && (int)p6[0] == 1)){
                                A++;
                            }
                            if(((int)p6[0] == 0 && (int)p7[0] == 1)){
                                A++;
                            }
                            if(((int)p7[0] == 0 && (int)p8[0] == 1)){
                                A++;
                            }
                            if(((int)p8[0] == 0 && (int)p9[0] == 1)){
                                A++;
                            }
                            if(((int)p9[0] == 0 && (int)p2[0] == 1)) {
                                A++;
                            }

                            /* druhy algoritmus - vyzera byt horsi
                            int C = ( (ivt((int)p2[0]) & ( (int)p3[0] | (int)p4[0] )) + (ivt((int)p4[0]) & ( (int)p5[0] | (int)p6[0] )) + (ivt((int)p6[0]) & ( (int)p7[0] | (int)p8[0] )) + (ivt((int)p8[0]) & ( (int)p9[0] | (int)p2[0] )) );
                            int N1 = ( ((int)p9[0] | (int)p2[0])  +  ((int)p3[0] | (int)p4[0])  +  ((int)p5[0] | (int)p6[0]) + ((int)p7[0] | (int)p8[0]) );
                            int N2 = ( ((int)p2[0] | (int)p3[0])  +  ((int)p4[0] | (int)p5[0])  +  ((int)p6[0] | (int)p7[0]) + ((int)p8[0] | (int)p9[0]) );
                            int N = N1 < N2 ? N1 : N2;
                            int m = iter == 0 ?  (((int)p6[0] | (int)p7[0] | ivt((int)p9[0])) & (int)p8[0]) : (((int)p2[0] | (int)p3[0] | ivt((int)p5[0])) & (int)p4[0]) ;

                            if(C == 1 && (N >= 2 && N <= 6) && m == 0){
                                marker.put(k, l, data_input);
                            }*/

                            B = (int)p2[0] + (int)p3[0] + (int)p4[0] + (int)p5[0] + (int)p6[0] + (int)p7[0] + (int)p8[0] + (int)p9[0];
                            m1 = iter == 0 ? ((int)p2[0] * (int)p4[0] * (int)p6[0]) : ((int)p2[0] * (int)p4[0] * (int)p8[0]);
                            m2 = iter == 0 ? ((int)p4[0] * (int)p6[0] * (int)p8[0]) : ((int)p2[0] * (int)p6[0] * (int)p8[0]);

                            if(A == 1 && (B >= 2 && B <= 6) && m1 == 0 && m2 == 0){
                                marker.put(k, l, data_input);
                            }
                            A = 0;
                            //spadne ked sa dostane na posledny pixel prveho riadku ! (musel som v for cykloch dat -1 na stlpce a -1 na riadky) - upravene minus jeden block

                        }
                    }
            }
        }

        Core.bitwise_not(marker, marker);
        Core.bitwise_and(image, marker, image);
    }

    private int ivt(int x){
        if( x==1 ){
            return 0;
        }else{
            return 1;
        }
    }

    public void removeSinglePoint(Mat image){
        double[] data_input = new double[1];
        data_input[0] = 0;

        for(int i = 0; i < blocksHeight-1; i++){
            for (int j = 0; j < blocksWidth - 1; j++) {
                if (mask[j][i] == 1) {
                    for (int k = i * BLOCK_SIZE; k < i * BLOCK_SIZE + BLOCK_SIZE; k++) {
                        for (int l = j * BLOCK_SIZE; l < j * BLOCK_SIZE + BLOCK_SIZE; l++) {
                            pC = image.get(k, l);
                            p2 = image.get(k-1, l);
                            p3 = image.get(k-1, l+1);
                            p4 = image.get(k, l+1);
                            p5 = image.get(k+1, l+1);
                            p6 = image.get(k+1, l);
                            p7 = image.get(k+1, l-1);
                            p8 = image.get(k, l-1);
                            p9 = image.get(k-1, l-1);

                            if( pC[0] == 255.0 && p2[0] == 0.0 &&  p3[0] == 0.0 &&  p4[0] == 0.0 &&  p5[0] == 0.0 &&  p6[0] == 0.0 &&  p7[0] == 0.0 &&  p8[0] == 0.0 &&  p9[0] == 0.0 ){
                                image.put(k, l, data_input);
                            }
                        }
                    }
                }
            }

        }
    }

    class AsyncTaskSegmentation extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onProgressUpdate(0);
            mProgressBarText.setText(R.string.thinning_running);
        }

        @Override
        protected String doInBackground(String... params) {

            Mat image = help.bitmap2mat(imageBitmap);
            int progress = 0;

            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

            blocksWidth = (int)Math.floor(image.width()/BLOCK_SIZE);
            blocksHeight = (int)Math.floor(image.height()/BLOCK_SIZE);

            Core.divide(image, Scalar.all(255.0), image);
            Mat prev = Mat.zeros(image.size(), CvType.CV_8UC1);
            Mat diff = new Mat(image.rows(), image.cols(), CvType.CV_8UC1);

            do{
                thinningIteration(image, 0);
                thinningIteration(image, 1);
                Core.absdiff(image, prev, diff);
                image.copyTo(prev);

                if( progress >= 100)
                    progress = 90;
                progress += 10;
                publishProgress( progress );

            }while(Core.countNonZero(diff) > 0);
            Core.multiply(image, Scalar.all(255.0), image);

            //repare skeleton
            removeSinglePoint(image);
            publishProgress(100);

            Utils.matToBitmap(image, imageAftefThinning);

            return "thinning_finished";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            pb.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mProgressBarText.setText(R.string.thinning_finished);
            mThinningImage.setImageBitmap(imageAftefThinning);

            mProgresBarLayout.setVisibility(View.GONE);

            mNextProcess.setEnabled(true);
            mNextProcess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPreprocessing(imageAftefThinning);
                }
            });

            if( type.equals(AUTOMATIC_FULL) )
                startPreprocessing(imageAftefThinning);
        }

    }


    public void settingsDialog(){
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_settings);
        dialog.setTitle(R.string.settings);

        dialogButton = (Button) dialog.findViewById(R.id.popUpOK);
        mSettingTitleText = (TextView) dialog.findViewById(R.id.popUpSettingTextTitle);
        mSettingTitleText.setText(R.string.thinning_settings_title);
        mEdittextTitle = (TextView) dialog.findViewById(R.id.text_for_edittext);
        mEdittextTitle.setText(R.string.thinning_block);
        mThinningBlock = (EditText) dialog.findViewById(R.id.settings_edittext);
        mThinningBlock.setText(String.valueOf(BLOCK_SIZE));

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( !mThinningBlock.getText().toString().isEmpty() )
                    BLOCK_SIZE = Integer.valueOf(mThinningBlock.getText().toString());

                if( BLOCK_SIZE > 0 && BLOCK_SIZE < 100 ){
                    dialog.dismiss();

                    mProgresBarLayout.setVisibility(View.VISIBLE);
                    new AsyncTaskSegmentation().execute();
                }

            }
        });

        dialog.show();
    }
}