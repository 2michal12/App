package com.example.michal.myapplication.ftrScan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.michal.myapplication.R;

import java.io.File;

public class SelectFileFormatActivity extends Activity {

	private Button mButtonOK;
	private RadioGroup mRadioGroup;
	private RadioButton mRadioBitmap;
	private RadioButton mRadioWSQ;
	private EditText mEditFileName;
	private TextView mMessage;

    private static File mDir;
	private String mFileFormat = "BITMAP";
	private String mFileName;
    // Return Intent extra
    public static String EXTRA_FILE_FORMAT = "file_format";
    private Toolbar toolbar;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ftr_activity_save);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.futronic_title);

        mButtonOK = (Button) findViewById(R.id.btnSaveScan);
        //mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
        //mRadioBitmap = (RadioButton) findViewById(R.id.radioBitmap);
        //mRadioWSQ = (RadioButton) findViewById(R.id.radioWSQ);
        mEditFileName = (EditText) findViewById(R.id.editFileName);
        //mMessage = (TextView) findViewById(R.id.textMessage);
        
        setResult(Activity.RESULT_CANCELED);

        /*mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
        	public void onCheckedChanged(RadioGroup group, int checkedId) {          	 
        		if(checkedId==mRadioBitmap.getId())
        			mFileFormat = "BITMAP";
        		else if(checkedId==mRadioWSQ.getId())
        			mFileFormat = "WSQ";
        	}
        });*/
        
        mButtonOK.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	mFileName = mEditFileName.getText().toString();
        		if( mFileName.trim().isEmpty() )
        		{
            		ShowAlertDialog();
            		return;
            	}
            	if( !isImageFolder() )
            		return;
            	
            	if(mFileFormat.compareTo("BITMAP") == 0 )
            		mFileName = mFileName + ".bmp";
            	else 
            		mFileName = mFileName + ".wsq";
            	
            	CheckFileName();
            }
        });
    }
    
    private void ShowAlertDialog()
    {
        new AlertDialog.Builder(this) 
        .setTitle("File name") 
        .setMessage("File name can not be empty!") 
        .setPositiveButton("OK", new DialogInterface.OnClickListener() { 
             public void onClick(DialogInterface dialog, int whichButton) { 
             } 
        })
		.setCancelable(false)
        .show();
    }
    
    private void SetFileName()
    {    	
    	String[] extraString = new String[2];
    	extraString[0] = mFileFormat;
    	extraString[1] = mDir.getAbsolutePath() + "/"+ mFileName;
        Intent intent = new Intent();
        intent.putExtra(EXTRA_FILE_FORMAT, extraString);
        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
    
    private void CheckFileName()
    {    	
    	File f = new File(mDir, mFileName);
    	if( f.exists() )
    	{
            new AlertDialog.Builder(this) 
            .setTitle("File name") 
            .setMessage("File already exists. Do you want replace it?") 
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() { 
                 public void onClick(DialogInterface dialog, int whichButton) { 
                	 SetFileName();              	
                 } 
            }) 
            .setNegativeButton("No", new DialogInterface.OnClickListener() { 
                 public void onClick(DialogInterface dialog, int whichButton) {
                	 //mMessage.setText("Cancel");
                 } 
            })
			.setCancelable(false)
            .show();
        }
    	else
    		SetFileName();
    }
    
    public boolean isImageFolder()
    {
        File extStorageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES); //public directory used default
        mDir = new File(extStorageDirectory, "DIPLOMOVKA/FtrScan");

         if( mDir.exists() )
        {
            if( !mDir.isDirectory() )
            {
            	//mMessage.setText( "Can not create image folder " + mDir.getAbsolutePath() + ". File with the same name already exist." );
            	return false;
            }
        } else {
            try
            {
            	mDir.mkdirs();
            }
            catch( SecurityException e )
            {
            	//mMessage.setText( "Can not create image folder " + mDir.getAbsolutePath() + ". Access denied.");
            	return false;
            }
        }
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
}
