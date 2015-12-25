package com.example.michal.myapplication.ftrScan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.michal.myapplication.Help;
import com.example.michal.myapplication.R;
import com.futronictech.Scanner;
import com.futronictech.UsbDeviceDataExchangeImpl;
import com.futronictech.ftrWsqAndroidHelper;

import java.io.File;
import java.io.FileOutputStream;

public class FtrScanDemoUsbHostActivity extends Activity {
    /** Called when the activity is first created. */
	private static Button mButtonScan;
	private static Button mButtonStop;
	private Button mButtonSave;
	private static ImageView mFingerImage;

    public static boolean mStop = false;
    
    public static final int MESSAGE_SHOW_MSG = 1;
    public static final int MESSAGE_SHOW_SCANNER_INFO = 2;
    public static final int MESSAGE_SHOW_IMAGE = 3;
    public static final int MESSAGE_ERROR = 4;
    public static final int MESSAGE_TRACE = 5;

    public static byte[] mImageFP = null;  
	public static Object mSyncObj= new Object();	
	
    public static int mImageWidth = 0;
    public static int mImageHeight = 0;
	private static int[] mPixels = null;
    private static Bitmap mBitmapFP = null;
	private static Canvas mCanvas = null;
    private static Paint mPaint = null;
	
    private FPScan mFPScan = null;   
    //
    public static boolean mUsbHostMode = true;

    // Intent request codes
    private static final int REQUEST_FILE_FORMAT = 1;
    private UsbDeviceDataExchangeImpl usb_host_ctx = null;

	private static Toolbar toolbar;

