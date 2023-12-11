package com.example.wya;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;

import static android.app.Activity.RESULT_OK;

public class VideoFragment extends Fragment {

    private static final int PICK_VIDEO_REQUEST = 1;
    private Uri videoUri; // To store the selected video URI

    public VideoFragment() {
        // Required empty public constructor
    }

    public static VideoFragment newInstance() {
        return new VideoFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);

        Button uploadButton = view.findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    // Open the gallery to pick a video
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    // Handle the result from the gallery
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            videoUri = data.getData();
            // Now you can save the videoUri and process it as needed
            saveVideoToInternalStorage(videoUri);
            Toast.makeText(getActivity(), "Video selected: " + videoUri.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    // Save the video to internal storage
    private void saveVideoToInternalStorage(Uri videoUri) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(videoUri);

            if (inputStream != null) {
                // Specify the filename for the saved video
                String fileName = "saved_video.mp4";

                // Save the video to internal storage
                FileOutputStream outputStream = getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();

                // Log success or display a Toast message
                Log.d("VideoFragment", "Video saved successfully: " + fileName);
                Toast.makeText(getActivity(), "Video saved successfully", Toast.LENGTH_SHORT).show();
                // loop over first 10 frames
                for(int i = 0; i < 10; i++){

                }
                // Make an API call after video is saved
                new APICallTask().execute("https://73e4-2600-6c98-a500-71e-7123-c9-1c41-7b9c.ngrok-free.app/predict");
            } else {
                // Log an error or display an error Toast message
                Log.e("VideoFragment", "Error: InputStream is null");
                Toast.makeText(getActivity(), "Error saving video", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();

            // Log an error or display an error Toast message
            Log.e("VideoFragment", "Error saving video: " + e.getMessage());
            Toast.makeText(getActivity(), "Error saving video", Toast.LENGTH_SHORT).show();
        }
    }

    private class APICallTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String apiUrl = params[0];

            try {
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                try {
                    InputStream in = urlConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }

                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("API_CALL_ERROR", "Error making API call", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // Handle the API response here
            if (result != null) {
                Log.d("API_RESPONSE", result);
                // Log or process the API response as needed
            } else {
                Log.e("API_RESPONSE", "Error in API response");
            }
        }
    }
}
