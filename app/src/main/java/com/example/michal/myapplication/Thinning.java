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

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;

public class Thinning extends AppCompatActivity {
    private static Toolbar toolbar;
    private static ImageView mThinningImage;
    private static Button mStartThinning;
    private static Button mNextProcess;
    private static Bitmap imageAftefThinning;

    private static int[][] mask;
    private static int BLOCK_SIZE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thinning);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.thinning);
        setSupportActionBar(toolbar);

        mThinningImage = (ImageView) findViewById(R.id.view_thinning_image);

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
            imageAftefThinning = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

            mThinningImage.setImageBitmap(image);

            mStartThinning = (Button) findViewById(R.id.start_thinning);
            mStartThinning.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startThinning(bitmap2mat(image));
                    mThinningImage.setImageBitmap(imageAftefThinning);

                    mNextProcess = (Button) findViewById(R.id.next);
                    mNextProcess.setEnabled(true);
                    mNextProcess.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //startPreprocessing(imageAftefBinarisation);
                        }
                    });
                }
            });
        } else {
            mThinningImage.setImageResource(R.mipmap.ic_menu_report_image);
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

    private void startPreprocessing(Bitmap image) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Intent i = new Intent(this, Thinning.class);
        i.putExtra("BitmapImage", byteArray);
        startActivity(i);
    }

    private void startThinning(Mat image){
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

        Core.divide(image, Scalar.all(255.0), image);

        Mat prev = Mat.zeros(image.size(), CvType.CV_8UC1);
        Mat diff = new Mat(image.rows(), image.cols(), CvType.CV_8UC1);

        do{
            thinningIteration(image, 0);
            thinningIteration(image, 1);
            Core.absdiff(image, prev, diff);
            image.copyTo(prev);
        }while(Core.countNonZero(diff) > 0);
        Core.multiply(image, Scalar.all(255.0), image);

        Utils.matToBitmap(image, imageAftefThinning);
    }

    public void thinningIteration(Mat image, int iter){
        Mat marker = Mat.zeros(image.size(), CvType.CV_8UC1);

        double[] p2, p3, p4, p5, p6, p7, p8, p9;
        double[] data_input = new double[1];
        data_input[0] = 1;
        int A = 0, B, m1, m2;

        int blocksWidth = (int)Math.floor(image.width()/BLOCK_SIZE);
        int blocksHeight = (int)Math.floor(image.height()/BLOCK_SIZE);

        for(int i = 0; i < blocksHeight-1; i++)
        {
            for(int j = 0; j < blocksWidth-1; j++)
            {
                if(mask[j][i] == 1)
                    for(int k = i*BLOCK_SIZE; k < i*BLOCK_SIZE+BLOCK_SIZE; k++)
                    {
                        for(int l = j*BLOCK_SIZE; l < j*BLOCK_SIZE+BLOCK_SIZE; l++)
                        {

                            p2 = image.get(k-1, l);
                            p3 = image.get(k-1, l+1);
                            p4 = image.get(k, l+1);
                            p5 = image.get(k+1, l+1);
                            p6 = image.get(k+1, l);
                            p7 = image.get(k+1, l-1);
                            p8 = image.get(k, l-1);
                            p9 = image.get(k-1, l-1);

                            if((int)p2[0] == 0 && (int)p3[0] == 1){
                                A++;
                            }
                            if(((int)p3[0] == 0 && (int)p4[0] == 1)){
                                A++;
                            }
                            if(((int)p4[0] == 0 && (int)p5[0] == 1)){
                                A++;
                            }
                            if(((int)p5[0] == 0 && (int)p6[0] == 1)){
                                A++;
                            }
                            if(((int)p6[0] == 0 && (int)p7[0] == 1)){
                                A++;
                            }
                            if(((int)p7[0] == 0 && (int)p8[0] == 1)){
                                A++;
                            }
                            if(((int)p8[0] == 0 && (int)p9[0] == 1)){
                                A++;
                            }
                            if(((int)p9[0] == 0 && (int)p2[0] == 1)) {
                                A++;
                            }


                            B = (int)p2[0] + (int)p3[0] + (int)p4[0] + (int)p5[0] + (int)p6[0] + (int)p7[0] + (int)p8[0] + (int)p9[0];
                            m1 = iter == 0 ? ((int)p2[0] * (int)p4[0] * (int)p6[0]) : ((int)p2[0] * (int)p4[0] * (int)p8[0]);
                            m2 = iter == 0 ? ((int)p4[0] * (int)p6[0] * (int)p8[0]) : ((int)p2[0] * (int)p6[0] * (int)p8[0]);

                            if(A == 1 && (B >= 2 && B <= 6) && m1 == 0 && m2 == 0){
                                marker.put(k, l, data_input);
                            }
                            A = 0;
                            //spadne ked sa dostane na posledny pixel prveho riadku ! (musel som v for cykloch dat -1 na stlpce a -1 na riadky) - upravene minus jeden block
                        }
                    }
            }
        }

        Core.bitwise_not(marker, marker);
        Core.bitwise_and(image, marker, image);
    }

    public Mat bitmap2mat(Bitmap src){
        Mat dest = new Mat(src.getWidth(), src.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(src, dest);
        return dest;
    }

}