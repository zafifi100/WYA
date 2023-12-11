package com.example.wya;
import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class CameraActivity extends AppCompatActivity implements SensorEventListener {

    private CustomSensorEventListener customSensorEventListener;
    private SensorManager sensorManager;
    private muse_plus my_muse_plus;
    private Sensor mAccelerator;
    private Sensor mAmeter;
    private Sensor mGyro;
    private Sensor mMag;
    private LocationManager locationManager;
    private TextView textViewEuler;
    private float yaw;

    private final Handler updateHandler = new Handler(Looper.getMainLooper());
    private static final int UPDATE_INTERVAL = 1000;

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

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
        customSensorEventListener = new CustomSensorEventListener(my_muse_plus);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        //MapsFragment mapFragment = new MapsFragment();
        VideoFragment videoFragment = new VideoFragment();

        //fragmentTransaction.replace(R.id.map, mapFragment);
        fragmentTransaction.replace(R.id.video, videoFragment);
        fragmentTransaction.commit();
    }

    private void updateTextViewEuler() {
        float[] euler_angles = customSensorEventListener.getAngles();
        yaw = euler_angles[2];
        String eulerText = String.format("Yaw: %.2f, Pitch: %.2f, Roll: %.2f", euler_angles[2], euler_angles[1], euler_angles[0]);
        textViewEuler.setText(eulerText);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Registering Handler
        sensorManager.registerListener(customSensorEventListener, mMag, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(customSensorEventListener, mAccelerator, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(customSensorEventListener, mAmeter, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(customSensorEventListener, mGyro, SensorManager.SENSOR_DELAY_GAME);

        updateHandler.postDelayed(updateRunnable, UPDATE_INTERVAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(customSensorEventListener);

        updateHandler.removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {}
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
