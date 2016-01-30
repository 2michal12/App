package com.example.michal.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.MediaStore;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
    private static int BLOCK_SIZE = 0; //velkost pouzita ako v segmentacii
    private static String type;

    private static RelativeLayout mProgresBarLayout;
    private static ProgressBar pb;
    private static Bitmap imageBitmap ;
    private static Button dialogButton;
    private static Button mSettings;
    private static TextView mProgressBarText;
    private static int[][] mask;


    private RadioGroup radioGroup;
    private RadioButton radioButton;

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
            imageAftefExtraction = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mExtractionImage.setImageBitmap(imageBitmap);

            //mSettings.setVisibility(View.GONE);
            //mProgresBarLayout.setVisibility(View.VISIBLE);
            //new AsyncTaskSegmentation().execute();
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
            int progress = 0;
            int cn = 0;
            int[][] endings = new int[2][image.rows()*image.cols()];
            int[][] bifurcation = new int[2][image.rows()*image.cols()];
            int countBifurcation = 0;
            int countEndings = 0;

            int blocksWidth = (int)Math.floor(image.width()/BLOCK_SIZE);
            int blocksHeight = (int)Math.floor(image.height()/BLOCK_SIZE);
            int mod = 100/(blocksHeight-1) ;


            for(int i = 0; i < blocksHeight-1; i++){
                for(int j = 0; j < blocksWidth-1; j++) {
                    if (mask[j][i] == 1)
                        for (int k = i * BLOCK_SIZE; k < i * BLOCK_SIZE + BLOCK_SIZE; k++) {
                            for (int l = j * BLOCK_SIZE; l < j * BLOCK_SIZE + BLOCK_SIZE; l++) {

                                if (image.get(k, l)[0] == 255) {
                                    cn = (int) Math.abs(image.get(k - 1, l - 1)[0] - image.get(k, l - 1)[0]) +
                                            (int) Math.abs(image.get(k, l - 1)[0] - image.get(k + 1, l - 1)[0]) +
                                            (int) Math.abs(image.get(k + 1, l - 1)[0] - image.get(k + 1, l)[0]) +
                                            (int) Math.abs(image.get(k + 1, l)[0] - image.get(k + 1, l + 1)[0]) +
                                            (int) Math.abs(image.get(k + 1, l + 1)[0] - image.get(k, l + 1)[0]) +
                                            (int) Math.abs(image.get(k, l + 1)[0] - image.get(k - 1, l + 1)[0]) +
                                            (int) Math.abs(image.get(k - 1, l + 1)[0] - image.get(k - 1, l)[0]) +
                                            (int) Math.abs(image.get(k - 1, l)[0] - image.get(k - 1, l - 1)[0]);

                                    if (((cn / 255) / 2) == 1) {
                                        endings[0][countEndings] = k;
                                        endings[1][countEndings] = l;
                                        countEndings++;
                                    } else if (((cn / 255) / 2) == 3) {
                                        bifurcation[0][countBifurcation] = k;
                                        bifurcation[1][countBifurcation] = l;
                                        countBifurcation++;
                                    }
                                }

                            }
                        }
                }
                progress+=mod;
                publishProgress(progress+mod);
            }

            if( params[0].equals( getResources().getString(R.string.minutie_ending) ) ){
                for(int i = 0; i < countEndings; i++){
                    Point core = new Point(endings[1][i], endings[0][i]);
                    Imgproc.circle(image, core, 8, new Scalar(150, 0, 0), 2);
                }
            }else if(params[0].equals( getResources().getString(R.string.minutie_bifurcation) ) ){
                for(int i = 0; i < countBifurcation; i++){
                    Point core = new Point(bifurcation[1][i], bifurcation[0][i]);
                    Imgproc.circle(image, core, 8, new Scalar(150, 0, 0), 2);
                }
            }


            Utils.matToBitmap(image, imageAftefExtraction);

            return "extraction_finished";
        }

        @Override
        protected void onProgressUpdate(Integer... values){
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
        dialog.setContentView(R.layout.popup_settings_extraction);
        dialog.setTitle(R.string.settings);

        dialogButton = (Button) dialog.findViewById(R.id.popUpOK);

        radioGroup = (RadioGroup) dialog.findViewById(R.id.radioButtonGroup);

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int selectedId = radioGroup.getCheckedRadioButtonId();
                radioButton = (RadioButton) dialog.findViewById(selectedId);

                if (radioButton.getText().equals(getResources().getString(R.string.minutie_ending))) {
                    dialog.dismiss();
                    mProgresBarLayout.setVisibility(View.VISIBLE);
                    new AsyncTaskSegmentation().execute(getResources().getString(R.string.minutie_ending));
                } else if (radioButton.getText().equals(getResources().getString(R.string.minutie_bifurcation))) {
                    dialog.dismiss();
                    mProgresBarLayout.setVisibility(View.VISIBLE);
                    new AsyncTaskSegmentation().execute(getResources().getString(R.string.minutie_bifurcation));
                }

            }
        });

        dialog.show();
    }

}
