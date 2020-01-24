package com.example.taskfacedetectopencvandmlkit;

import android.Manifest;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.TimingLogger;
import android.view.SurfaceView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.List;

import static android.graphics.ImageFormat.NV21;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static org.opencv.core.Core.rotate;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{

    CameraBridgeViewBase cameraView;

    private static final String TAG = "CAMERA";

    Mat mRgba;
    Rect rect;
    int mFPS = 0;
    long allTime = 0;
    long faceTime = 0;
    long timeSlot = 1000;
    TextView txt1;
    boolean flag = false;
    TimeCounter time;

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    cameraView.setCameraPermissionGranted();
                    cameraView.SetCaptureFormat(NV21); // set
                    cameraView.setMaxFrameSize(640, 480 );
                    cameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        txt1 = findViewById(R.id.txt1);
        time = new TimeCounter();
        cameraView = (JavaCameraView)findViewById(R.id.jcvCamera);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
        ActivityCompat.requestPermissions(
                this, new String[]{Manifest.permission.CAMERA},1 );
        Log.e(TAG, "Orientation " + this.getResources().getConfiguration().orientation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!OpenCVLoader.initDebug()){
            Log.e(TAG, "Smth wrong");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraView != null) {
            cameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        time.startTime();
        final int width = mRgba.width();
        int height = mRgba.height();
        final int upscaleParam = 4;
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .build();

        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setWidth(width/upscaleParam)
                .setHeight(height/upscaleParam)
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setRotation(FirebaseVisionImageMetadata.ROTATION_270)
                .build();

        final FirebaseVisionFaceDetector detector =
                FirebaseVision.getInstance().getVisionFaceDetector(options);

        Mat mRgbaF = new Mat();

        Imgproc.resize(inputFrame.gray(),mRgbaF, new Size(width/upscaleParam,
                height/upscaleParam),0,0,Imgproc.INTER_LINEAR);

        // rotate image
        Point center = new Point(mRgba.cols()>>1,mRgba.rows()>>1);
        Mat M = Imgproc.getRotationMatrix2D(center,90,1);
        Imgproc.warpAffine(mRgba, mRgba, M, new Size(width,height));


        byte[] byteArray = null;
        if (byteArray == null) {
            byteArray = new byte[mRgbaF.channels()*mRgbaF.cols()*mRgbaF.rows()];
        }
        mRgbaF.get(0, 0, byteArray);
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);

        FirebaseVisionImage image = FirebaseVisionImage.fromByteBuffer(buffer,metadata);

        Task<List<FirebaseVisionFace>> result = detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                @Override
                    public void onSuccess(List<FirebaseVisionFace> firebaseVisionFaces) {

                    if (firebaseVisionFaces.isEmpty()) {
                        time.endTime();
                        return;
                    }
                    rect = firebaseVisionFaces.get(0).getBoundingBox();
                    rect.bottom = rect.bottom*upscaleParam;
                    rect.top = rect.top*upscaleParam;
                    rect.left = rect.left*upscaleParam;
                    rect.right = rect.right*upscaleParam;
                    flag = true;

                    time.endTime();
                    allTime += time.getMillisecondCounter();
                    ++mFPS;
                    if(allTime >= timeSlot) {
                        txt1.setText("FPS: " + mFPS);
                        allTime = 0;
                        mFPS = 0;
                    }
                }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, ": " + time.getMillisecondDuration());
                        time.endTime();
                    }
                });
        if(flag != false) {
            int offsetX = (width - height) >> 1;
            int offsetY = offsetX >> 1;
            Imgproc.rectangle(mRgba, new Point(rect.left + offsetX, rect.top - offsetX),
                    new Point(rect.right + offsetX, rect.bottom - offsetY ), new Scalar(255, 120, 120), 2);
            flag = false;
        }
        return mRgba;
    }
}
