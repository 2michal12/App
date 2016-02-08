package com.example.michal.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by Michal on 01/02/16.
 */
public class MaxMin extends AppCompatActivity {
    Bitmap imageBitmap = null, maxminBitmap;

    @Bind(R.id.text)
    TextView text;

    @Bind(R.id.image)
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maxmin);
        ButterKnife.bind(this);


        System.out.println("max min");
        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        if (byteArray != null) {

            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            maxminBitmap = Bitmap.createBitmap(imageBitmap.getWidth(), imageBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        }

        text.setText(imageBitmap.getHeight() + " " + imageBitmap.getWidth());
        image.setImageBitmap(imageBitmap);

    }
/*
    void gaborFilter(Mat src_gray, Mat dest_gray, double** smerova_mapa_gauss, double** frekvencna_mapa ,int velkost_gabora, double sigmaa, double lambdaa, double gammaa, double psii){
        Point anchor;
        double delta, sucet = 0.0;
        int ddepth, u = 0, v = 0, count = 0;
        int kernel_size=0;
        Mat kernel;
        /// Initialize arguments for the filter
        anchor = Point(-1, -1);
        delta = 0;
        ddepth = -1;
        ///Vytvorenie Gaborovho filtra
        double sigma = sigmaa, // to je ta odchylka - urcuje silu filtra - skus ju menit a uvidis
                lambd = lambdaa,  // toto znamena ze rozostup linii je cca 10 pixelov a to je pravda
                gamma = gammaa,  // to je rozmerovy faktor je to pomer dlzka_filtra/sirka_filtra, teraz je viac dlhy ako siroky lebo je 0.25
                psi = psii; // to je nejake vysunutie filtra v bloku , nechaj ako je
        CvSize dim = cvSize(velkost_gabora, velkost_gabora);

        for (int i = (velkost_gabora - 1) / 2; i<src_gray.rows - (velkost_gabora - 1) / 2; i++){ //prechadzam vsetky body okrem okrajovich aby sa dala pocitat okolo okrajovich matica
            for (int j = (velkost_gabora - 1) / 2; j<src_gray.cols - (velkost_gabora - 1) / 2; j++){

                if (smerova_mapa_gauss[i][j]>M_PI_2){
                    smerova_mapa_gauss[i][j] -= M_PI_2;
                }
                else{
                    smerova_mapa_gauss[i][j] += M_PI_2;
                }
                kernel = getGaborKernel(dim, sigma, smerova_mapa_gauss[i][j], lambdaa, gamma, psi, CV_64F); //spomalujem vypocet

                for (int k = i - (velkost_gabora - 1) / 2; k<i + velkost_gabora - (velkost_gabora - 1) / 2; k++){ //ked mam vypocitany kernel pre dany bod tak urobim okolo neho blok o velkosti gaborovho filtra
                    for (int l = j - (velkost_gabora - 1) / 2; l<j + velkost_gabora - (velkost_gabora - 1) / 2; l++){
                        sucet = sucet + (src_gray.at<uchar>(k, l) / 255)*kernel.at<double>(u, v); //spomalujem vypocet tiez
                        v++;
                    }
                    v = 0;
                    u++;
                }
                u = 0;
                dest_gray.at<uchar>(i, j) = sucet; //ulozim sucet do centralneho bodu
                //cout << sucet<<endl;
                sucet = 0.0;
            }
            progressBar(i - (velkost_gabora - 1)/2+1, src_gray.rows - (velkost_gabora - 1), 50);
        }

    }
*/

}
