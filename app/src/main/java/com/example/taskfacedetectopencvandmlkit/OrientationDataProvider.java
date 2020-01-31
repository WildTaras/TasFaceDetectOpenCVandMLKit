package com.example.taskfacedetectopencvandmlkit;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

public class OrientationDataProvider  {

    private static OrientationDataProvider instance;
    private IOrientationDataSource orientationDataSource;

    private OrientationDataProvider(Context context, HardwareOrientationConfig config) {
        if(config == HardwareOrientationConfig.GYROSCOPE){
            orientationDataSource = new GyroscopeSource(context);
        } else if (config == HardwareOrientationConfig.NONGYROSCOPE)
            orientationDataSource = new NonGyroscopeSource(context);
    }

    public static OrientationDataProvider getInstance(Context context, HardwareOrientationConfig config) {
        if(OrientationDataProvider.instance == null) {
            instance = new OrientationDataProvider(context, config);
        }
        return instance;
    }

    public float axisX() {
        return orientationDataSource.axisX();
    }

    public float axisY() {
        return orientationDataSource.axisY();
    }

    public float axisZ() {
        return orientationDataSource.axisZ();
    }


}
