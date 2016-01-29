package com.example.michal.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by Michal on 27.01.16.
 */
public class Extraction extends AppCompatActivity {

    private static Help help;
    private static Toolbar toolbar;
    private static ImageView mExtractionImage;
    private static Button mNextProcess;
    private static Bitmap imageAftefExtraction;
    private static EditText mThinningBlock;
    private static int BLOCK_SIZE = 0; //velkost pouzita ako v segmentacii


    private static RelativeLayout mProgresBarLayout;
    private static ProgressBar pb;
    private static Bitmap imageBitmap ;
    private static Button dialogButton;
    private static Button mSettings;
    private static TextView mProgressBarText;
    private static TextView mEdittextTitle;
    private static TextView mSettingTitleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extraction);
        help = new Help(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pb = (ProgressBar) findViewById(R.id.progressBar);
        mProgresBarLayout = (RelativeLayout) findViewById(R.id.progress_bar_layout);
        mProgressBarText = (TextView) findViewById(R.id.progress_bar_text);
        mNextProcess = (Button) findViewById(R.id.next);
        mNextProcess.setEnabled(false);
        mSettings = (Button) findViewById(R.id.settings);
        mExtractionImage = (ImageView) findViewById(R.id.view_extraction_image);

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        if (byteArray != null) {

            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefExtraction = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mExtractionImage.setImageBitmap(imageBitmap);

            mSettings.setVisibility(View.GONE);
            mProgresBarLayout.setVisibility(View.VISIBLE);
            new AsyncTaskSegmentation().execute();
            mSettings.setVisibility(View.VISIBLE);
            mSettings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    settingsDialog();
                }
            });

        } else {
            mExtractionImage.setImageResource(R.drawable.ic_menu_report_image);
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
                help.saveImageToExternalStorage(imageAftefExtraction, "extraction");
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

    class AsyncTaskSegmentation extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onProgressUpdate(0);
            mProgressBarText.setText(R.string.extraction_running);
        }

        @Override
        protected String doInBackground(String... params) {
            Mat image = help.bitmap2mat(imageBitmap);
            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

            double[] data = new double[1];
            data[0] = 0;
            int mod = image.width() / 100;
            int progress = 0;
            int cn = 0;
            int[][] endings = new int[2][image.rows()*image.cols()];
            int[][] bifurcation = new int[2][image.rows()*image.cols()];
            int countBifurcation = 0;
            int countEndings = 0;


            for(int i = 1; i < image.rows()-1; i++) {
                for (int j = 1; j < image.cols()-1; j++) {
                    if(image.get(i,j)[0] == 0) {
                        cn = (int) Math.abs(image.get(i - 1, j - 1)[0] - image.get(i, j - 1)[0]) +
                                (int) Math.abs(image.get(i, j - 1)[0] - image.get(i + 1, j - 1)[0]) +
                                (int) Math.abs(image.get(i + 1, j - 1)[0] - image.get(i + 1, j)[0]) +
                                (int) Math.abs(image.get(i + 1, j)[0] - image.get(i + 1, j + 1)[0]) +
                                (int) Math.abs(image.get(i + 1, j + 1)[0] - image.get(i, j + 1)[0]) +
                                (int) Math.abs(image.get(i, j + 1)[0] - image.get(i - 1, j + 1)[0]) +
                                (int) Math.abs(image.get(i - 1, j + 1)[0] - image.get(i - 1, j)[0]) +
                                (int) Math.abs(image.get(i - 1, j)[0] - image.get(i - 1, j - 1)[0]);

                        if (((cn / 255) / 2) == 1) {
                            endings[0][countEndings] = i;
                            endings[1][countEndings] = j;
                            countEndings++;
                        }else if(((cn / 255) / 2) == 3) {
                            bifurcation[0][countBifurcation] = i;
                            bifurcation[1][countBifurcation] = j;
                            countBifurcation++;
                        }
                    }

                }
                if( i % mod == 0 ) {
                    progress++;
                }
                publishProgress( progress );
            }

            for(int i = 0; i < countBifurcation; i++){
                Point core = new Point(bifurcation[1][i], bifurcation[0][i]);
                Imgproc.circle(image, core, 10, new Scalar(200, 0, 0), 2);
            }

            Utils.matToBitmap(image, imageAftefExtraction);

            return "extraction_finished";
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
            mExtractionImage.setImageBitmap(imageAftefExtraction);

            mProgresBarLayout.setVisibility(View.GONE);

            mNextProcess.setEnabled(true);
            mNextProcess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPreprocessing(imageAftefExtraction);
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
