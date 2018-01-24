package com.peelson.camerashot4peelson;

import android.content.ContentValues;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";
    private int FLAG = 1;

    private Mat mRgba;
    private Mat mIntermediateMat;
    private Mat mGray;

    private CameraBridgeViewBase mCameraShotView;


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("opencv_java");
        System.loadLibrary("opencv_java3");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraShotView = findViewById(R.id.jcv_shot);
        mCameraShotView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mCameraShotView.setCvCameraViewListener(this);
        mCameraShotView.setClickable(true);
        mCameraShotView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Camera camera = ((JavaCameraView) mCameraShotView).getCamera();
                if (camera != null) camera.autoFocus(null);
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraShotView != null)
            mCameraShotView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mCameraShotView.enableView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraShotView != null)
            mCameraShotView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        if (FLAG == 1) {
            Mat mBgr = new Mat();
            FLAG++;
            final long currentTimeMillis = System.currentTimeMillis();
            final String appName = getString(R.string.app_name);
            final String galleryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            final String albumPath = galleryPath + File.separator + appName;
            final String photoPath = albumPath + File.separator + currentTimeMillis + ".jpg";
            final ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DATA, photoPath);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
            values.put(MediaStore.Images.Media.TITLE, appName);
            values.put(MediaStore.Images.Media.DESCRIPTION, appName);
            values.put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMillis);

            // Ensure that the album directory exists.
            File album = new File(albumPath);
            if (!album.isDirectory() && !album.mkdirs()) {
                Log.e(TAG, "Failed to create album directory at " + albumPath);
            }

            // Try to create the photo.
            Imgproc.cvtColor(mRgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
            if (!Imgcodecs.imwrite(photoPath, mBgr)) {
                Log.e(TAG, "Failed to save photo to " + photoPath);

            }
            Log.d(TAG, "Photo saved successfully to " + photoPath);
            // Try to insert the photo into the MediaStore.
            Uri uri;
            try {
                uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } catch (final Exception e) {
                Log.e(TAG, "Failed to insert photo into MediaStore");
                e.printStackTrace();

                // Since the insertion failed, delete the photo.
                File photo = new File(photoPath);
                if (!photo.delete()) {
                    Log.e(TAG, "Failed to delete non-inserted photo");
                }
            }
            try {
                ExifInterface exif = new ExifInterface(photoPath);
                exif.setAttribute(ExifInterface.TAG_MAKE, "ShotByPeelson");
                exif.setAttribute(ExifInterface.TAG_MODEL, "ShotByPeelson");
                exif.saveAttributes();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        nativeProcessFrame(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
        return mRgba;
    }

    public native void nativeProcessFrame(long matAddrGr, long matAddrRgba);
}
