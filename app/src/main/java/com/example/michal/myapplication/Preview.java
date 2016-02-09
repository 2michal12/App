package com.example.michal.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;

public class Preview extends AppCompatActivity {

    private static Help help;
    private static Toolbar toolbar;

    private static ImageView mLoadedImage;
    private static Bitmap mImage;
    private static Button mPreprocessingAutomatic;
    private static Button mPreprocessingManual;

    private static Button dialogButton;
    private static TextView mSettingTitleText;
    private static TextView mEdittextTitle;
    private static EditText mEditText;
    private static RadioButton radioButton1;
    private static RadioButton radioButton2;
    private static RadioGroup radioGroup;
    private static Dialog dialog;

    private static String AUTOMATIC = "automatic";
    private static String AUTOMATIC_FULL = "automatic_full";
    private static String MANUAL = "manual";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        help = new Help(this);
        dialog = new Dialog(this);

        mLoadedImage = (ImageView) findViewById(R.id.view_loaded_image);
        mPreprocessingAutomatic = (Button) findViewById(R.id.preprocessing_automatic);
        mPreprocessingManual = (Button) findViewById(R.id.preprocessing_manual);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        byte[] byteArray = getIntent().getByteArrayExtra("BitmapImage");
        if(byteArray != null) {

            mImage = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            mLoadedImage.setImageBitmap(mImage);

            mPreprocessingAutomatic.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    settingsDialog();
                }
            });
            mPreprocessingManual.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startPreprocessing(mImage, MANUAL);
                }
            });
        }else{
            mLoadedImage.setImageResource(R.drawable.ic_menu_report_image);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        dialog.cancel();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        menu.getItem(1).setVisible(false);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) { //due to finishAffinity(); supported from API 16
            menu.getItem(4).setVisible(false);  //exit app
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        help.menuItemOtherActivities(id, mImage, help.PREVIEW);
        return super.onOptionsItemSelected(item);
    }

    private void startPreprocessing(Bitmap image, String type) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        Intent i = new Intent(this, Segmentation.class);
        i.putExtra("BitmapImage", byteArray);
        i.putExtra("Type", type);
        startActivity(i);
    }

    public void settingsDialog(){
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_settings);
        dialog.setTitle(R.string.settings);

        dialogButton = (Button) dialog.findViewById(R.id.popUpOK);
        mSettingTitleText = (TextView) dialog.findViewById(R.id.popUpSettingTextTitle);
        mSettingTitleText.setVisibility(View.GONE);
        mEdittextTitle = (TextView) dialog.findViewById(R.id.text_for_edittext);
        mEdittextTitle.setVisibility(View.GONE);
        mEditText = (EditText) dialog.findViewById(R.id.settings_edittext);
        mEditText.setVisibility(View.GONE);
        radioButton1 = (RadioButton) dialog.findViewById(R.id.radioButton1);
        radioButton2 = (RadioButton) dialog.findViewById(R.id.radioButton2);
        radioGroup = (RadioGroup) dialog.findViewById(R.id.radioButtonGroup);
        radioGroup.setVisibility(View.VISIBLE);

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( radioButton1.isChecked() ) {
                    startPreprocessing(mImage, AUTOMATIC);
                }else if( radioButton2.isChecked() ){
                    startPreprocessing(mImage, AUTOMATIC_FULL);
                }else{

                }
            }
        });

        dialog.show();
    }

}
