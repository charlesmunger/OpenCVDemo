package edu.ucsb.ece251.charlesmunger.cvdemo;

import java.io.File;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import edu.ucsb.ece251.charlesmunger.cvdemo.util.SystemUiHider;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class FaceMirrorActivity extends RoboActivity {
	private static final String TAG = "OCVDemo::Activity";
	/**
	 * The number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 5000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;
	@InjectView(R.id.fullscreen_content) private FaceMaskView fullscreenContent;

	protected CascadeClassifier mJavaDetector;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_face_mirror);
		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, fullscreenContent,HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (visible && true) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		fullscreenContent.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu m) {
		super.onCreateOptionsMenu(m);
		getMenuInflater().inflate(R.menu.menu_face_mirror, m);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem m) {
		super.onOptionsItemSelected(m);
		switch(m.getItemId()) {
			case R.id.menu_save: return saveImage();
		}
		return false;
	}

	private boolean saveImage() {
		Mat mat = fullscreenContent.getBuffer();
		Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
		Utils.matToBitmap(mat, bmp);
		mat.release();
		String uri = MediaStore.Images.Media.insertImage(getContentResolver(), bmp,
				"A gorgeous self portrait " + DateFormat.getDateFormat(getBaseContext()), "not used");
		Intent intent = new Intent(Camera.ACTION_NEW_PICTURE);
		intent.setData(new Uri.Builder().path(uri).build());
		sendBroadcast(intent);
		Toast.makeText(this, R.string.picture_saved, Toast.LENGTH_SHORT).show();
		return true;
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(1000);
	}

	@Override
    public void onPause() {
        if (fullscreenContent != null)
            fullscreenContent.disableView();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, new BaseLoaderCallback(this) {
        	@Override
            public void onManagerConnected(int status) {
        		if(status == LoaderCallbackInterface.SUCCESS) {
        			Log.i(TAG, "OpenCV loaded successfully");
                    try {
                    	String path = Utils.exportResource(FaceMirrorActivity.this, R.raw.lbpcascade_frontalface,"cascade");
                        File cascadeDir = getDir("cascade",Context.MODE_PRIVATE);
                        CascadeClassifier mJavaDetector = new CascadeClassifier(path);
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else {
                            Log.i(TAG, "Loaded cascade classifier from " + path);
                            fullscreenContent.setDetector(mJavaDetector);
                        }
                        cascadeDir.delete();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    fullscreenContent.enableView();
        		} else {
        			super.onManagerConnected(status);
        		}
            }
        });
    }
	
	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			delayedHide(AUTO_HIDE_DELAY_MILLIS);
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}
}
