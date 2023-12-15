package com.example.wya;

import androidx.appcompat.app.AppCompatActivity;

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

public class TakeVideo extends AppCompatActivity {

    public static Uri uri;
    private ActivityResultLauncher<Intent> videoCaptureLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_video);

        ActivityCompat.requestPermissions(this, new String[]
                {Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);

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
}