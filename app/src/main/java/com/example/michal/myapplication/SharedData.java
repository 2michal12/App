package com.example.michal.myapplication;

import android.app.Application;

/**
 * Created by Michal on 14/03/16.
 */
public class SharedData extends Application{
    public static double[][] orientation_map;

    public static double[][] getInstance() {
        return orientation_map;
    }

}
