package com.example.michal.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;

import org.opencv.android.OpenCVLoader;


public class MainActivity extends AppCompatActivity {

    private static Button mSelectImage;
    private static Toolbar toolbar;

    static{
        if(!OpenCVLoader.initDebug()){
            Log.i("opencv","opencv failed");
        }else{
            Log.i("opencv","opencv initialized");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);
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

        switch (id){
            case R.id.home :
                break;
            case R.id.load_image :
                Intent i = new Intent(this, LoadImage.class);
                startActivity(i);
                break;
            case R.id.preprocessing :
                break;
            case R.id.extraction:
                break;
            case R.id.information:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

}