	public static void InitFingerPictureParameters(int wight, int height)
    {
    	 mImageWidth = wight;
	     mImageHeight = height;
         
         mImageFP = new byte[FtrScanDemoUsbHostActivity.mImageWidth*FtrScanDemoUsbHostActivity.mImageHeight];
	     mPixels = new int[FtrScanDemoUsbHostActivity.mImageWidth*FtrScanDemoUsbHostActivity.mImageHeight];
	  	     	             
	     mBitmapFP = Bitmap.createBitmap(wight, height, Config.RGB_565); 
	     
	     mCanvas = new Canvas(mBitmapFP); 
	     mPaint = new Paint(); 
	        
	     ColorMatrix cm = new ColorMatrix(); 
	     cm.setSaturation(0); 
	     ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm); 
	     mPaint.setColorFilter(f);
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ftr_activity_main);

		toolbar = (Toolbar) findViewById(R.id.toolbar);
		toolbar.setTitle(R.string.futronic_title);

        mUsbHostMode = true;
    	mButtonScan = (Button) findViewById(R.id.btnScan);
        mButtonStop = (Button) findViewById(R.id.btnStop);
        mButtonSave = (Button) findViewById(R.id.btnSave);
        mFingerImage = (ImageView) findViewById(R.id.imageFinger);

        usb_host_ctx = new UsbDeviceDataExchangeImpl(this, mHandler);

        mButtonScan.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mFPScan != null) {
					mStop = true;
					mFPScan.stop();
				}

				mStop = false;
				if (mUsbHostMode) {
					usb_host_ctx.CloseDevice();
					if (usb_host_ctx.OpenDevice(0, true)) {
						if (StartScan()) {
							mButtonScan.setEnabled(false);
							mButtonSave.setEnabled(false);
							mButtonStop.setEnabled(true);
						}
					}else{
						Toast.makeText(getApplicationContext(), R.string.scanner_unplag, Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
        
        mButtonStop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mStop = true;
				if (mFPScan != null) {
					mFPScan.stop();
					mFPScan = null;

				}
				mButtonScan.setEnabled(true);
				mButtonSave.setEnabled(true);
				mButtonStop.setEnabled(false);
			}
		});
        
        mButtonSave.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mImageFP != null)
					SaveImage();
			}
		});

    }
    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		mStop = true;	       
		if( mFPScan != null )
		{
			mFPScan.stop();
			mFPScan = null;
		}
		usb_host_ctx.CloseDevice();
		usb_host_ctx.Destroy();
		usb_host_ctx = null;
    }

	private boolean StartScan()
    {
		mFPScan = new FPScan(usb_host_ctx, mHandler);
		mFPScan.start();		
		return true;
    }
    
    private void SaveImage()
    {
	    Intent serverIntent = new Intent(this, SelectFileFormatActivity.class);
	    startActivityForResult(serverIntent, REQUEST_FILE_FORMAT);
    }

	private void SaveImageByFileFormat(String fileFormat, String fileName)
	{
		if( fileFormat.compareTo("WSQ") == 0 )	//save wsq file
		{
			Scanner devScan = new Scanner();
			boolean bRet;
			if( mUsbHostMode )
				bRet = devScan.OpenDeviceOnInterfaceUsbHost(usb_host_ctx);
			else
				bRet = devScan.OpenDevice();
			if( !bRet )
			{
				return;
			}
			byte[] wsqImg = new byte[mImageWidth*mImageHeight];
			long hDevice = devScan.GetDeviceHandle();
			ftrWsqAndroidHelper wsqHelper = new ftrWsqAndroidHelper();
			if( wsqHelper.ConvertRawToWsq(hDevice, mImageWidth, mImageHeight, 2.25f, mImageFP, wsqImg) )
			{
				File file = new File(fileName);
				try {
					FileOutputStream out = new FileOutputStream(file);
					out.write(wsqImg, 0, wsqHelper.mWSQ_size);	// save the wsq_size bytes data to file
					out.close();
					//mMessage.setText("Image is saved as " + fileName);
				} catch (Exception e) {
					//mMessage.setText("Exception in saving file");
				}
			}
			else
				//mMessage.setText("Failed to convert the image!");
			if( mUsbHostMode )
				devScan.CloseDeviceUsbHost();
			else
				devScan.CloseDevice();
			return;
		}
		// 0 - save bitmap file
		File file = new File(fileName);
		try {
			FileOutputStream out = new FileOutputStream(file);
			//mBitmapFP.compress(Bitmap.CompressFormat.PNG, 90, out);
			MyBitmapFile fileBMP = new MyBitmapFile(mImageWidth, mImageHeight, mImageFP);
			out.write(fileBMP.toBytes());
			out.close();
			//mMessage.setText("Image is saved as " + fileName);
		} catch (Exception e) {
			//mMessage.setText("Exception in saving file");
		}

		// Tell the media scanner about the new file so that it is immediately available to the user.
		MediaScannerConnection.scanFile(this, new String[]{file.toString()}, null,
				new MediaScannerConnection.OnScanCompletedListener() {
					public void onScanCompleted(String path, Uri uri) {
						Log.i("ExternalStorage", "Scanned " + path + ":");
						Log.i("ExternalStorage", "-> uri=" + uri);
					}
				});

		Toast.makeText(getApplicationContext(), R.string.image_saved, Toast.LENGTH_SHORT).show();
	}
    
    // The Handler that gets information back from the FPScan
	private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_SHOW_MSG:            	
            	String showMsg = (String) msg.obj;
                //mMessage.setText(showMsg);
                break;
            case MESSAGE_SHOW_SCANNER_INFO:            	
            	String showInfo = (String) msg.obj;
                //mScannerInfo.setText(showInfo);
                break;
            case MESSAGE_SHOW_IMAGE:
            	ShowBitmap();
                break;              
            case MESSAGE_ERROR:
           		//mFPScan = null;
            	mButtonScan.setEnabled(true);
            	mButtonStop.setEnabled(false);
            	break;
            case UsbDeviceDataExchangeImpl.MESSAGE_ALLOW_DEVICE:
            	if(usb_host_ctx.ValidateContext())
            	{
            		if( StartScan() )
	        		{
	        			mButtonScan.setEnabled(false);
	        	        mButtonSave.setEnabled(false);
	        	        mButtonStop.setEnabled(true);
	        		}	
            	}
            	else
            		//mMessage.setText("Can't open scanner device");
            	break;           
	        case UsbDeviceDataExchangeImpl.MESSAGE_DENY_DEVICE:
            	//mMessage.setText("User deny scanner device");
            	break;
            }
        }
    };
    
    private static void ShowBitmap()
    {
    	for( int i=0; i<mImageWidth * mImageHeight; i++)
    	{
    		mPixels[i] = Color.rgb(mImageFP[i],mImageFP[i],mImageFP[i]);
    	}
     	                  
        mCanvas.drawBitmap(mPixels, 0, mImageWidth, 0, 0,  mImageWidth, mImageHeight, false, mPaint);
        
        mFingerImage.setImageBitmap(mBitmapFP);
        mFingerImage.invalidate();
        
        synchronized (mSyncObj) 
        {
         	mSyncObj.notifyAll();
        }
    }        

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
         case REQUEST_FILE_FORMAT:
			 if (resultCode == Activity.RESULT_OK) {
			     // Get the file format
				 String[] extraString = data.getExtras().getStringArray(SelectFileFormatActivity.EXTRA_FILE_FORMAT);
				 String fileFormat = extraString[0];
				 String fileName = extraString[1];
				 SaveImageByFileFormat(fileFormat, fileName);
             }
				 //mMessage.setText("Cancelled!");
             break;            
        }
    }

}