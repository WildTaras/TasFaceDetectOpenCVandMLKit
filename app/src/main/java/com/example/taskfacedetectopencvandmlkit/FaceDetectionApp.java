package com.example.taskfacedetectopencvandmlkit;

import android.app.Application;
import android.content.Context;

public class FaceDetectionApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        OrientationDataProvider.init(this, HardwareOrientationConfig.NONGYROSCOPE);
    }

}
