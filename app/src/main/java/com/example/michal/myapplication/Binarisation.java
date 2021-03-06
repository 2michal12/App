package com.example.michal.myapplication;

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

public class Binarisation extends AppCompatActivity {

    private static Help help;
    private static Bitmap imageBitmap;
    private static Bitmap imageAftefBinarisation;

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.progressBar) ProgressBar pb;
    @Bind(R.id.view_binarisation_image) ImageView mBinarisationImage;
    @Bind(R.id.next) Button mNextProcess;
    @Bind(R.id.settings) Button mSettings;
    @Bind(R.id.progress_bar_text) TextView mProgressBarText;
    @Bind(R.id.progress_bar_layout) RelativeLayout mProgresBarLayout;

    private static int BLOCK_SIZE;
    private static int[][] mask;
    private static String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binarisation);
        ButterKnife.bind(this);

        help = new Help(this);

        setSupportActionBar(toolbar);
        if( getSupportActionBar() != null )
            getSupportActionBar().setTitle(R.string.binarisation);

        mNextProcess.setEnabled(false);
        type = getIntent().getStringExtra(help.TYPE);
        BLOCK_SIZE = help.BLOCK_SIZE;
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
            imageAftefBinarisation = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mBinarisationImage.setImageBitmap(imageBitmap);

            if( type.equals(help.AUTOMATIC) ) {
                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            }else if( type.equals(help.AUTOMATIC_FULL) ){
                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            }else{
                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            }
        }else{
            mBinarisationImage.setImageResource(R.drawable.ic_menu_report_image);
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
        help.menuItemOtherActivities(id, imageAftefBinarisation, help.BINARISATION);
        return super.onOptionsItemSelected(item);
    }

    private void startPreprocessing(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Bundle mBundle = new Bundle();
        mBundle.putSerializable(help.MASK, mask);

        Intent i = new Intent(this, Thinning.class);
        i.putExtra(help.BITMAP_IMAGE, byteArray);
        i.putExtra(help.TYPE, type);
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

            double treshold = grayscaleTreshold(image, 0, 0, image.width(), image.height());

            int blocksWidth = (int)Math.floor(image.width()/BLOCK_SIZE);
            int blocksHeight = (int)Math.floor(image.height()/BLOCK_SIZE);

            for(int i = 0; i < blocksHeight-1; i++){
                for (int j = 0; j < blocksWidth - 1; j++) {
                    if (mask[j][i] == 1) {
                        for (int k = i * BLOCK_SIZE; k < i * BLOCK_SIZE + BLOCK_SIZE; k++) {
                            for (int l = j * BLOCK_SIZE; l < j * BLOCK_SIZE + BLOCK_SIZE; l++) {

                                data = image.get(k, l);
                                if (data[0] < treshold) {
                                    data[0] = 0;
                                    image.put(k, l, data);
                                } else {
                                    data[0] = 255;
                                    image.put(k, l, data);
                                }
                            }
                        }
                    }
                }
                if( i % mod == 0 ) {
                    progress++;
                }
                publishProgress( progress );
            }

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

            if( type.equals(help.AUTOMATIC_FULL) )
                startPreprocessing(imageAftefBinarisation);
        }

    }
}
