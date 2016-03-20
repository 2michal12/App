package com.example.michal.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Vector;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Michal on 01/02/16.
 */
public class MaxMin extends AppCompatActivity {
    Bitmap imageBitmap = null, imageAfter;
    Mat image,color_image,image_new;
    Help help;

    @Bind(R.id.text)
    TextView text;

    @Bind(R.id.image)
    ImageView imageView;

    @Bind(R.id.origimage)
    Button origimage;

    @Bind(R.id.newimage)
    Button newimage;

    @Bind(R.id.saveimage)
    Button saveimage;

    @Bind(R.id.contour)
    Button contour;

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

        help = new Help(this);
        image = help.bitmap2mat(imageBitmap);
        color_image = new Mat(image.rows(), image.cols(), CvType.CV_8UC3);
        image_new = new Mat(image.rows(), image.cols(), CvType.CV_8UC1);

        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);

        convertSkeleton(image);

        contour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contours(image);
                Utils.matToBitmap(image, imageAfter);
                imageView.setImageBitmap(imageAfter);
            }
        });

        /*
        extraction(image, 1); //1 = ukoncenia, 2 = rozdvojenia

        Utils.matToBitmap(image, imageAfter); //default value without click any button
        origimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.matToBitmap(image, imageAfter);
                imageView.setImageBitmap(imageAfter);
            }
        });

        newimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.matToBitmap(color_image, imageAfter);
                imageView.setImageBitmap(imageAfter);
            }
        });

        final Help h = new Help(this);
        saveimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        */

    }

    public void saveText(){
        String filename = "myfile";
        String string = "Hello world!";
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, this.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
            Toast.makeText(this, "The contents are saved in the file.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Exception: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public boolean convertSkeleton(Mat image){
        double[] black = new double[1];
        black[0] = 0;
        double[] white = new double[1];
        white[0] = 255;


        for(int i = 0; i < image.height(); i++){
            for(int j = 0; j < image.width(); j++){
                if(image.get(i, j)[0] > 100){
                    image.put(i, j, white);
                }else{
                    image.put(i, j, black);
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
        double[] black3 = new double[3];
        black3[0] = 0;
        black3[1] = 0;
        black3[2] = 0;
        double[] white3 = new double[3];
        white3[0] = 255;
        white3[1] = 255;
        white3[2] = 255;
        double[] black1 = new double[1];
        black1[0] = 0;
        double[] white1 = new double[1];
        white1[0] = 255;

        for(int i = 0; i < image.height(); i++){
            for(int j = 0; j < image.width(); j++){
                if(image.get(i, j)[0] > 100){
                    color_image.put(i, j, white3);
                    image_new.put(i, j, white1);
                }else{
                    color_image.put(i, j, black3);
                    image_new.put(i, j, black1);
                }

            }
        }

        int dlzka_fragmentu = 15;
        int[][] fragment = new int[2][dlzka_fragmentu];

        double[] white_pixel;
        double[] point = new double[2];
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
                    neighbour = testOneNeighbourOfPoint(i, j);
                    count = 0;
                    if (neighbour[2] == 1) {
                        fragment[0][0] = i;
                        fragment[1][0] = j;
                        image.put(i, j, black1);
                        count++;
                        //image_new.put(i, j, gray1);
                        for (int k = 0; k < dlzka_fragmentu - 1; k++) {
                            tempi = neighbour[0];
                            tempj = neighbour[1];
                            neighbour = testOneNeighbourOfPoint(neighbour[0], neighbour[1]);
                            if(neighbour[2] > 1){
                                erase = false;
                                break;
                            }else if (neighbour[2] != 1) {
                                if(k < dlzka_fragmentu-1){
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

    }

    int[] testOneNeighbourOfPoint(int row, int col){
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

    Mat contours(Mat image){
        List<MatOfPoint> contours = new Vector<MatOfPoint>();
        List<MatOfPoint> fragments = new Vector<MatOfPoint>();
        Mat hierarchy = new Mat();
        Mat neww = image.clone();

        Imgproc.findContours(neww, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE, new Point(0, 0));
        for (int i = 0; i < contours.size(); i++) {
            if( contours.get(i).size().height > 10 && contours.get(i).size().height < 100){
                fragments.add(contours.get(i));
                contours.remove(i);
                i--;
            }
        }

        Imgproc.drawContours(neww, contours, -1, new Scalar(255,255,255));
        Imgproc.drawContours(neww, fragments, -1, new Scalar(100,100,100));

        return neww;
    }

}
