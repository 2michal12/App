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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.io.ByteArrayOutputStream;
import butterknife.Bind;
import butterknife.ButterKnife;

public class Thinning extends AppCompatActivity {

    private static Help help;
    private static Bitmap imageBitmap;
    private static Bitmap imageAftefThinning;

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.progressBar)
    ProgressBar pb;
    @Bind(R.id.view_thinning_image)
    ImageView mThinningImage;
    @Bind(R.id.next)
    Button mNextProcess;
    @Bind(R.id.settings)
    Button mSettings;
    @Bind(R.id.progress_bar_text)
    TextView mProgressBarText;
    @Bind(R.id.progress_bar_layout)
    RelativeLayout mProgresBarLayout;

    private static int BLOCK_SIZE;
    private static int[][] mask;
    private static int[][] mask2;
    int blocksWidth, blocksHeight;
    double[] pC, p2, p3, p4, p5, p6, p7, p8, p9;
    private static String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thinning);
        ButterKnife.bind(this);

        help = new Help(this);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.thinning);

        mNextProcess.setEnabled(false);
        type = getIntent().getStringExtra(help.TYPE);
        BLOCK_SIZE = help.BLOCK_SIZE;
        mask = null;
        Object[] objectArray = (Object[]) getIntent().getExtras().getSerializable(help.MASK);
        if (objectArray != null) {
            mask = new int[objectArray.length][];
            for (int i = 0; i < objectArray.length; i++) {
                mask[i] = (int[]) objectArray[i];
            }
        }

        byte[] byteArray = getIntent().getByteArrayExtra(help.BITMAP_IMAGE);
        if (byteArray != null) {

            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefThinning = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mThinningImage.setImageBitmap(imageBitmap);

            if (type.equals(help.AUTOMATIC)) {
                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            } else if (type.equals(help.AUTOMATIC_FULL)) {
                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            } else {
                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            }
        } else {
            mThinningImage.setImageResource(R.drawable.ic_menu_report_image);
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
        help.menuItemOtherActivities(id, imageAftefThinning, help.THINNING);
        return super.onOptionsItemSelected(item);
    }

    private void startExtraction(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Bundle mBundle = new Bundle();
        mBundle.putSerializable(help.MASK, mask2);

        Intent i = new Intent(this, Extraction.class);
        i.putExtra(help.BITMAP_IMAGE, byteArray);
        i.putExtra(help.TYPE, type);
        i.putExtras(mBundle);
        startActivity(i);
        overridePendingTransition(R.anim.pull_in_right, R.anim.push_out_left);
    }

    public void thinningIteration(Mat image, int iter) {
        Mat marker = Mat.zeros(image.size(), CvType.CV_8UC1);

        double[] data_input = new double[1];
        data_input[0] = 1;
        int A = 0, B, m1, m2;
        int BLACK = 0;
        int WHITE = 1;

        for (int i = 0; i < blocksHeight - 1; i++) {
            for (int j = 0; j < blocksWidth - 1; j++) {
                if (mask[j][i] == 1)
                    for (int k = i * BLOCK_SIZE; k < i * BLOCK_SIZE + BLOCK_SIZE; k++) {
                        for (int l = j * BLOCK_SIZE; l < j * BLOCK_SIZE + BLOCK_SIZE; l++) {

                            p2 = image.get(k - 1, l);
                            p3 = image.get(k - 1, l + 1);
                            p4 = image.get(k, l + 1);
                            p5 = image.get(k + 1, l + 1);
                            p6 = image.get(k + 1, l);
                            p7 = image.get(k + 1, l - 1);
                            p8 = image.get(k, l - 1);
                            p9 = image.get(k - 1, l - 1);

                            if ((int) p2[0] == BLACK && (int) p3[0] == WHITE) {
                                A++;
                            }
                            if (((int) p3[0] == BLACK && (int) p4[0] == WHITE)) {
                                A++;
                            }
                            if (((int) p4[0] == BLACK && (int) p5[0] == WHITE)) {
                                A++;
                            }
                            if (((int) p5[0] == BLACK && (int) p6[0] == WHITE)) {
                                A++;
                            }
                            if (((int) p6[0] == BLACK && (int) p7[0] == WHITE)) {
                                A++;
                            }
                            if (((int) p7[0] == BLACK && (int) p8[0] == WHITE)) {
                                A++;
                            }
                            if (((int) p8[0] == BLACK && (int) p9[0] == WHITE)) {
                                A++;
                            }
                            if (((int) p9[0] == BLACK && (int) p2[0] == WHITE)) {
                                A++;
                            }

                            B = (int) p2[0] + (int) p3[0] + (int) p4[0] + (int) p5[0] + (int) p6[0] + (int) p7[0] + (int) p8[0] + (int) p9[0];
                            m1 = iter == 0 ? ((int) p2[0] * (int) p4[0] * (int) p6[0]) : ((int) p2[0] * (int) p4[0] * (int) p8[0]);
                            m2 = iter == 0 ? ((int) p4[0] * (int) p6[0] * (int) p8[0]) : ((int) p2[0] * (int) p6[0] * (int) p8[0]);

                            if (A == 1 && (B >= 2 && B <= 6) && m1 == 0 && m2 == 0) {
                                marker.put(k, l, data_input);
                            }
                            A = 0;
                        }
                    }
            }
        }

        Core.bitwise_not(marker, marker);
        Core.bitwise_and(image, marker, image);
    }

    public void removeSinglePoint(Mat image) {
        double[] data_input = new double[1];
        data_input[0] = 0;
        double BLACK = 0.0, WHITE = 255.0;

        for (int i = 0; i < blocksHeight - 1; i++) {
            for (int j = 0; j < blocksWidth - 1; j++) {
                if (mask[j][i] == 1) {
                    for (int k = i * BLOCK_SIZE; k < i * BLOCK_SIZE + BLOCK_SIZE; k++) {
                        for (int l = j * BLOCK_SIZE; l < j * BLOCK_SIZE + BLOCK_SIZE; l++) {
                            pC = image.get(k, l);
                            p2 = image.get(k - 1, l);
                            p3 = image.get(k - 1, l + 1);
                            p4 = image.get(k, l + 1);
                            p5 = image.get(k + 1, l + 1);
                            p6 = image.get(k + 1, l);
                            p7 = image.get(k + 1, l - 1);
                            p8 = image.get(k, l - 1);
                            p9 = image.get(k - 1, l - 1);

                            if (pC[0] == WHITE && p2[0] == BLACK && p3[0] == BLACK && p4[0] == BLACK && p5[0] == BLACK && p6[0] == BLACK && p7[0] == BLACK && p8[0] == BLACK && p9[0] == BLACK) {
                                image.put(k, l, data_input);
                            }
                        }
                    }
                }
            }

        }
    }

    public void removeIslands(Mat image) {
        double[] data_input = new double[1];
        data_input[0] = 0;
        double WHITE = 255.0;
        int count = 0;

        for (int i = 0; i < blocksHeight - 1; i++) {
            for (int j = 0; j < blocksWidth - 1; j++) {
                if (mask[j][i] == 1) {
                    for (int k = i * BLOCK_SIZE; k < i * BLOCK_SIZE + BLOCK_SIZE; k++) {
                        for (int l = j * BLOCK_SIZE; l < j * BLOCK_SIZE + BLOCK_SIZE; l++) {
                            if( image.get(k, l)[0] == WHITE ){
                                if( image.get(k - 1, l)[0] == WHITE )count++;
                                if( image.get(k - 1, l + 1)[0] == WHITE )count++;
                                if( image.get(k, l + 1)[0] == WHITE )count++;
                                if( image.get(k + 1, l + 1)[0] == WHITE )count++;
                                if( image.get(k + 1, l)[0] == WHITE )count++;
                                if( image.get(k + 1, l - 1)[0] == WHITE )count++;
                                if( image.get(k, l - 1)[0] == WHITE )count++;
                                if( image.get(k - 1, l - 1)[0] == WHITE )count++;

                                if(count == 1){
                                    image.put(k, l, data_input);
                                }
                                count = 0;
                            }

                        }
                    }
                }
            }
        }

    }

    public void removeMaskEdges(Mat image) {
        double[] data_new = new double[1];
        mask2 = cloneArray(mask);

        for(int i = 0; i < blocksHeight; i++) {
            for (int j = 0; j < blocksWidth; j++) {
                if ( mask[j][i] == 0 ) {
                    for (int k = i * BLOCK_SIZE; k < i * BLOCK_SIZE + BLOCK_SIZE; k++) {
                        for (int l = j * BLOCK_SIZE; l < j * BLOCK_SIZE + BLOCK_SIZE; l++) {
                            data_new[0] = 0;
                            image.put(k, l, data_new);
                        }
                    }
                }
                if ( !testMaskEdge(j, i) ) {
                    mask2[j][i] = 0;
                    for (int k = i * BLOCK_SIZE; k < i * BLOCK_SIZE + BLOCK_SIZE; k++) {
                        for (int l = j * BLOCK_SIZE; l < j * BLOCK_SIZE + BLOCK_SIZE; l++) {
                            data_new[0] = 0;
                            image.put(k, l, data_new);
                        }
                    }
                }

            }
        }

    }

    public static int[][] cloneArray(int[][] src) {
        int length = src.length;
        int[][] target = new int[length][src[0].length];
        for (int i = 0; i < length; i++) {
            System.arraycopy(src[i], 0, target[i], 0, src[i].length);
        }
        return target;
    }

    protected boolean testMaskEdge(int j, int i){
        int WHITE = 1;
        int BLACK = 0;
        //tu niekde chyba !
        if(mask[j][i] == WHITE && (mask[j-1][i]==BLACK || mask[j-1][i+1]==BLACK || mask[j][i+1]==BLACK || mask[j+1][i+1]==BLACK || mask[j+1][i]==BLACK || mask[j+1][i-1]==BLACK || mask[j][i-1]==BLACK || mask[j-1][i-1]==BLACK ) ){
            return false;
        }
        return true;
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

            image = convert(image);

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

            for(int i = 0; i < help.ISLANDS_LENGTH_FILTER; i++)
                removeIslands(image);

            removeSinglePoint(image);

            removeMaskEdges(image);

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
                    startExtraction(imageAftefThinning);
                }
            });
        }

    }

    public Mat convert(Mat image){
        double[] black = new double[1];
        black[0] = 0;
        double[] white = new double[1];
        white[0] = 255;

        for(int i = 0; i < image.height(); i++){
            for(int j = 0; j < image.width(); j++){
                if(image.get(i, j)[0] == 0){
                    image.put(i, j, white);
                }else{
                    image.put(i, j, black);
                }

            }
        }
        return image;
    }
}