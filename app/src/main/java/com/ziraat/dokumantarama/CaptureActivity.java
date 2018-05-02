package com.ziraat.dokumantarama;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.crashlytics.android.Crashlytics;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class CaptureActivity extends AppCompatActivity implements View.OnClickListener,SocketListener,SurfaceHolder.Callback {

    private final int MAX_HEIGHT = 1024;
    private final int MAX_WIDTH = 1024;
    private final int IMAGE_QUALITY = 100;

    ServerSocketThread socketThread;
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        initCamera();
        initSocket();


    }

    private void initCamera(){
        Dexter.withActivity(this)
                .withPermission(
                        Manifest.permission.CAMERA
                ).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse response) {
                surfaceHolder = surfaceView.getHolder();
                surfaceHolder.addCallback(CaptureActivity.this);
                surfaceHolder.setFormat(PixelFormat.RGB_565);
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse response) {

            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

            }
        }).check();
    }

    private void initSocket(){
        socketThread = new ServerSocketThread(this);
        socketThread.start();
    }

    @Override
    public void onClick(View v) {
//        captureAndSendImage();
        Crashlytics.getInstance().crash();
    }

    @Override
    public void sendSocketCommand(int commandType) {
        SocketActions action = SocketActions.fromKey(commandType);
        switch (action) {
            case CLOSE_APP:
                System.exit(1);
                break;
            case CAPTURE_PICTURE:
                captureAndSendImage();
                break;
        }
    }

    public void captureAndSendImage() {
        //take the picture
        try {
            camera.takePicture(null, null, jpegCallback);
        } catch (Exception e) {

        }
    }

    public void refreshCamera() {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
        }

        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (Exception e) {

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        refreshCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
        } catch (RuntimeException e) {
            System.err.println(e);
            return;
        }
        setCameraParameters(camera);
        try {

            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            camera.autoFocus(new Camera.AutoFocusCallback() {
                public void onAutoFocus(boolean success, Camera camera) {
                }
            });
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            return;
        }
    }

    public void setCameraParameters(Camera mCamera) {
        Camera.Parameters mParameters;
        mParameters = camera.getParameters();
        mParameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);

        mParameters = mCamera.getParameters();
        mParameters.set("orientation", "portrait");
        List<Camera.Size> supportedPreviewSizes = mParameters.getSupportedPreviewSizes();
        mParameters.setPreviewSize(supportedPreviewSizes.get(0).width, supportedPreviewSizes.get(0).height);
        List<Camera.Size> supportedPictureSizes = mParameters.getSupportedPictureSizes();

        int captureWidth = supportedPictureSizes.get(0).width;
        int captureHeight = supportedPictureSizes.get(0).height;

        mParameters.setPictureSize(captureWidth, captureHeight);


        List<String> focusModes = mParameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }


        List<?> whiteMode = mParameters.getSupportedWhiteBalance();
        if (whiteMode != null
                && whiteMode
                .contains(android.hardware.Camera.Parameters.WHITE_BALANCE_AUTO)) {
            mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }

        mCamera.setParameters(mParameters);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if(camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == 0) {
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");//this is your bitmap image and now you can do whatever you want with this
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            socketThread.sendPhoto(byteArray);
        }
    }

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            byte[] reduced = resizeImage(data);
            String encoded = Base64.encodeToString(reduced, Base64.DEFAULT);
            socketThread.sendPhoto(encoded);
            refreshCamera();
        }
    };

    public byte[] resizeImage(byte[] input) {
        Bitmap original = BitmapFactory.decodeByteArray(input, 0, input.length);
        Bitmap resized = scaleBitmap(original,MAX_WIDTH,MAX_HEIGHT);
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, blob);
        return blob.toByteArray();
    }

    private Bitmap scaleBitmap(Bitmap bm,int maxWidth,int maxHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();

        if (width > height) {
            // landscape
            float ratio = (float) width / maxWidth;
            width = maxWidth;
            height = (int)(height / ratio);
        } else if (height > width) {
            // portrait
            float ratio = (float) height / maxHeight;
            height = maxHeight;
            width = (int)(width / ratio);
        } else {
            // square
            height = maxHeight;
            width = maxWidth;
        }


        bm = Bitmap.createScaledBitmap(bm, width, height, true);
        return bm;
    }
}