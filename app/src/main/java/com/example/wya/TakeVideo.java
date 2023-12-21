package com.example.wya;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.lang.*;
import java.util.ArrayList;

public class TakeVideo extends AppCompatActivity {

    private CustomSensorEventListener custSEL;


//    public static Uri uri;
    public Uri uri;

    private ActivityResultLauncher<Intent> videoCaptureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_video);

//        ActivityCompat.requestPermissions(this, new String[]
//                {Manifest.permission.ACCESS_FINE_LOCATION,
//                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);

        videoCaptureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri videoUri = data.getData();
                            uri = videoUri;

                            System.out.println(videoUri);
                            // Handle the captured video
                            Toast.makeText(this, "Video Saved", Toast.LENGTH_SHORT).show();


                            // return uri to VideoFragment
                            Intent returnIntent = new Intent();
                            returnIntent.setData(uri);

                            // not returning yaw because recording it in video frag
//                            returnIntent.putIntegerArrayListExtra("yaws", yawList);

                            setResult(RESULT_OK, returnIntent);

                            finish();
                        }
                    }
                }
        );

        dispatchTakeVideoIntent( findViewById(android.R.id.content));
    }

    public void dispatchTakeVideoIntent(View v) {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        Uri videoUri = createVideoFileUri(); // Method to create a file URI
        if (videoUri != null) {
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);


//            isRecording = true;
//            Thread recordingThread = new Thread(recordingTask);
//            recordingThread.start();


            videoCaptureLauncher.launch(takeVideoIntent);
        } else {
            // Handle error
        }
    }

    private Uri createVideoFileUri() {
        // Get the external files directory
        File videoDirectory = getExternalFilesDir(Environment.DIRECTORY_MOVIES);

        String videoFileName = "VIDEO_test1_";
        // Create a File object for the video
        File videoFile;
        try {
            videoFile = File.createTempFile(
                    videoFileName,
                    ".mp4",
                    videoDirectory
            );
        } catch (IOException ex) {
            return null;
        }
        return FileProvider.getUriForFile(this, "com.example.wya.fileprovider", videoFile);
    }


// threading for yaw that was removed

//    private Runnable recordingTask = new Runnable() {
//        @Override
//        public void run() {
//            while (isRecording && yawList.size() < 18) {
//                // then take 18 photos every 20 degrees, store in arrays
//                if(yaw >= -180 && yaw < -160 && !yawList.contains(-170)){
//                    yawList.add(-170);
//                }
//                else if(yaw >= -160 && yaw < -140 && !yawList.contains(-150)){
//                    yawList.add(-150);
//                }
//                else if(yaw >= -140 && yaw < -120 && !yawList.contains(-130)){
//                    yawList.add(-130);
//                }
//                else if(yaw >= -120 && yaw < -100 && !yawList.contains(-110)){
//                    yawList.add(-110);
//                }
//                else if(yaw >= -100 && yaw < -80 && !yawList.contains(-90)){
//                    yawList.add(-90);
//                }
//                else if(yaw >= -80 && yaw < -60 && !yawList.contains(-70)){
//                    yawList.add(-70);
//                }
//                else if(yaw >= -60 && yaw < -40 && !yawList.contains(-50)){
//                    yawList.add(-50);
//                }
//                else if(yaw >= -40 && yaw < -20 && !yawList.contains(-30)){
//                    yawList.add(-30);
//                }
//                else if(yaw >= -20 && yaw < 0 && !yawList.contains(-10)){
//                    yawList.add(-10);
//                }
//                else if(yaw >= 0 && yaw < 20 && !yawList.contains(10)){
//                    yawList.add(10);
//                }
//                else if(yaw >= 20 && yaw < 40 && !yawList.contains(30)){
//                    yawList.add(30);
//                }
//                else if(yaw >= 40 && yaw < 60 && !yawList.contains(50)){
//                    yawList.add(50);
//                }
//                else if(yaw >= 60 && yaw < 80 && !yawList.contains(70)){
//                    yawList.add(70);
//                }
//                else if(yaw >= 80 && yaw < 100 && !yawList.contains(90)){
//                    yawList.add(90);
//                }
//                else if(yaw >= 100 && yaw < 120 && !yawList.contains(110)){
//                    yawList.add(110);
//                }
//                else if(yaw >= 120 && yaw < 140 && !yawList.contains(130)){
//                    yawList.add(130);
//                }
//                else if(yaw >= 140 && yaw < 160 && !yawList.contains(150)){
//                    yawList.add(150);
//                }
//                else if(yaw >= 160 && yaw <= 180 && !yawList.contains(170)){
//                    yawList.add(170);
//                }
//
//                if(yawList.size() == 2){
//                    if(yawList.get(0) == 170 && (yawList.get(1) == -170 || yawList.get(1) == -150)){
//                        yawList.clear();
//                        fillArray(170, 18, true);
//                        isRecording = false;
//                        return;
//                    }
//                    else if (yawList.get(0) == -170 && (yawList.get(1) == 170 || yawList.get(1) == 150)) {
//                        yawList.clear();
//                        fillArray(-170, 18, false);
//                        isRecording = false;
//                        return;
//                    }
//                    else if (yawList.get(0) > yawList.get(1)){
//                        int start = yawList.get(0);
//                        yawList.clear();
//                        fillArray(start, 18, false);
//                        isRecording = false;
//                        return;
//                    }
//                    else if (yawList.get(0) < yawList.get(1)){
//                        int start = yawList.get(0);
//                        yawList.clear();
//                        fillArray(start, 18, true);
//                        isRecording = false;
//                        return;
//                    }
//                }
//
////        }
//                // iterate through every image and sort them based on yaw
//                // index 0 = -170, 1 = -150, 2 = -130, etc increments of 20degrees
//                // now each person has array of pics where indexes and yaws align
////        while(images.size() != 0){
////            int index = (yawList.remove(yawList.size()-1) + 170) / 20;
////            imagesOrdered[index] = images.remove(images.size()-1);
////        }
//                // pass ordered image array to server for comparison
//
//
//                // Sleep for a short duration to prevent tight looping
//                try {
//                    Thread.sleep(30); // Adjust the time as needed
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    return;
//                }
//            }
//        }
//
//    };
//
//    public void fillArray(int startValue, int size, boolean increment) {
//        int currentValue = startValue;
//
//        for (int i = 0; i < size; i++) {
//            yawList.add(currentValue);
//
//            // Increment or decrement by 20
//            if (increment) {
//                currentValue += 20;
//            } else {
//                currentValue -= 20;
//            }
//
//            // Wrap around if out of bounds
//            if (currentValue > 180) {
//                currentValue = -180 + (currentValue - 180);
//            } else if (currentValue < -180) {
//                currentValue = 180 - (-180 - currentValue);
//            }
//        }
//    }


}