package com.example.taskfacedetectopencvandmlkit;

import android.Manifest;
import android.app.Application;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
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

public class MainActivity extends AppCompatActivity{

    private static final String TAG = "APP";

    private CameraBridgeViewBase cameraView;
    FaceDetector faceDetector;
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    cameraView.setCameraPermissionGranted();
                    cameraView.SetCaptureFormat(NV21); // set
                    cameraView.setMaxFrameSize(640, 480 );
                    //cameraView.setCameraIndex(99);
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
        ActivityCompat.requestPermissions(
                this, new String[]{Manifest.permission.CAMERA},1 );

        faceDetector = new FaceDetector();
        cameraView = findViewById(R.id.jcvCamera);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(faceDetector);

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "In OnStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e(TAG, "In OnRestart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "In OnResume");
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
        Log.e(TAG, "In onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "In onDestroy");
    }

}
