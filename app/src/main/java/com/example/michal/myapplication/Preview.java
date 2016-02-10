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

import butterknife.Bind;
import butterknife.ButterKnife;

public class Preview extends AppCompatActivity {

    private static Help help;
    private static Bitmap mImage;
    private static Dialog dialog;

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.view_loaded_image) ImageView mLoadedImage;
    @Bind(R.id.preprocessing_automatic) Button mPreprocessingAutomatic;
    @Bind(R.id.preprocessing_manual) Button mPreprocessingManual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        if( getSupportActionBar() != null )
            getSupportActionBar().setTitle(R.string.preview);

        help = new Help(this);
        dialog = new Dialog(this);

        byte[] byteArray = getIntent().getByteArrayExtra(help.BITMAP_IMAGE);
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
                    startPreprocessing(mImage, help.MANUAL);
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
        i.putExtra(help.BITMAP_IMAGE, byteArray);
        i.putExtra(help.TYPE, type);
        startActivity(i);
    }

    public void settingsDialog(){
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.popup_settings);
        dialog.setTitle(R.string.settings);

        Button dialogButton = (Button) dialog.findViewById(R.id.popUpOK);
        TextView mSettingTitleText = (TextView) dialog.findViewById(R.id.popUpSettingTextTitle);
        TextView mEdittextTitle = (TextView) dialog.findViewById(R.id.textForEdittext);
        EditText mEditText = (EditText) dialog.findViewById(R.id.settingsEdittext);
        final RadioButton radioButton1 = (RadioButton) dialog.findViewById(R.id.radioButton1);
        final RadioButton radioButton2 = (RadioButton) dialog.findViewById(R.id.radioButton2);
        final RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.radioButtonGroup);

        mSettingTitleText.setVisibility(View.GONE);
        mEdittextTitle.setVisibility(View.GONE);
        mEditText.setVisibility(View.GONE);
        radioGroup.setVisibility(View.VISIBLE);

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( radioButton1.isChecked() ) {
                    startPreprocessing(mImage, help.AUTOMATIC);
                }else if( radioButton2.isChecked() ){
                    startPreprocessing(mImage, help.AUTOMATIC_FULL);
                }else{

                }
            }
        });

        dialog.show();
    }

}