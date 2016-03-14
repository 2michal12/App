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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Michal on 27.01.16.
 */
public class Extraction extends AppCompatActivity {

    private static Help help;
    private static Bitmap imageBitmap;
    private static Bitmap imageAftefExtraction;

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.progressBar) ProgressBar pb;
    @Bind(R.id.view_extraction_image) ImageView mExtractionImage;
    @Bind(R.id.next) Button mNextProcess;
    @Bind(R.id.settings) Button mSettings;
    @Bind(R.id.progress_bar_text) TextView mProgressBarText;
    @Bind(R.id.progress_bar_layout) RelativeLayout mProgresBarLayout;

    private static int BLOCK_SIZE;
    private static int[][] mask;
    private static String type;
    private static double[][] orientation_map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extraction);
        ButterKnife.bind(this);

        help = new Help(this);

        setSupportActionBar(toolbar);
        if( getSupportActionBar() != null )
            getSupportActionBar().setTitle(R.string.extraction);

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
        orientation_map = SharedData.orientation_map;

        byte[] byteArray = getIntent().getByteArrayExtra(help.BITMAP_IMAGE);
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
        menu.getItem(1).setVisible(false);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) { //due to finishAffinity(); supported from API 16
            menu.getItem(4).setVisible(false);  //exit app
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        help.menuItemOtherActivities(id, imageAftefExtraction, help.EXTRACTION);
        return super.onOptionsItemSelected(item);
    }

    private void startPreprocessing(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Intent i = new Intent(this, Extraction.class);
        i.putExtra(help.BITMAP_IMAGE, byteArray);
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
                    if (mask[j][i] == 1 && testMaskEdge(j, i)){
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
                }
                progress+=mod;
                publishProgress(progress+mod);
            }


            //copy image to color image
            Mat color_image = new Mat(image.rows(), image.cols(), CvType.CV_8UC3);

            //copy image to color image
            double[] black3 = new double[3];
            black3[0] = 0;
            black3[1] = 0;
            black3[2] = 0;
            double[] white3 = new double[3];
            white3[0] = 255;
            white3[1] = 255;
            white3[2] = 255;

            for(int i = 0; i < image.height(); i++){
                for(int j = 0; j < image.width(); j++){
                    if(image.get(i, j)[0] > 100){
                        color_image.put(i, j, white3);
                    }else{
                        color_image.put(i, j, black3);
                    }

                }
            }

            //findFragments(image);

            //print minutie
            int SIZE = help.SIZE_BETWEEN_MINUTIE;
            int fix_val_x, fix_val_y;

            if( params[0].equals( getResources().getString(R.string.minutie_ending) ) ){

                //cistenie blizkych markantov
                for(int j = 0; j < countEndings; j++) {
                    fix_val_x = endings[1][j];
                    fix_val_y = endings[0][j];

                    for(int i = 0; i < countEndings; i++) {
                        if( (endings[0][i]!=fix_val_y) && (endings[1][i]!=fix_val_x) && ( ( Math.abs(fix_val_x - endings[1][i])) <= SIZE) && ( (Math.abs(fix_val_y - endings[0][i])) <= SIZE)  ){
                            endings[1][i] = 0;
                            endings[0][i] = 0;
                            endings[1][j] = 0;
                            endings[0][j] = 0;
                            break;
                        }
                    }
                }

                for(int i = 0; i < countEndings; i++){
                    if(endings[1][i] != 0 && endings[0][i] != 0) {
                        Point core = new Point(endings[1][i], endings[0][i]);
                        Imgproc.circle(color_image, core, 8, new Scalar(251, 18, 34), 2);
                        System.out.println(endings[1][i]+"  "+endings[0][i]+"  "+Math.toDegrees( orientation_map[endings[0][i]][endings[1][i]] )+"  Q");
                    }
                }
            }else if(params[0].equals( getResources().getString(R.string.minutie_bifurcation) ) ){

                //cistenie blizkych markantov
                for(int j = 0; j < countBifurcation; j++) {
                    fix_val_x = bifurcation[1][j];
                    fix_val_y = bifurcation[0][j];

                    for(int i = 0; i < countBifurcation; i++) {
                        if( (bifurcation[0][i]!=fix_val_y) && (bifurcation[1][i]!=fix_val_x) && ( ( Math.abs(fix_val_x - bifurcation[1][i])) <= SIZE) && ( (Math.abs(fix_val_y - bifurcation[0][i])) <= SIZE)  ){
                            bifurcation[1][i] = 0;
                            bifurcation[0][i] = 0;
                            bifurcation[1][j] = 0;
                            bifurcation[0][j] = 0;
                            break;
                        }
                    }
                }

                for(int i = 0; i < countBifurcation; i++){
                    if(bifurcation[1][i] != 0 && bifurcation[0][i] != 0) {
                        Point core = new Point(bifurcation[1][i], bifurcation[0][i]);
                        Imgproc.circle(color_image, core, 8, new Scalar(102, 255, 51), 2);
                        System.out.println(bifurcation[1][i] + "  " + bifurcation[0][i] + "  " +Math.toDegrees( orientation_map[bifurcation[0][i]][bifurcation[1][i]] ) + "  Q");
                    }
                }
            }

            Utils.matToBitmap(color_image, imageAftefExtraction);

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

    int[] testOneNeighbourOfPoint(Mat image, int row, int col){
        int count = 0;
        int[] neighbour = new int[3];

        if(image.get(row-1,col-1)[0] == 255){
            neighbour[0] = row-1;
            neighbour[1] = col-1;
            count++;
        }
        if(image.get(row-1,col)[0] == 255){
            neighbour[0] = row-1;
            neighbour[1] = col;
            count++;
        }
        if(image.get(row-1,col+1)[0] == 255){
            neighbour[0] = row-1;
            neighbour[1] = col+1;
            count++;
        }
        if(image.get(row,col-1)[0] == 255){
            neighbour[0] = row;
            neighbour[1] = col-1;
            count++;
        }
        if(image.get(row,col+1)[0] == 255){
            neighbour[0] = row;
            neighbour[1] = col+1;
            count++;
        }
        if(image.get(row+1,col-1)[0] == 255){
            neighbour[0] = row+1;
            neighbour[1] = col-1;
            count++;
        }
        if(image.get(row+1,col)[0] == 255){
            neighbour[0] = row+1;
            neighbour[1] = col;
            count++;
        }
        if(image.get(row+1,col+1)[0] == 255){
            neighbour[0] = row+1;
            neighbour[1] = col+1;
            count++;
        }

        neighbour[2] = count; // last parameter set how much neighbours has current central point
        return neighbour;
    }

    public Mat findFragments(Mat image){
        Mat image_new = new Mat(image.rows(), image.cols(), CvType.CV_8UC1);

        double[] black1 = new double[1];
        black1[0] = 0;

        double[] white1 = new double[1];
        white1[0] = 255;

        int[][] fragment = new int[2][help.SIZE_OF_FRAGMENTS];

        double[] next_point = new double[2];
        next_point[0] = 0;
        next_point[1] = 0;
        boolean erase = false;
        int count;
        double[] gray1 = new double[1];
        gray1[0] = 100;
        int[] neighbour;
        int tempi,tempj;

        for(int i = 1; i < image.height()-1; i++) {
            for (int j = 1; j < image.width()-1; j++) {
                if(image.get(i,j)[0]==255) {
                    neighbour = testOneNeighbourOfPoint(image, i, j);
                    count = 0;
                    if (neighbour[2] == 1) {
                        fragment[0][0] = i;
                        fragment[1][0] = j;
                        image.put(i, j, black1);
                        count++;
                        for (int k = 0; k < help.SIZE_OF_FRAGMENTS - 1; k++) {
                            tempi = neighbour[0];
                            tempj = neighbour[1];
                            neighbour = testOneNeighbourOfPoint(image, neighbour[0], neighbour[1]);
                            if(neighbour[2] > 1){
                                erase = false;
                                break;
                            }else if (neighbour[2] != 1) {
                                if(k < help.SIZE_OF_FRAGMENTS-1){
                                    erase = true;
                                    fragment[0][k + 1] = tempi;
                                    fragment[1][k + 1] = tempj;
                                    count++;
                                    image.put(tempi, tempj, black1);
                                    break;
                                }else {
                                    erase = false;
                                    break;
                                }
                            }
                            fragment[0][k + 1] = tempi;
                            fragment[1][k + 1] = tempj;
                            image.put(tempi, tempj, black1);
                            count++;
                        }
                        for (int k = 0; k < count; k++) {
                            if(erase) {
                                image_new.put(fragment[0][k], fragment[1][k], gray1);
                            }else{
                                image.put(fragment[0][k], fragment[1][k], white1);
                            }
                            fragment[0][k] = 0;
                            fragment[1][k] = 0;
                        }
                        erase = false;

                    }
                }
            }
        }

        return image;
    }

    protected boolean testMaskEdge(int j, int i){
        int WHITE = 1;
        int BLACK = 0;

        if(mask[j][i] == WHITE && (mask[j-1][i]==BLACK || mask[j-1][i+1]==BLACK || mask[j][i+1]==BLACK || mask[j+1][i+1]==BLACK || mask[j+1][i]==BLACK || mask[j+1][i-1]==BLACK || mask[j][i-1]==BLACK || mask[j-1][i-1]==BLACK ) ){
            return false;
        }
        return true;
    }

    public void settingsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_settings_extraction);
        dialog.setTitle(R.string.settings);

        Button dialogButton = (Button) dialog.findViewById(R.id.popUpOK);
        final RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.radioButtonGroup);

        final EditText mSize = (EditText) dialog.findViewById(R.id.settingsEdittext);
        mSize.setText(String.valueOf(help.SIZE_BETWEEN_MINUTIE));

        final EditText mSizeFragments = (EditText) dialog.findViewById(R.id.settingsEdittext2);
        mSizeFragments.setText(String.valueOf(help.SIZE_OF_FRAGMENTS));

        dialogButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {

                                                int selectedId = radioGroup.getCheckedRadioButtonId();
                                                Button radioButton = (RadioButton) dialog.findViewById(selectedId);
                                                if (!mSize.getText().toString().isEmpty() && !mSizeFragments.toString().isEmpty()) {
                                                    help.SIZE_BETWEEN_MINUTIE = Integer.valueOf(mSize.getText().toString());
                                                    help.SIZE_OF_FRAGMENTS = Integer.valueOf(mSizeFragments.getText().toString());
                                                }

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
                                        }

        );

        dialog.show();
        }

    }
