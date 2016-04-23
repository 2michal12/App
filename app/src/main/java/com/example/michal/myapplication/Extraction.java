package com.example.michal.myapplication;

import android.app.Dialog;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.util.List;
import java.util.Vector;
import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Michal on 27.01.16.
 */
public class Extraction extends AppCompatActivity {

    private static Help help;
    private static Bitmap imageBitmap;
    private static Bitmap imageAftefExtraction;
    private static Bitmap imageAftefExtractionOrig;

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.progressBar) ProgressBar pb;
    @Bind(R.id.view_extraction_image) ImageView mExtractionImage;
    @Bind(R.id.change_image) Button mChangeImage;
    @Bind(R.id.settings) Button mSettings;
    @Bind(R.id.progress_bar_text) TextView mProgressBarText;
    @Bind(R.id.progress_bar_layout) RelativeLayout mProgresBarLayout;

    private static int BLOCK_SIZE;
    private static int[][] mask;
    private static String type;
    private static double[][] orientation_map;
    private static boolean createTxt = false;
    private static StringBuilder ENDINGS_TXT = new StringBuilder("");
    private static StringBuilder BIFURCATIONS_TXT = new StringBuilder("");
    private static boolean isSetOrigImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extraction);
        ButterKnife.bind(this);

        help = new Help(this);

        setSupportActionBar(toolbar);
        if( getSupportActionBar() != null )
            getSupportActionBar().setTitle(R.string.extraction);

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
        orientation_map = Help.orientation_map;

        byte[] byteArray = getIntent().getByteArrayExtra(help.BITMAP_IMAGE);
        if (byteArray != null) {
            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefExtraction = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            imageAftefExtractionOrig = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mExtractionImage.setImageBitmap(imageBitmap);
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
        if (isSetOrigImage) {
            help.menuItemOtherActivities(id, imageAftefExtractionOrig, help.EXTRACTION);
        }else {
            help.menuItemOtherActivities(id, imageAftefExtraction, help.EXTRACTION);
        }
        return super.onOptionsItemSelected(item);
    }

    public void onCheckBoxTxt(View view) {
        boolean checked = ((CheckBox) view).isChecked();

        switch (view.getId()){
            case R.id.checkboxTxtId:
                if(checked){
                    createTxt = true;
                }
                break;
        }
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
            int progress = 0, cn;
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

            int SIZE = help.SIZE_BETWEEN_MINUTIE;
            int fix_val_x, fix_val_y;

            Help.restoreImages();
            Mat color_image = help.copyImageToRGB(image, 1);

            if( params[0].equals( getResources().getString(R.string.minutie_ending) ) ){
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
                        Imgproc.circle(Help.getImageEndings(), core, 8, new Scalar(251, 18, 34), 1);
                        ENDINGS_TXT.append(endings[1][i] +";"+ endings[0][i] +";"+ (int)Math.toDegrees(orientation_map[endings[0][i]][endings[1][i]]) +";Q\n");
                    }
                }
                Utils.matToBitmap(Help.getImageEndings(), imageAftefExtractionOrig);
            }else if(params[0].equals( getResources().getString(R.string.minutie_bifurcation) ) ){
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
                        Imgproc.circle(Help.getImageBifurcation(), core, 8, new Scalar(102, 255, 51), 1);
                        BIFURCATIONS_TXT.append(bifurcation[1][i] +";"+ bifurcation[0][i] +";"+ (int)Math.toDegrees(orientation_map[bifurcation[0][i]][bifurcation[1][i]]) +";Q\n");
                    }
                }
                Utils.matToBitmap(Help.getImageBifurcation(), imageAftefExtractionOrig);
            }else if(params[0].equals( getResources().getString(R.string.minutie_fragment) ) ) {
                Utils.matToBitmap(fragments(image,color_image), imageAftefExtraction);
                Utils.matToBitmap(Help.getImageFragment(), imageAftefExtractionOrig);
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
            if(createTxt) {
                if (!ENDINGS_TXT.toString().equals("")) {
                    help.saveTxtToExternalStorage(ENDINGS_TXT, help.ENDING_FILE);
                    ENDINGS_TXT.append("");
                }
                if (!BIFURCATIONS_TXT.toString().equals("")) {
                    help.saveTxtToExternalStorage(BIFURCATIONS_TXT, help.BIFURCATION_FILE);
                    BIFURCATIONS_TXT.append("");
                }
            }

            mProgressBarText.setText(R.string.thinning_finished);
            mExtractionImage.setImageBitmap(imageAftefExtraction);
            mProgresBarLayout.setVisibility(View.GONE);
            mChangeImage.setEnabled(true);
            mChangeImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!isSetOrigImage) {
                        mExtractionImage.setImageBitmap(imageAftefExtractionOrig);
                        isSetOrigImage = true;
                    } else {
                        mExtractionImage.setImageBitmap(imageAftefExtraction);
                        isSetOrigImage = false;
                    }
                }
            });
        }
    }

    Mat fragments(Mat image, Mat color){
        List<MatOfPoint> contours = new Vector<>();
        List<MatOfPoint> fragments = new Vector<>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(image, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE, new Point(0, 0));
        for (int i = 0; i < contours.size(); i++) {
            if( contours.get(i).size().height > help.SIZE_OF_FRAGMENTS_MIN && contours.get(i).size().height < help.SIZE_OF_FRAGMENTS_MAX){
                fragments.add(contours.get(i));
                contours.remove(i);
                i--;
            }
        }

        Imgproc.drawContours(color, contours, -1, new Scalar(255,255,255));
        Imgproc.drawContours(color, fragments, -1, new Scalar(255,255,0));

        return color;
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

        final EditText mSizeFragmentsMin = (EditText) dialog.findViewById(R.id.settingsEdittext2);
        mSizeFragmentsMin.setText(String.valueOf(help.SIZE_OF_FRAGMENTS_MIN));

        final EditText mSizeFragmentsMax = (EditText) dialog.findViewById(R.id.settingsEdittext3);
        mSizeFragmentsMax.setText(String.valueOf(help.SIZE_OF_FRAGMENTS_MAX));

        dialogButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                isSetOrigImage = false;
                                                int selectedId = radioGroup.getCheckedRadioButtonId();
                                                Button radioButton = (RadioButton) dialog.findViewById(selectedId);
                                                if (!mSize.getText().toString().isEmpty() && !mSizeFragmentsMin.toString().isEmpty() && !mSizeFragmentsMax.toString().isEmpty()) {
                                                    help.SIZE_BETWEEN_MINUTIE = Integer.valueOf(mSize.getText().toString());
                                                    help.SIZE_OF_FRAGMENTS_MIN = Integer.valueOf(mSizeFragmentsMin.getText().toString());
                                                    help.SIZE_OF_FRAGMENTS_MAX = Integer.valueOf(mSizeFragmentsMax.getText().toString());
                                                }

                                                if (radioButton.getText().equals(getResources().getString(R.string.minutie_ending))) {
                                                    dialog.dismiss();
                                                    mProgresBarLayout.setVisibility(View.VISIBLE);
                                                    new AsyncTaskSegmentation().execute(getResources().getString(R.string.minutie_ending));
                                                } else if (radioButton.getText().equals(getResources().getString(R.string.minutie_bifurcation))) {
                                                    dialog.dismiss();
                                                    mProgresBarLayout.setVisibility(View.VISIBLE);
                                                    new AsyncTaskSegmentation().execute(getResources().getString(R.string.minutie_bifurcation));
                                                } else if (radioButton.getText().equals(getResources().getString(R.string.minutie_fragment))) {
                                                    dialog.dismiss();
                                                    mProgresBarLayout.setVisibility(View.VISIBLE);
                                                    new AsyncTaskSegmentation().execute(getResources().getString(R.string.minutie_fragment));
                                                }
                                            }
                                        }

        );
        dialog.show();
        }

    }
