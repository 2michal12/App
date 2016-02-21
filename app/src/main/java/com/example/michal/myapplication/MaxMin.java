package com.example.michal.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

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


}
