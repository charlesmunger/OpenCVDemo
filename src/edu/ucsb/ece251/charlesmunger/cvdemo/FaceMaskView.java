package edu.ucsb.ece251.charlesmunger.cvdemo;

import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.util.AttributeSet;

public class FaceMaskView extends JavaCameraView implements CvCameraViewListener {
    private static final Scalar    GLASSES_COLOR     = new Scalar(0, 255, 0, 255); //green
    private Mat                    mRgba;
    private Mat                    mGray;
    private CascadeClassifier      mJavaDetector;

    private float mRelativeFaceSize   = 0.2f;
    private int mAbsoluteFaceSize   = 0;
	
	public FaceMaskView(Context c, AttributeSet a) {
		super(c,a);
		super.setCvCameraViewListener(this);
		super.mCameraIndex = getFrontCameraId();
	}
	
	@Override
	public void onCameraViewStarted(int width, int height) {
		mGray = new Mat();
        mRgba = new Mat();
	}

	@Override
	public void onCameraViewStopped() {
		mGray.release();
        mRgba.release();
	}

	@Override
	public Mat onCameraFrame(Mat inputFrame) {
		inputFrame.copyTo(mRgba);
        Imgproc.cvtColor(inputFrame, mGray, Imgproc.COLOR_RGBA2GRAY);

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null) mJavaDetector.detectMultiScale(
        		mGray, faces, 1.1, 2, 2, new Size(
        				mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
        	Rect p = facesArray[i];
        	Point leftEye = new Point(p.tl().x+p.width/4, p.tl().y+p.height/4);
        	Point rightEye = new Point(leftEye.x+p.width/2, leftEye.y);
        	Core.circle(mRgba, leftEye, p.width/4, GLASSES_COLOR,4);
        	Core.circle(mRgba, rightEye, p.width/4, GLASSES_COLOR,4);
//            Core.rectangle(mRgba, p.tl(), p.br(), GLASSES_COLOR, 3);
        }
        if(facesArray.length < 1) {
        	Core.putText(mRgba, getContext().getString(R.string.no_face_found), new Point(mRgba.width()/2, mRgba.height()/2), Core.FONT_HERSHEY_PLAIN, 3, GLASSES_COLOR);
        }
        return mRgba;
	}
	
	private static int getFrontCameraId() {
	    CameraInfo ci = new CameraInfo();
	    for (int i = 0 ; i < Camera.getNumberOfCameras(); i++) {
	        Camera.getCameraInfo(i, ci);
	        if (ci.facing == CameraInfo.CAMERA_FACING_FRONT) return i;
	    }
	    return -1; // No front-facing camera found
	}

	public void setDetector(CascadeClassifier mJavaDetector) {
		this.mJavaDetector = mJavaDetector;		
	}
	
	public Mat getBuffer() {
		return this.mRgba;
	}
}
