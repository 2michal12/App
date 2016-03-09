package com.example.michal.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Michal on 01/02/16.
 */
public class MaxMin extends AppCompatActivity {
    Bitmap imageBitmap = null, imageAfter;

    @Bind(R.id.text)
    TextView text;

    @Bind(R.id.image)
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maxmin);
        ButterKnife.bind(this);


        System.out.println("max min");
        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        if (byteArray != null) {

            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            imageAfter = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        }

        text.setText(imageBitmap.getHeight() + " " + imageBitmap.getWidth());
        imageView.setImageBitmap(imageBitmap);

        Help help = new Help(this);
        Mat image = help.bitmap2mat(imageBitmap);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

        if( !convertSkeleton(image) ){
            Log.i("convertSkeleton","Error in converting skeleton to reverse color.");
        }

        extraction(image, 1); //1 = ukoncenia, 2 = rozdvojenia

        Utils.matToBitmap(image, imageAfter);
        imageView.setImageBitmap(imageAfter);

    }

    public boolean convertSkeleton(Mat image){
        double[] black = new double[1];
        black[0] = 0;
        double[] white = new double[1];
        white[0] = 255;


        for(int i = 0; i < image.height(); i++){
            for(int j = 0; j < image.width(); j++){
                if(image.get(i, j)[0] == 255){
                    image.put(i, j, black);
                }else{
                    image.put(i, j, white);
                }

            }
        }
        return true;
    }

    public void extraction(Mat image, int type){
        int BLOCK_SIZE = 7;
        double[] data = new double[1];
        data[0] = 0;
        int cn;
        int[][] endings = new int[2][image.rows()*image.cols()];
        int[][] bifurcation = new int[2][image.rows()*image.cols()];
        int countBifurcation = 0;
        int countEndings = 0;

        int blocksWidth = (int)Math.floor(image.width()/BLOCK_SIZE);
        int blocksHeight = (int)Math.floor(image.height()/BLOCK_SIZE);
        int mod = 100/(blocksHeight-1) ;


        for(int i = 0; i < blocksHeight-1; i++){
            for(int j = 0; j < blocksWidth-1; j++) {

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

        int fix_val_x, fix_val_y;
        int x, y;
        int SIZE = 10;
        for(int j = 0; j < countEndings; j++) {
            fix_val_x = endings[1][j];
            fix_val_y = endings[0][j];

            for(int i = 0; i < countEndings; i++) {
                x = Math.abs(fix_val_x - endings[1][i]);
                y = Math.abs(fix_val_y - endings[0][i]);
                if( (endings[0][i]!=fix_val_y) && (endings[1][i]!=fix_val_x) && (x <= SIZE) && (y <= SIZE)  ){
                    endings[1][i] = 0;
                    endings[0][i] = 0;
                    endings[1][j] = 0;
                    endings[0][j] = 0;
                    break;
                }
            }
        }

            if( type == 1 ){
            for(int i = 0; i < countEndings; i++){
                Point core = new Point(endings[1][i], endings[0][i]);
                Imgproc.circle(image, core, 8, new Scalar(150, 0, 0), 2);
            }
        }else if( type == 2 ){
            for(int i = 0; i < countBifurcation; i++){
                Point core = new Point(bifurcation[1][i], bifurcation[0][i]);
                Imgproc.circle(image, core, 8, new Scalar(150, 0, 0), 2);
            }
        }

    }



}
