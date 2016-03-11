package com.example.michal.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
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
    Mat image,color_image;

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
        image = help.bitmap2mat(imageBitmap);
        color_image = new Mat(image.rows(), image.cols(), CvType.CV_8UC3);
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
                if(image.get(i, j)[0] > 100){
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


        //copy image to color image
        double[] black = new double[3];
        black[0] = 0;
        black[1] = 0;
        black[2] = 0;
        double[] white = new double[3];
        white[0] = 255;
        white[1] = 255;
        white[2] = 255;

        for(int i = 0; i < image.height(); i++){
            for(int j = 0; j < image.width(); j++){
                if(image.get(i, j)[0] > 100){
                    color_image.put(i, j, white);
                }else{
                    color_image.put(i, j, black);
                }

            }
        }

        int dlzka_fragmentu = 2;
        int[][] fragment = new int[2][dlzka_fragmentu];

        double[] white_pixel;
        double[] point = new double[2];
        double[] next_point = new double[2];
        next_point[0] = 0;
        next_point[1] = 0;
        boolean erase = false;
        int count;
        double[] black1 = new double[1];
        black1[0] = 0;

        for(int i = 0; i < image.height(); i++) {
            for (int j = 0; j < image.width(); j++) {
                white_pixel = image.get(i,j);
                if(white_pixel[0] == 255){
                    point[0] = i;
                    point[1] = j;
                    count = 1;
                    for(int k = 1; k < dlzka_fragmentu; k++) { //dlzka fragmentu
                        erase = false;
                        next_point = testNeighbour(point);
                        fragment[0][0] = (int)point[0];
                        fragment[1][0] = (int)point[1];
                        if (next_point[0] == -1 && next_point[1] == -1) {
                            count++;
                            erase = true;
                            break;
                        } else {
                            count++;
                            point[0] = next_point[0];
                            point[1] = next_point[1];
                            fragment[0][k] = (int)next_point[0];
                            fragment[1][k] = (int)next_point[1];
                            if(k == dlzka_fragmentu-1)
                                erase = true;
                            else
                                erase = false;
                        }
                    }
                    if(erase){
                        //vymazat fragment
                        for (int k = 0; k < count; k++){
                            image.put(fragment[0][k],fragment[1][k], black1);
                        }
                        erase = false;
                    }
                }
            }
        }


        /*
        //print minutie
        int SIZE = 15;
        int fix_val_x, fix_val_y;

        if(type==1){

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
                }
            }
        }else if(type ==2){

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
                }
            }
        }
        */
    }

    double[] testNeighbour(double[] prev){
        int count = 0;
        double[] new_point = new double[2];
        double row_d = prev[0];
        double col_d = prev[1];
        int row = (int)row_d;
        int col = (int)col_d;

        if(image.get(row-1,col-1)[0] == 255 && (prev[0]!=row-1) && (prev[1]!=col-1) ){
            count++;
            new_point[0]=row-1;
            new_point[1]=col-1;
        }else if(image.get(row-1,col)[0] == 255 && (prev[0]!=row-1) && (prev[1]!=col) ){
            count++;
            new_point[0]=row-1;
            new_point[1]=col;
        }else if(image.get(row-1,col+1)[0] == 255 && (prev[0]!=row-1) && (prev[1]!=col+1) ){
            count++;
            new_point[0]=row-1;
            new_point[1]=col+1;
        }else if(image.get(row,col-1)[0] == 255 && (prev[0]!=row) && (prev[1]!=col-1) ){
            count++;
            new_point[0]=row;
            new_point[1]=col-1;
        }else if(image.get(row,col+1)[0] == 255 && (prev[0]!=row) && (prev[1]!=col+1) ){
            count++;
            new_point[0]=row;
            new_point[1]=col+1;
        }else if(image.get(row+1,col-1)[0] == 255 && (prev[0]!=row+1) && (prev[1]!=col-1) ){
            count++;
            new_point[0]=row+1;
            new_point[1]=col-1;
        }else if(image.get(row+1,col)[0] == 255 && (prev[0]!=row+1) && (prev[1]!=col) ){
            count++;
            new_point[0]=row+1;
            new_point[1]=col;
        }else if(image.get(row+1,col+1)[0] == 255 && (prev[0]!=row+1) && (prev[1]!=col+1) ){
            count++;
            new_point[0]=row+1;
            new_point[1]=col+11;
        }

        if(count == 1){
            return new_point;
        }else{
            new_point[0]=-1;
            new_point[1]=-1;
            return new_point;
        }
    }



}
