package com.example.taskfacedetectopencvandmlkit;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class NonGyroscopeSource implements IOrientationDataSource {

    private Sensor sensor;
    private SensorManager sensorManager;
    private SensorEventListener eventListener;

    private final float[] rotationMatrix;
    private final float[] remappedRotationMatrix;
    private final float[] orientations;

    private int axes = 3;
    private int matrixElement = 9;
    private int X = 0;
    private int Y = 1;
    private int Z = 2;

    public NonGyroscopeSource(Context context) {
        rotationMatrix = new float[matrixElement];
        remappedRotationMatrix = new float[matrixElement];
        orientations = new float[axes];
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        eventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedRotationMatrix);
                SensorManager.getOrientation(remappedRotationMatrix, orientations);


                for(int i = 0; i < 3; i++) {
                    orientations[i] = (float)(Math.toDegrees(orientations[i]));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        sensorManager.registerListener(eventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public float axisX() {
        return orientations[X];
    }

    @Override
    public float axisY() {
        return orientations[Y];
    }

    @Override
    public float axisZ() {
        return orientations[Z];
    }
}
