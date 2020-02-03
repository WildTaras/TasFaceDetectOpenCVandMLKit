package com.example.taskfacedetectopencvandmlkit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

public class FaceDetector implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static FaceDetector instance;
    private CameraBridgeViewBase cameraView;
    private OrientationDataProvider orientationDataProvider;
    private BaseLoaderCallback baseLoaderCallback;
    private static final String TAG = "FACEDETECTOR";

    private int height;
    private int width;
    private int rotationForMetadata;

    private Mat mRgba;
    private Rect rect;
    private int mFPS = 0;
    private long allTime = 0;
    private long timeSlot = 1000;
    private boolean flag = false;
    private TimeCounter time;

    private FaceDetector(Context context, JavaCameraView view) {
        cameraView = view;
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
        baseLoaderCallback = new BaseLoaderCallback(context) {
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
        time = new TimeCounter();
        orientationDataProvider = OrientationDataProvider.getInstance(context, HardwareOrientationConfig.NONGYROSCOPE);
    }

    public void initOpenCVDebug(Context context) {
        if(!OpenCVLoader.initDebug()){
            Log.e(TAG, "Smth wrong");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, context, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    public BaseLoaderCallback getBaseLoaderCallback() {
        return baseLoaderCallback;
    }

    public void setVisibility() {
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
    }



    public static FaceDetector getInstance(Context context, JavaCameraView view) {
        if(instance == null) {
            instance = new FaceDetector(context, view);
        }
        return instance;
    }

    public boolean isCameraViewEnabled() {
        return cameraView != null;
    }

    public void cameraViewDisabled() {
        cameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        time.startTime();
        final int upscaleParam = 4;
        FirebaseVisionFaceDetectorOptions options =
                new FirebaseVisionFaceDetectorOptions.Builder()
                        .build();

        float orientationZ = orientationDataProvider.axisZ();

        if((orientationZ > 45) && (orientationZ < 135)) {
 //           Log.e(TAG, "Right rotate");
            rotationForMetadata = 2;
        } else if((orientationZ < -45) && (orientationZ > -135)) {
//            Log.e(TAG, "Left rotate");
            rotationForMetadata = 0;
        } else if((orientationZ < 45) && (orientationZ > -45) ) {
 //           Log.e(TAG, "Portrait");
            rotationForMetadata = 3;
        } else {
            rotationForMetadata = 1;
        }

        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setWidth(width/upscaleParam)
                .setHeight(height/upscaleParam)
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setRotation(rotationForMetadata)
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
            if(rotationForMetadata == 3 ){
                Imgproc.rectangle(mRgba, new Point(rect.left + offsetX, rect.top - offsetX),
                        new Point(rect.right + offsetX, rect.bottom - offsetY ), new Scalar(255, 120, 120), 2);
            } else if( rotationForMetadata == 0){
                Imgproc.rectangle(mRgba,
                        new Point( rect.top + offsetY , width - offsetX - rect.left ),
                        new Point( rect.bottom + offsetX + offsetY, width - offsetX - rect.right),// calculate offset without
                        new Scalar(255, 120, 120), 2);
            } else if ( rotationForMetadata == 2){
                Imgproc.rectangle(mRgba,
                        new Point( width - offsetX - rect.top ,  rect.left - offsetX ),
                        new Point( height + offsetY - rect.bottom ,  rect.right - offsetX),
                        new Scalar(255, 120, 120), 2);

            } else {
                Imgproc.rectangle(mRgba,
                        new Point( 640 - rect.left - offsetX, 480 - rect.top + offsetX),
                        new Point( 640 - rect.right - offsetX, 480 - rect.bottom + offsetY),
                        new Scalar(255, 120, 120), 2);

            }
            flag = false;
        }
        return mRgba;
    }
}
