package com.example.michal.myapplication;

import android.app.Application;
import org.opencv.core.Mat;

/**
 * Created by Michal on 14/03/16.
 */
public class SharedData extends Application{
    public static double[][] orientation_map;
    public static Mat original_image_bifurcations, original_image_endings;
    private static Mat original;

    public static double[][] getInstance() {
        return orientation_map;
    }

    public static void setImage(Mat image){
        original = image.clone();
    }

    public static void restoreImages(){
        original_image_bifurcations = original.clone();
        original_image_endings = original.clone();
    }

    public static Mat getImageBifurcation(){
        return original_image_bifurcations;
    }

    public static Mat getImageEndings(){
        return original_image_endings;
    }

}
