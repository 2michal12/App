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
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import butterknife.Bind;
import butterknife.ButterKnife;

public class Filtering extends AppCompatActivity {

    private static Help help;
    private static Bitmap imageBitmap;
    private static Bitmap imageAftefFiltering;

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.progressBar)
    ProgressBar pb;
    @Bind(R.id.view_filtering_image)
    ImageView mFilteringImage;
    @Bind(R.id.next)
    Button mNextProcess;
    @Bind(R.id.settings)
    Button mSettings;
    @Bind(R.id.progress_bar_text)
    TextView mProgressBarText;
    @Bind(R.id.progress_bar_layout)
    RelativeLayout mProgresBarLayout;

    private static int BLOCK_SIZE = 0; //velkost pouzita ako v segmentacii
    private static int[][] mask = null;
    private static Mat orientation_angle, orientation_gui;
    private static String type;

    //variables use when direction map is printing
    private double x1, y1, x2, y2; //points of line
    private Point calculate_point, static_point;
    Scalar sc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtering);
        ButterKnife.bind(this);

        help = new Help(this);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setTitle(R.string.filtering);

        mNextProcess.setEnabled(false);
        type = getIntent().getStringExtra(help.TYPE);
        BLOCK_SIZE = help.BLOCK_SIZE;
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
            imageAftefFiltering = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            mFilteringImage.setImageBitmap(imageBitmap);

            if (type.equals(help.AUTOMATIC)) {
                //FILTERING_BLOCK = 10; dorobit vypocet automatickeho zvysenia

                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            } else if (type.equals(help.AUTOMATIC_FULL)) {
                mSettings.setVisibility(View.GONE);
                mProgresBarLayout.setVisibility(View.VISIBLE);
                new AsyncTaskSegmentation().execute();
            } else {
                mSettings.setVisibility(View.VISIBLE);
                mSettings.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        settingsDialog();
                    }
                });
            }
        } else {
            mFilteringImage.setImageResource(R.drawable.ic_menu_report_image);
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
        help.menuItemOtherActivities(id, imageAftefFiltering, help.FILTERING);
        return super.onOptionsItemSelected(item);
    }

    private void startPreprocessing(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Bundle mBundle = new Bundle();
        mBundle.putSerializable(help.MASK, mask);

        Intent i = new Intent(this, Binarisation.class);
        i.putExtra(help.BITMAP_IMAGE, byteArray);
        i.putExtra(help.TYPE, type);
        i.putExtras(mBundle);
        startActivity(i);
    }

    private double[][] orientationMap(Mat image, int block) {
        double[][] orientation_map = new double[image.rows()][image.cols()];
        orientation_gui = new Mat(image.rows(), image.cols(), CvType.CV_8UC1);
        orientation_angle = new Mat(image.rows(), image.cols(), CvType.CV_64F);
        Mat gradientX = new Mat(image.rows(), image.cols(), CvType.CV_8UC1);
        Mat gradientY = new Mat(image.rows(), image.cols(), CvType.CV_8UC1);
        Mat tempImage = new Mat(image.rows(), image.cols(), CvType.CV_8UC1);
        image.copyTo(tempImage);

        Imgproc.Sobel(tempImage, gradientX, CvType.CV_64FC1, 1, 0, 3, 1, 0, 0); //CV_16S
        Imgproc.Sobel(tempImage, gradientY, CvType.CV_64FC1, 0, 1, 3, 1, 0, 0);

        double gauss_x, gauss_y;
        double[] data_x, data_y;
        double[] data_input = new double[1];
        int rows = image.rows() - block / 2;
        int cols = image.cols() - block / 2;

        for (int x = block / 2; x < rows - block / 2; x += block) {
            for (int y = block / 2; y < cols - block / 2; y += block) {
                gauss_x = -1;
                gauss_y = -1;

                for (int i = x - block / 2; i < x + block / 2; i++) {
                    for (int j = y - block / 2; j < y + block / 2; j++) {
                        data_x = gradientX.get(i, j);
                        data_y = gradientY.get(i, j);
                        gauss_x = gauss_x + (Math.pow(data_x[0], 2) - Math.pow(data_y[0], 2));
                        gauss_y = gauss_y + (2 * (data_x[0] * data_y[0]));

                        if (gauss_y != -1 && gauss_x != -1) {
                            data_input[0] = 0.5 * Math.atan2(gauss_y, gauss_x) + Math.PI / 2; //uhol v radianoch
                            orientation_angle.put(i, j, data_input);
                            orientation_map[i][j] = data_input[0];
                        } else {
                            data_input[0] = 0;
                            orientation_angle.put(x, y, data_input);
                            orientation_map[x][y] = data_input[0];
                        }
                    }
                }
            }
            //System.out.println((float) x / ((tempImage.rows() - block / 2) - 1) * 100);

        }

        mapExtermination(block);

        //Zapnut len v pripade ze chcem vykreslit smerovu mapu "orientation_gui"
        /*for (int i = 0; i<orientation_gui.rows() / block; i++){
            for (int j = 0; j<orientation_gui.cols() / block; j++){
                data_input = orientation_angle.get(i*block+block/2, j*block+block/2); //angle
                printLine(orientation_gui, block, j, i, data_input[0]);
            }
        }*/

        return orientation_map;
    }

    private void mapExtermination(int block) { // vyhladenie smerovej mapy
        Mat sinComponent = new Mat(orientation_angle.rows(), orientation_angle.cols(), CvType.CV_64F);
        Mat cosComponent = new Mat(orientation_angle.rows(), orientation_angle.cols(), CvType.CV_64F);
        Mat sinOutput = new Mat(orientation_angle.rows(), orientation_angle.cols(), CvType.CV_64F);
        Mat cosOutput = new Mat(orientation_angle.rows(), orientation_angle.cols(), CvType.CV_64F);

        double[] data = new double[1];
        double[] cos = new double[1];
        double[] sin = new double[1];
        ;
        for (int i = 0; i < orientation_angle.rows(); i++) {
            for (int j = 0; j < orientation_angle.cols(); j++) {
                data = orientation_angle.get(i, j);
                cos[0] = Math.cos(2 * data[0]);
                cosComponent.put(i, j, cos);
                sin[0] = Math.sin(2 * data[0]);
                sinComponent.put(i, j, sin);
            }
        }

        Size kernel = new Size((2 * block) - 1, (2 * block) - 1);

        Imgproc.GaussianBlur(sinComponent, sinOutput, kernel, 10, 10);
        Imgproc.GaussianBlur(cosComponent, cosOutput, kernel, 10, 10);

        for (int i = 1; i < cosOutput.rows(); i++) {
            for (int j = 1; j < sinOutput.cols(); j++) {
                sin = sinOutput.get(i, j);
                cos = cosOutput.get(i, j);
                data[0] = 1 / 2.0 * (Math.atan2(sin[0], cos[0]));
                orientation_angle.put(i - 1, j - 1, data);
            }
        }

    }

    private void printLine(Mat image, int block, int i, int j, double angle) {
        x1 = block / 2.0 + (i * block);
        y1 = block / 2.0 + (j * block);
        x2 = block + (i * block);
        y2 = block / 2.0 + (j * block);

        calculate_point = new Point(((x2 - x1) * Math.cos(angle) - (y2 - y1) * Math.sin(angle)) + block / 2.0 + (i * block), ((x2 - x1) * Math.sin(angle) + (y2 - y1) * Math.cos(angle)) + block / 2.0 + (j * block));
        static_point = new Point(x1, y1);
        sc = new Scalar(255, 255, 255);

        Imgproc.line(image, static_point, calculate_point, sc, 2, 4, 0); //color of line, thickness, type, shift
    }

    private void frequenceMap(Mat image, int block) {
        Point center;
        double angle;
        double[] data;
        double[] min_max;
        double[] data_input = new double[1];
        Size kernel;
        RotatedRect rRect;
        Mat M = new Mat(image.rows(), image.cols(), CvType.CV_64F);
        Mat rotate = new Mat(image.rows(), image.cols(), CvType.CV_64F);
        Mat crop = new Mat(image.rows(), image.cols(), CvType.CV_64F);

        for (int i = 0; i < image.rows() / block; i++) { //x
            for (int j = 0; j < image.cols() / block; j++) { //y

                center = new Point(j * block + block / 2, i * block + block / 2);
                data = orientation_angle.get(i * block, j * block);
                angle = (data[0] + Math.PI / 2) * 180 / Math.PI; //uhol do stupnov

                kernel = new Size(3 * block, 2 * block);
                rRect = new RotatedRect(center, kernel, angle);
                M = Imgproc.getRotationMatrix2D(rRect.center, angle, 1.0); //otocenie
                Imgproc.warpAffine(image, rotate, M, image.size(), Imgproc.INTER_CUBIC); //rotacia na 0 stupnov
                Imgproc.getRectSubPix(rotate, rRect.size, rRect.center, crop); //vyber ROI

                Vector xSignature = new Vector();
                for (int k = 0; k < crop.cols(); k++) {
                    int sum = 0;
                    for (int d = 0; d < crop.rows(); d++) {
                        data = crop.get(d, k);
                        sum = sum + (int) data[0];
                    }
                    xSignature.add((float) sum / block);
                }

                Vector xSignature2 = new Vector(); //xSignatura pre vypocet sigmy v Gaborovom filtri
                for (int index = 0; index < xSignature.size(); index++) {
                    xSignature2.add(Math.abs((float) xSignature.get(index) - 255.0));
                }

                //min_max = localMinMax(xSignature2);

                //System.out.println(min_max[0]+" * "+min_max[1]);

                for (int k = 0; k < block; k++) {
                    for (int l = 0; l < block; l++) {
                        //this->sigma.at<double>(i*velkost_bloku+k,j*velkost_bloku+l) = vysledok_min; // hodnota urcena pre sigma v Gabore
                        //this->frekvencnaMat.at<double>(i*velkost_bloku+k,j*velkost_bloku+l) = 2*vysledok; //frekvencncia
                        //data_input[0] = 10 * min_max[1];
                        image.put(i * block + k, j * block + l, data_input);
                    }
                }

            }
            //System.out.println( (float)i/((image.rows() / block)-1)*100 ) ;
        }

    }

    private void gaussianFilter(Mat image) {
        Size kernel = new Size(help.GABOR_KERNEL_SIZE, help.GABOR_KERNEL_SIZE);
        Imgproc.GaussianBlur(image, image, kernel, help.GABOR_STRENGTH, help.GABOR_STRENGTH);
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
        for(int i = 0; i <= padding_x; i++) //clear padding column on the left
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
        for(int i = 0; i <= padding_y; i++) //clear padding row at the top
        {
            for(int j = 0; j < image.width(); j++)
            {
                image.put(i, j, data);
            }
        }
    }

    class AsyncTaskSegmentation extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onProgressUpdate(0);
            mProgressBarText.setText(R.string.filtering_running);
        }

        @Override
        protected String doInBackground(String... params) {

            Mat image = help.bitmap2mat(imageBitmap);

            Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

            //image = enhanceImg(image); //gaborKernel in this function use from opencv library, fast but not so good
            //gaussianFilter(image);

            double[][] orientation_map = orientationMap(image, help.ORIENTATION_MAP_BLOCK); //calculating of fingerprint orientation map
            //frequenceMap(image, FILTERING_BLOCK); //calculating of fingerprint frequency map

            Mat dest = new Mat(image.rows(), image.cols(), image.type());
            Mat kernel;
            Size dim = new Size(help.GABOR_KERNEL_SIZE, help.GABOR_KERNEL_SIZE);
            double final_value = 0.0;
            int u = 0, v = 0;

            //variables for publishing state in progress bar
            int start = (help.GABOR_KERNEL_SIZE - 1) / 2;
            int end = image.rows() - (help.GABOR_KERNEL_SIZE - 1) / 2;
            double progress = 100.0 / (end - start);

            for (int i = (help.GABOR_KERNEL_SIZE - 1) / 2; i < image.rows() - (help.GABOR_KERNEL_SIZE - 1) / 2; i++) {
                for (int j = (help.GABOR_KERNEL_SIZE - 1) / 2; j < image.cols() - (help.GABOR_KERNEL_SIZE - 1) / 2; j++) {

                    if (orientation_map[i][j] > Math.PI / 2) {
                        orientation_map[i][j] -= Math.PI / 2;
                    } else {
                        orientation_map[i][j] += Math.PI / 2;
                    }
                    kernel = Imgproc.getGaborKernel(dim, help.GABOR_STRENGTH, orientation_map[i][j], help.GABOR_FREQUENCY, help.GABOR_RATIO, help.GABOR_PSI, CvType.CV_64F);

                    for (int k = i - (help.GABOR_KERNEL_SIZE - 1) / 2; k < i + help.GABOR_KERNEL_SIZE - (help.GABOR_KERNEL_SIZE - 1) / 2; k++) { //ked mam vypocitany kernel pre dany bod tak urobim okolo neho blok o velkosti gaborovho filtra
                        for (int l = j - (help.GABOR_KERNEL_SIZE - 1) / 2; l < j + help.GABOR_KERNEL_SIZE - (help.GABOR_KERNEL_SIZE - 1) / 2; l++) {
                            final_value = final_value + ((image.get(k, l)[0]) * (kernel.get(u, v)[0]));
                            v++;
                        }
                        v = 0;
                        u++;
                    }
                    u = 0;

                    dest.put(i, j, final_value);
                    final_value = 0.0;
                }
                publishProgress((int) (progress * i));
            }

            int padding_x = image.width() - (((int)Math.floor(image.width()/BLOCK_SIZE))*BLOCK_SIZE);
            int padding_y = image.height() - (((int)Math.floor(image.height()/BLOCK_SIZE))*BLOCK_SIZE);
            clearPadding(dest, padding_x, padding_y);

            Utils.matToBitmap(dest, imageAftefFiltering); //ak chcem vykreslit smerovu mapu staci zmenit prvy parameter na "orientation_gui" a odkomentovat zapisovanie na konci funkcie "orientationMap"

            return "filtering_finished";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            pb.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            mProgressBarText.setText(R.string.filtering_finished);
            mFilteringImage.setImageBitmap(imageAftefFiltering);

            mProgresBarLayout.setVisibility(View.GONE);

            mNextProcess.setEnabled(true);
            mNextProcess.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPreprocessing(imageAftefFiltering);
                }
            });

            if (type.equals(help.AUTOMATIC_FULL))
                startPreprocessing(imageAftefFiltering);
        }

    }


    public void settingsDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_settings_filtering);
        dialog.setTitle(R.string.settings);

        final EditText mFilterSize = (EditText) dialog.findViewById(R.id.settingsEdittext);
        final EditText mFilterFrequency = (EditText) dialog.findViewById(R.id.settingsEdittext2);
        final EditText mFilterStrength = (EditText) dialog.findViewById(R.id.settingsEdittext3);
        final EditText mFilterRatio = (EditText) dialog.findViewById(R.id.settingsEdittext4);
        final EditText mFilterPsi = (EditText) dialog.findViewById(R.id.settingsEdittext5);

        mFilterSize.setText(String.valueOf(help.GABOR_KERNEL_SIZE));
        mFilterFrequency.setText(String.valueOf(help.GABOR_FREQUENCY));
        mFilterStrength.setText(String.valueOf(help.GABOR_STRENGTH));
        mFilterRatio.setText(String.valueOf(help.GABOR_RATIO));
        mFilterPsi.setText(String.valueOf(help.GABOR_PSI));

        Button dialogButton = (Button) dialog.findViewById(R.id.popUpOK);

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mFilterSize.getText().toString().isEmpty() && !mFilterFrequency.getText().toString().isEmpty() && mFilterStrength.getText().toString().isEmpty() && !mFilterRatio.getText().toString().isEmpty() && !mFilterPsi.getText().toString().isEmpty()) {
                    help.GABOR_KERNEL_SIZE = Integer.valueOf(mFilterSize.getText().toString());
                    help.GABOR_STRENGTH = Integer.valueOf(mFilterStrength.getText().toString());
                    help.GABOR_FREQUENCY = Integer.valueOf(mFilterFrequency.getText().toString());
                    help.GABOR_RATIO = Integer.valueOf(mFilterRatio.getText().toString());
                    help.GABOR_PSI = Integer.valueOf(mFilterPsi.getText().toString());
                }

                if (help.GABOR_KERNEL_SIZE > 0 && (help.GABOR_KERNEL_SIZE % 2 != 0) && help.GABOR_STRENGTH > 0 && help.GABOR_FREQUENCY > 0 && help.GABOR_RATIO >= 0.0 && help.GABOR_RATIO <= 1.0 && help.GABOR_PSI >= 0.0 && help.GABOR_PSI <= Math.PI) {
                    dialog.dismiss();
                    mProgresBarLayout.setVisibility(View.VISIBLE);
                    new AsyncTaskSegmentation().execute();
                }

            }
        });

        dialog.show();
    }

}
