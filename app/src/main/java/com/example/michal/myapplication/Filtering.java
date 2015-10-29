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
import android.widget.Button;
import android.widget.ImageView;

public class Filtering extends AppCompatActivity {
    private static Toolbar toolbar;
    private static Bitmap imageAftefFiltering;
    private static ImageView mFilteringImage;
    private static Button mNextProcess;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtering);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.filtering);
        setSupportActionBar(toolbar);

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        final Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        imageAftefFiltering = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);

        mFilteringImage = (ImageView) findViewById(R.id.view_filtering_image);
        mFilteringImage.setImageBitmap(image);

        mNextProcess = (Button) findViewById(R.id.next);
        //mNextProcess.setEnabled(true);

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

        switch (id){
            case R.id.home :
                i = new Intent(this, MainActivity.class);
                startActivity(i);
                break;
            case R.id.information:
                System.out.println("informacie");
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
