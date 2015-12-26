package com.example.michal.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class Filtering extends AppCompatActivity {

    private static Help help;
    private static Toolbar toolbar;
    private static Bitmap imageAftefFiltering;
    private static ImageView mFilteringImage;
    private static Button mNextProcess;
    private static Button mStartFiltering;
    private static EditText mFilteringBlock;

    private static double treshold = 0.0;
    private static int[][] mask;

    private static int FILTERING_BLOCK;
    private static int GAUSS_SIZE = 5;
    private static int GAUSS_STRENGTH = 10;

    Mat orientation_angle, orientation_gui, frequence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtering);
        help = new Help(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.filtering);
        setSupportActionBar(toolbar);

        mFilteringImage = (ImageView) findViewById(R.id.view_filtering_image);

        treshold = getIntent().getDoubleExtra("Treshold",treshold);

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

            final Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefFiltering = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

            mFilteringBlock = (EditText) findViewById(R.id.filtering_block_edittext);
            mFilteringBlock.setText("9");

            mFilteringImage.setImageBitmap(image);

            mStartFiltering = (Button) findViewById(R.id.start_filtering);
            mStartFiltering.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FILTERING_BLOCK = Integer.parseInt(mFilteringBlock.getText().toString());
                    startFiltering(help.bitmap2mat(image));
                    mFilteringImage.setImageBitmap(imageAftefFiltering);

                    mNextProcess = (Button) findViewById(R.id.next);
                    mNextProcess.setEnabled(true);
                    mNextProcess.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startPreprocessing(imageAftefFiltering);
                        }
                    });
                }
            });
        } else {
            mFilteringImage.setImageResource(R.drawable.ic_menu_report_image);
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
                help.saveImageToExternalStorage(imageAftefFiltering, "filtering");
                break;
            case R.id.information:
                help.informationDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startPreprocessing(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Bundle mBundle = new Bundle();
        mBundle.putSerializable("Mask", mask);

        Intent i = new Intent(this, Binarisation.class);
        i.putExtra("BitmapImage", byteArray);
        i.putExtra("Treshold",treshold);
        i.putExtras(mBundle);
        startActivity(i);
    }

    private void startFiltering(Mat image) {
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

        //orientationMap(image, FILTERING_BLOCK);
        //frequenceMap(image, FILTERING_BLOCK);
        //gaussianFilter(image);

        Utils.matToBitmap(image, imageAftefFiltering); //ak chcem vykreslit smerovu mapu staci zmenit prvy parameter na "orientation"
    }

    private void orientationMap(Mat image, int block){
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

        for(int x = block / 2; x < rows - block / 2; x += block){
            for(int y=block / 2; y < cols - block / 2; y += block){
                gauss_x = -1;
                gauss_y = -1;

                for(int i = x - block / 2; i < x + block / 2; i++){
                    for(int j = y - block / 2; j < y + block / 2; j++) {
                        data_x = gradientX.get(i, j);
                        data_y = gradientY.get(i, j);
                        gauss_x = gauss_x + ( Math.pow(data_x[0], 2) - Math.pow(data_y[0], 2));
                        gauss_y = gauss_y + ( 2*(data_x[0] * data_y[0]) );

                        if(gauss_y != -1 && gauss_x != -1){
                            data_input[0] = 0.5*Math.atan2(gauss_y, gauss_x) + Math.PI/2; //uhol v radianoch
                            orientation_angle.put(i, j, data_input);
                        }else {
                            data_input[0] = 0;
                            orientation_angle.put(x, y, data_input);
                        }
                    }
                }
            }
            System.out.println((float) x / ((tempImage.rows() - block / 2) - 1) * 100);
        }

        //mapExtermination(block);

        for (int i = 0; i<orientation_gui.rows() / block; i++){
            for (int j = 0; j<orientation_gui.cols() / block; j++){
                data_input = orientation_angle.get(i*block+block/2, j*block+block/2); //angle
                printLine(orientation_gui, block, j, i, data_input[0]);
            }
        }
    }

    private void frequenceMap(Mat image, int block){
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

        for(int i = 0; i < image.rows() / block; i++){ //x
            for(int j = 0; j < image.cols() / block; j++) { //y

                center = new Point(j*block+block/2, i*block+block/2);
                data = orientation_angle.get(i*block, j*block);
                angle = ( data[0] + Math.PI/2 ) * 180/Math.PI; //uhol do stupnov

                kernel = new Size(3*block, 2*block);
                rRect = new RotatedRect(center, kernel, angle);
                M = Imgproc.getRotationMatrix2D(rRect.center, angle, 1.0); //otocenie
                Imgproc.warpAffine(image, rotate, M, image.size(), Imgproc.INTER_CUBIC); //rotacia na 0 stupnov
                Imgproc.getRectSubPix(rotate, rRect.size, rRect.center, crop); //vyber ROI

                Vector xSignature = new Vector();
                for (int k = 0; k < crop.cols(); k++){
                    int sum = 0;
                    for (int d = 0; d < crop.rows(); d++){
                        data = crop.get(d, k);
                        sum = sum + (int)data[0];
                    }
                    xSignature.add((float)sum/block);
                }

                Vector xSignature2 = new Vector(); //xSignatura pre vypocet sigmy v Gaborovom filtri
                for(int index = 0; index < xSignature.size(); index++){
                    xSignature2.add( Math.abs( (float)xSignature.get(index) - 255.0) );
                }

                min_max = localMinMax(xSignature2);

                //System.out.println(min_max[0]+" * "+min_max[1]);

                for(int k = 0; k < block; k++){
                    for(int l = 0; l < block; l++){
                        //this->sigma.at<double>(i*velkost_bloku+k,j*velkost_bloku+l) = vysledok_min; // hodnota urcena pre sigma v Gabore
                        //this->frekvencnaMat.at<double>(i*velkost_bloku+k,j*velkost_bloku+l) = 2*vysledok; //frekvencncia
                        data_input[0] = 10 * min_max[1];
                        image.put(i*block+k, j*block+l, data_input);
                    }
                }

            }
            System.out.println( (float)i/((image.rows() / block)-1)*100 ) ;
        }

    }

    private void gaussianFilter(Mat image){
        Size kernel = new Size(GAUSS_SIZE, GAUSS_SIZE);
        Imgproc.GaussianBlur(image, image, kernel, GAUSS_STRENGTH, GAUSS_STRENGTH);
    }

    private void mapExtermination(int block){ // vyhladenie smerovej mapy
        Mat sinComponent = new Mat(orientation_angle.rows(), orientation_angle.cols(), CvType.CV_64F);
        Mat cosComponent = new Mat(orientation_angle.rows(), orientation_angle.cols(), CvType.CV_64F);
        Mat sinOutput = new Mat(orientation_angle.rows(), orientation_angle.cols(), CvType.CV_64F);
        Mat cosOutput = new Mat(orientation_angle.rows(), orientation_angle.cols(), CvType.CV_64F);

        double[] data = new double[1];
        double[] cos = new double[1];
        double[] sin = new double[1];;
        for (int i = 0; i < orientation_angle.rows(); i++){
            for (int j = 0; j < orientation_angle.cols(); j++){
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

        for (int i = 1; i < cosOutput.rows(); i++){
            for (int j = 1; j < sinOutput.cols(); j++) {
                sin = sinOutput.get(i, j);
                cos = cosOutput.get(i, j);
                data[0] = 1 / 2.0 * ( Math.atan2(sin[0], cos[0]) );
                orientation_angle.put(i-1, j-1, data);
            }
        }
    }

    //ZMENIT SYNTAX !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    private void printLine(Mat image, int block, int i, int j, double angle){
        double x1, y1, x2, y2;
        x1 = block / 2.0 + (i*block);
        y1 = block / 2.0 + (j*block);
        x2 = block + (i*block);
        y2 = block / 2.0 + (j*block);

        Point calculate = new Point(((x2 - x1)*Math.cos(angle) - (y2 - y1)*Math.sin(angle)) + block / 2.0 + (i*block), ((x2 - x1)*Math.sin(angle) + (y2 - y1)*Math.cos(angle)) + block / 2.0 + (j*block));
        Point static_point = new Point(x1, y1);

        Scalar sc = new Scalar(255, 255, 255);
        Imgproc.line(image, static_point, calculate, sc, 2, 4, 0); //farba, hrubka, typ, shift
    }

    private double[] localMinMax(Vector<Double> vec){

        double array[] = new double[vec.size()];
        Double vectorValues;
        for(int i = 0; i != vec.size(); i++){
            vectorValues = (Double)vec.elementAt(i);
            array[i] = vectorValues.doubleValue();
        }

        ArrayList<Integer> mins = new ArrayList<Integer>();
        ArrayList<Integer> maxs = new ArrayList<Integer>();

        double prevDiff = array[0] - array[1];
        int i=1;
        while(i<array.length-1){
            double currDiff = 0;
            int zeroCount = 0;
            while(currDiff == 0 && i<array.length-1){
                zeroCount++;
                i++;
                currDiff = array[i-1] - array[i];
            }

            int signCurrDiff = Integer.signum((int)currDiff);
            int signPrevDiff = Integer.signum((int)prevDiff);
            if( signPrevDiff != signCurrDiff && signCurrDiff != 0){ //signSubDiff==0, the case when prev while ended bcoz of last elem
                int index = i-1-(zeroCount)/2;
                if(signPrevDiff == 1){
                    mins.add( index );
                }else{
                    maxs.add( index );
                }
            }
            prevDiff = currDiff;
        }

        double[] maxs_values = new double[maxs.size()];
        double[] mins_values = new double[mins.size()];

        int ind2 = 0;
        for(Integer ind : maxs){
            maxs_values[ind2++] = array[ind];
        }
        ind2 = 0;
        for(Integer ind : mins){
            mins_values[ind2++] = array[ind];
        }

        Arrays.sort(maxs_values);
        Arrays.sort(mins_values);

        //AZ TU PREBIEHA VYPOCET
        double sum = 0;
        for(int j = 0; j < maxs_values.length; j++){//priemerny pocet pixlov medzi dvoma maximami v xSignature
            if(j != (maxs_values.length - 1)){
                sum += maxs_values[j+1] - maxs_values[j] - 1;
            }
        }
        double vysledok = sum/maxs_values.length; //zmen na double
        if(vysledok<0){
            vysledok = 0;
        }
        sum = 0;
        for(int j = 0; j<mins_values.length; j++){
            if(j != (mins_values.length-1)){
                sum += mins_values[j+1] - mins_values[j] - 1;
            }
        }
        double vysledok_min = sum/mins_values.length; //zmen na double
        if(vysledok_min<0){
            vysledok_min = 0;
        }

        double[] min_max = new double[]{vysledok_min, vysledok};
        return min_max;
    }

}
