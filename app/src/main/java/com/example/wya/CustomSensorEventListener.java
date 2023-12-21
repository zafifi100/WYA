// CustomSensorEventListener.java

package com.example.wya;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class CustomSensorEventListener implements SensorEventListener {

    private muse_plus my_muse_plus;
    private float[] linearAccelerationValues;
    private float[] euler_angles;
    private int stepCount;
    private boolean setReset;

    public CustomSensorEventListener(muse_plus myMusePlus) {
        this.my_muse_plus = myMusePlus;
        this.linearAccelerationValues = new float[3];
        this.euler_angles = new float[]{0, 0, 0};
        this.stepCount = 0;
        this.setReset = false;
    }

    public float[] getAngles() {
        return this.euler_angles;
    }

    public int getStepCount() {
        return this.stepCount;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // If there is a change in Linear Acceleration Sensor
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            // Fetching the sensor values
            float[] accelerationValues = sensorEvent.values;

            // Getting the acceleration along XYZ axes
            float accelerationX = accelerationValues[0];
            float accelerationY = accelerationValues[1];
            float accelerationZ = accelerationValues[2];

            // Apply a low-pass filter to the acceleration values to remove noise
            // Using a simple exponential smoothing with alpha = 0.2 for demonstration
            float alpha = 0.2f;
            linearAccelerationValues  = new float[3];
            linearAccelerationValues[0] = alpha * linearAccelerationValues[0] + (1 - alpha) * accelerationX;
            linearAccelerationValues[1] = alpha * linearAccelerationValues[1] + (1 - alpha) * accelerationY;
            linearAccelerationValues[2] = alpha * linearAccelerationValues[2] + (1 - alpha) * accelerationZ;

            // Calculate the magnitude of the low-pass filtered acceleration
            float accelerationMagnitude = (float) Math.sqrt(
                    linearAccelerationValues[0] * linearAccelerationValues[0] +
                            linearAccelerationValues[1] * linearAccelerationValues[1] +
                            linearAccelerationValues[2] * linearAccelerationValues[2]);

            // Determine whether the threshold is crossed or not
            float threshold = 10.0f;  // Adjust the threshold as needed
            boolean thresholdCrossed = accelerationMagnitude > threshold;

            // If the acceleration crosses the threshold, increment the step count
            if (thresholdCrossed) {
                stepCount++;
            }

            // If the reset button is pressed, reset the step count
            if (setReset) {
                stepCount = 0;
                setReset = false; // Reset the reset button flag
            }

        }

        //If there is a change in Accelerometer values
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //Fetching the accelerometer values
            linearAccelerationValues = sensorEvent.values;
            my_muse_plus.update_acc(sensorEvent.values);
        }

        //If there is a change in Magnometer values
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //Fetching the accelerometer values
            my_muse_plus.update_mag(sensorEvent.values, sensorEvent.timestamp);
        }

        //If there is a change in Gyroscope values
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            //Fetching the gyroscope values
            my_muse_plus.update_gyro(sensorEvent.values, sensorEvent.timestamp);
            //compute euler angles
            float[] tmp = my_muse_plus.get_EulerAngles();
            euler_angles = (tmp == null) ? euler_angles : tmp;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
