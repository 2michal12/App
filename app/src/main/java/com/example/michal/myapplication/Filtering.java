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
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class Filtering extends AppCompatActivity {
    private static Toolbar toolbar;
    private static Bitmap imageAftefFiltering;
    private static ImageView mFilteringImage;
    private static Button mNextProcess;
    private static Button mStartFiltering;

    Mat theta, orientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtering);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.filtering);
        setSupportActionBar(toolbar);

        mFilteringImage = (ImageView) findViewById(R.id.view_filtering_image);

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        if (byteArray != null) {

            final Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAftefFiltering = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

            mFilteringImage.setImageBitmap(image);

            mStartFiltering = (Button) findViewById(R.id.start_filtering);
            mStartFiltering.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startFiltering(bitmap2mat(image));
                    mFilteringImage.setImageBitmap(imageAftefFiltering);

                    mNextProcess = (Button) findViewById(R.id.next);
                    mNextProcess.setEnabled(true);
                }
            });
        } else {
            mFilteringImage.setImageResource(R.mipmap.ic_menu_report_image);
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
            case R.id.information:
                System.out.println("informacie");
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void startFiltering(Mat image) {
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

        orientationMap(image, 5);

        Utils.matToBitmap(image, imageAftefFiltering);
    }

    private void orientationMap(Mat image, int block){
        orientation = new Mat(image.cols(), image.rows(), CvType.CV_8UC1);
        theta = new Mat(image.cols(), image.rows(), CvType.CV_64F);
        Mat gradientX = new Mat(image.cols(), image.rows(), CvType.CV_8UC1);
        Mat gradientY = new Mat(image.cols(), image.rows(), CvType.CV_8UC1);
        Mat tempImage = new Mat(image.cols(), image.rows(), CvType.CV_8UC1);
        image.copyTo(tempImage);

        Imgproc.Sobel(tempImage, gradientX, CvType.CV_64FC1, 1, 0, 3, 1, 0, 0); //CV_16S
        Imgproc.Sobel(tempImage, gradientY, CvType.CV_64FC1, 0, 1, 3, 1, 0, 0);

        double gauss_x, gauss_y;
        double[] data_x, data_y;
        double[] data_input = new double[1];

        for(int x = block / 2; x < tempImage.rows() - block / 2; x++){
            for(int y=block / 2; y < tempImage.cols() - block / 2; y++){
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
                            theta.put(i, j, data_input);
                        }else {
                            data_input[0] = 0;
                            theta.put(x, y, data_input);
                        }
                    }
                }
            }
            System.out.println((float)x/((tempImage.rows() - block/2)-1)*100);
        }

        //mapExtermination(block);

        for (int i = 0; i<orientation.rows() / block; i++){
            for (int j = 0; j<orientation.cols() / block; j++){
                data_input = theta.get(i*block, j*block); //angle
                printLine(image, block, j, i, data_input[0]);
            }
        }
    }

    public void mapExtermination(int block){ //pracujem s globalnou Mat theta
        Mat sinComponent = new Mat(theta.cols(), theta.rows(), CvType.CV_64F);
        Mat cosComponent = new Mat(theta.cols(), theta.rows(), CvType.CV_64F);
        Mat sinOutput = new Mat(theta.cols(), theta.rows(), CvType.CV_64F);
        Mat cosOutput = new Mat(theta.cols(), theta.rows(), CvType.CV_64F);

        double[] data = new double[1];
        double[] cos = new double[1];
        double[] sin = new double[1];;
        for (int i = 0; i < theta.rows(); i++){
            for (int j = 0; j < theta.cols(); j++){
                data = theta.get(i, j);
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
                theta.put(i-1, j-1, data);
            }
        }
    }

    //ZMENIT SYNTAX !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    public void printLine(Mat image, int block, int i, int j, double angle){
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

    public Mat bitmap2mat(Bitmap src) {
        Mat dest = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, dest);
        return dest;
    }
}
