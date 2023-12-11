package com.example.wya;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import org.json.JSONObject;



import static android.app.Activity.RESULT_OK;

import com.google.common.net.MediaType;
import com.google.firebase.database.util.JsonMapper;

public class VideoFragment extends Fragment {

    private static final int PICK_VIDEO_REQUEST = 1;
    public static String imageEncoded = "";
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


    private void saveVideoToInternalStorage(Uri videoUri) {
        try {
            InputStream inputStream = getActivity().getContentResolver().openInputStream(videoUri);

            if (inputStream != null) {
                // Get the original filename from the content resolver
                String fileName = getFileNameFromUri(videoUri);

                // Save the video to internal storage with its original filename
                FileOutputStream outputStream = getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();

                // Get the file path of the saved video
                File file = new File(getActivity().getFilesDir(), fileName);
                String filePath = file.getAbsolutePath();

                // Log success or display a Toast message
                Log.d("VideoFragment", "Video saved successfully: " + filePath);
                Toast.makeText(getActivity(), "Video saved successfully", Toast.LENGTH_SHORT).show();

                new APICallTask().execute("https://73e4-2600-6c98-a500-71e-7123-c9-1c41-7b9c.ngrok-free.app/predict");

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (imageEncoded != null && imageEncoded.length() > 0) {

                                JSONObject jsonObject = new JSONObject(imageEncoded);

                                String imageEncoded = jsonObject.getString("image_encoded");

                                // Convert the base64 string to a bitmap
                                Bitmap bitmap = decodeBase64ToBitmap(imageEncoded);

                                // Set the bitmap as the background of the fragment's root view
                                getView().setBackground(new BitmapDrawable(getResources(), bitmap));

                            } else {
                                System.out.println("JSON string is empty or null.");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

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
    private Bitmap decodeBase64ToBitmap(String base64) {
        byte[] decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "unknown_file";
        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
            cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                fileName = cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fileName;
    }


    private class APICallTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String apiUrl = params[0];

            try {
                // Create the connection
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + "*****");

                // Create the output stream for writing the request body
                OutputStream outputStream = urlConnection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

                // Append video file data to the request body
                appendVideoFile(writer, outputStream);

                // Finish the request
                writer.write("--" + "*****" + "--");
                writer.flush();
                writer.close();

                // Get the response from the server
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response from the server
                    InputStream in = urlConnection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }

                    return stringBuilder.toString();
                } else {
                    Log.e("API_CALL_ERROR", "Error in API response. Response Code: " + responseCode);
                    return null;
                }
            } catch (IOException e) {
                Log.e("API_CALL_ERROR", "Error making API call", e);
                return null;
            }
        }

        // Helper method to append video file data to the request body
        private void appendVideoFile(BufferedWriter writer, OutputStream outputStream) throws IOException {
            // Specify the boundary
            String BOUNDARY = "*****";

            // Specify the filename for the video file
            String fileName = "saved_video.mp4";

            // Start the file part
            writer.write("--" + BOUNDARY + "\r\n");
            writer.write("Content-Disposition: form-data; name=\"video\"; filename=\"" + fileName + "\"\r\n");
            writer.write("Content-Type: video/mp4\r\n");
            writer.write("\r\n");
            writer.flush();

            // Write the video file content to the request body
            FileInputStream fileInputStream = new FileInputStream(new File(getActivity().getFilesDir(), fileName));
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            fileInputStream.close();

            // End the file part
            writer.write("\r\n");
            writer.flush();
        }

        @Override
        protected void onPostExecute(String result) {
            // Handle the API response here
            if (result != null) {
                Log.d("API_RESPONSE", result);
                VideoFragment.imageEncoded = result;

                // Log or process the API response as needed
            } else {
                Log.e("API_RESPONSE", "Error in API response");
            }
        }
    }

}
