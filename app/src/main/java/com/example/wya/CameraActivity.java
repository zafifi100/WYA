package com.example.wya;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class CameraActivity extends AppCompatActivity implements SensorEventListener {

    public CustomSensorEventListener customSensorEventListener;
    private SensorManager sensorManager;
    private muse_plus my_muse_plus;
    private Sensor mAccelerator;
    private Sensor mAmeter;
    private Sensor mGyro;
    private Sensor mMag;
//    private LocationManager locationManager;
    private TextView textViewEuler;
    private float yaw;

    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private static final int UPDATE_INTERVAL = 1000;

    private static final int REQUEST_CODE_PERMISSIONS = 100;

    String userName;
    public boolean arrow;
    public float yawToFace;


    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            updateTextViewEuler();
            updateHandler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        my_muse_plus = new muse_plus();

        // Creating a sensorManager Object
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerator = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mAmeter = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // Initialize TextView
        textViewEuler = findViewById(R.id.eulerAnglesTextView);

        // Initialize CustomSensorEventListener
//        customSensorEventListener = new CustomSensorEventListener(my_muse_plus);


        userName = getIntent().getStringExtra("user");
        System.out.println("\n\n\nUsername is " + userName + "\n\n\n");

        ActivityCompat.requestPermissions(this, new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSIONS);
        // need to add permission for camera /////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        euler_angles = new float[]{0,0,0};
        linearAccelerationValues = new float[3];


        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        //MapsFragment mapFragment = new MapsFragment();
        VideoFragment videoFragment = new VideoFragment();

        //fragmentTransaction.replace(R.id.map, mapFragment);
        fragmentTransaction.replace(R.id.video, videoFragment);
        fragmentTransaction.commit();
    }

    private void updateTextViewEuler() {
//        float[] euler_angles = customSensorEventListener.getAngles();
//        yaw = customSensorEventListener.getAngles()[2];
//        String eulerText = String.format("Yaw: %.2f, Pitch: %.2f, Roll: %.2f", customSensorEventListener.getAngles()[2], euler_angles[1], euler_angles[0]);

        String eulerText = String.format("Yaw: %.2f, Pitch: %.2f, Roll: %.2f", euler_angles[2], euler_angles[1], euler_angles[0]);

        textViewEuler.setText(eulerText);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Registering Handler
//        sensorManager.registerListener(customSensorEventListener, mMag, SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(customSensorEventListener, mAccelerator, SensorManager.SENSOR_DELAY_GAME);
//        sensorManager.registerListener(customSensorEventListener, mAmeter, SensorManager.SENSOR_DELAY_FASTEST);
//        sensorManager.registerListener(customSensorEventListener, mGyro, SensorManager.SENSOR_DELAY_GAME);


        sensorManager.registerListener(this, mMag, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, mAccelerator, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, mAmeter, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_GAME);

        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
//        sensorManager.unregisterListener(customSensorEventListener);


        sensorManager.unregisterListener(this);

        updateHandler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

//    @Override
//    public void onSensorChanged(SensorEvent event) {}

    public float[] euler_angles; // = new float[]{0,0,0};
    public float[] linearAccelerationValues; // = new float[3];
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
        }

        //If there is a change in Accelerometer values
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //Fetching the accelerometer values
//            linearAccelerationValues = sensorEvent.values;
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

        // if updating arrow true, call update method
        if(arrow){
            updateArrowDirection();
        }

    }
    public float[] getAngles() {
        return this.euler_angles;
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can access the location here
//                fineLocationAccess = 1;
            } else {
                // Permission denied, handle the case where the user denies location access
            }

            if (grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can access the location here
//                coarseLocationAccess = 1;
            } else {
                // Permission denied, handle the case where the user denies location access
            }

            if (grantResults.length > 2 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can access the location here
            } else {
                // Permission denied, handle the case where the user denies location access
            }

            if (grantResults.length > 3 && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can access the location here
            } else {
                // Permission denied, handle the case where the user denies location access
            }
        }
    }


    private void updateArrowDirection() {
//        float currentYaw = (float)(Math.toDegrees(orientationAngles[0]) + 360) % 360;
//        float currentYaw = customSensorEventListener.getAngles()[2];

        float currentYaw = euler_angles[2];
        float rotation = currentYaw - yawToFace;

        // Offset by 270 degrees (or -90 degrees) to account for the arrow's rightward orientation
        float adjustedRotation = (rotation + 270) % 360;

        ImageView arrowImageView = findViewById(R.id.arrowImageView);
        arrowImageView.setRotation(adjustedRotation);
    }

}
