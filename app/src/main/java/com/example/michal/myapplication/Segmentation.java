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

public class Segmentation extends AppCompatActivity{

    private static Help help;
    private static Bitmap imageAftefSegmentation;
    private static Bitmap imageBitmap ;
    private static Dialog dialog;

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.view_segmentation_image) ImageView mSegmentationImage;
    @Bind(R.id.next) Button mNextProcess;
    @Bind(R.id.settings) Button mSettings;
    @Bind(R.id.progress_bar_text) TextView mProgressBarText;
    @Bind(R.id.progress_bar_layout) RelativeLayout mProgresBarLayout;
    @Bind(R.id.progressBar) ProgressBar pb;

    private static int BLOCK_SIZE;
    private static final Integer SEGMENTATION_CLEANING = 10;
    private static boolean clearOnceEdges = false;
    private static int mask[][];
    private static String type;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_segmentation);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if( getSupportActionBar() != null )
            getSupportActionBar().setTitle(R.string.segmentation);

        help = new Help(this);
        mNextProcess.setEnabled(false);

        BLOCK_SIZE = help.BLOCK_SIZE;
        type = getIntent().getStringExtra(help.TYPE);
        byte[] byteArray = getIntent().getByteArrayExtra(help.BITMAP_IMAGE);
        if(byteArray != null) {

            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefSegmentation = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mSegmentationImage.setImageBitmap(imageBitmap);

            if( type.equals(help.AUTOMATIC) ) {
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
            mSegmentationImage.setImageResource(R.drawable.ic_menu_report_image);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.pull_in_left, R.anim.push_out_right);
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
        mBundle.putSerializable(help.MASK, mask);

        Intent i = new Intent(this, Normalisation.class);
        i.putExtra(help.BITMAP_IMAGE, byteArray);
        i.putExtra(help.TYPE,type);
        i.putExtra(help.TRESHOLD_NAME, help.TRESHOLD);
        i.putExtra(help.VARIANCE_NAME, help.VARIANCE);
        i.putExtras(mBundle);
        startActivity(i);
        overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left);
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
                help.VARIANCE += ( data[0]-treshold)*( data[0]-treshold);
            }
        }
        return (double)Math.round( help.VARIANCE/(image.width()*image.height()));
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

        for(int i = image.height(); i >= image.height() - 40; i--) //added due to wrong scanner (black at bottom)
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
            Help.setImage(image);


            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
            help.TRESHOLD = grayscaleTreshold(image, 0, 0, image.width(), image.height());
            help.VARIANCE = grayVariance(image, help.TRESHOLD);

            int blocksWidth = (int)Math.floor(image.width()/BLOCK_SIZE);
            int blocksHeight = (int)Math.floor(image.height()/BLOCK_SIZE);
            mask = new int[blocksWidth][blocksHeight];
            Help.maskHeight = blocksHeight;
            Help.maskWidth = blocksWidth;

            int padding_x = image.width() - (blocksWidth*BLOCK_SIZE);
            int padding_y = image.height() - (blocksHeight*BLOCK_SIZE);

            //calculate mask
            for(int i = 0; i < blocksWidth; i++)
            {
                for(int j = 0; j < blocksHeight; j++)
                {
                    if(help.TRESHOLD < grayscaleTreshold(image, i * BLOCK_SIZE, j * BLOCK_SIZE, i * BLOCK_SIZE + BLOCK_SIZE, j * BLOCK_SIZE+BLOCK_SIZE)) {
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

            //apply mask
            double[] data = new double[1];
            data[0] = 0;
            for(int i = 0; i < blocksHeight; i++)
            {
                for(int j = 0; j < blocksWidth; j++)
                {
                    if(mask[j][i] == 0)
                        for(int k = i*BLOCK_SIZE; k < i*BLOCK_SIZE+BLOCK_SIZE; k++)
                        {
                            for(int l = j*BLOCK_SIZE; l < j*BLOCK_SIZE+BLOCK_SIZE; l++)
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

            if( type.equals(help.AUTOMATIC_FULL) )
                startPreprocessing(imageAftefSegmentation);

        }

    }


    public void settingsDialog(){
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_settings);
        dialog.setTitle(R.string.settings);

        Button dialogButton = (Button) dialog.findViewById(R.id.popUpOK);
        TextView mSettingTitleText = (TextView) dialog.findViewById(R.id.popUpSettingTextTitle);
        TextView mEdittextTitle = (TextView) dialog.findViewById(R.id.textForEdittext);
        final EditText mSegmentationBlockSize = (EditText) dialog.findViewById(R.id.settingsEdittext);

        mSettingTitleText.setText(R.string.segmentation_settings_title);
        mEdittextTitle.setText(R.string.segmentation_block);
        mSegmentationBlockSize.setText(String.valueOf(BLOCK_SIZE));

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( !mSegmentationBlockSize.getText().toString().isEmpty() )
                    BLOCK_SIZE = Integer.valueOf(mSegmentationBlockSize.getText().toString());

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
