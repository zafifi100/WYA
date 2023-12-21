package com.example.wya;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.SensorEventListener;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Dictionary;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import static android.app.Activity.RESULT_OK;

public class VideoFragment extends Fragment {

    private static int sent = 0;
    private static int taken = 0;
    private static final int PICK_VIDEO_REQUEST = 1;
    public static String imageEncoded = "";
//    private Uri videoUri; // To store the selected video URI

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


        while(!isAdded()){
            ;
        }
        cameraAct = (CameraActivity) getActivity();
        if(cameraAct != null){
            System.out.println("\n\n\n\n\n\n\n CAMERA ACT NOT NULL \n\n\n\n\n\n\n");
        }
        else{
            System.out.println("\n\n\n\n\n\n\n CAMERA ACT IS NULL \n\n\n\n\n\n\n");
        }
//        sensor = cameraAct.customSensorEventListener;
//        sensor = cameraAct;


        Button uploadButton = view.findViewById(R.id.uploadButton);
        Button takeVideo = view.findViewById(R.id.takeVideo);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(taken == 1){
//                    saveVideoToInternalStorage(TakeVideo.uri);
                    saveVideoToInternalStorage(uri);
                    sent = 1;
                    taken = 0;
//                    new APICallTask(true).execute("https://98e4-72-33-2-141.ngrok-free.app/predict");
                    new APICallTask(true).execute("https://95d8-72-33-0-123.ngrok-free.app/predict");
                    return;
                }
                if (sent == 1) {
//                    new APICallTask(false).execute("https://98e4-72-33-2-141.ngrok-free.app/check");
                    new APICallTask(false).execute("https://95d8-72-33-0-123.ngrok-free.app/check");

//                    JSONObject jsonObject;
//                    try {
//                        jsonObject = new JSONObject(imageEncoded);
//                    } catch (JSONException e) {
//                        throw new RuntimeException(e);
//                    }

//                    String imageEncoded;
//                    try {
//                        imageEncoded = jsonObject.getString("image_encoded");
//                    } catch (JSONException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    // Convert the base64 string to a bitmap
//                    Bitmap bitmap = decodeBase64ToBitmap(imageEncoded);

//                    getView().setBackground(new BitmapDrawable(getResources(), bitmap));

//                    ImageView imageView = getView().findViewById(R.id.imageView);
//                    imageView.setRotation(-90.0f);
//                    imageView.setImageResource(R.drawable.arrow);
//                    imageView.setVisibility(View.VISIBLE);

                    Button uploadButton = getView().findViewById(R.id.uploadButton);
                    uploadButton.setVisibility(View.GONE);
                }
            }
        });

        takeVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(getActivity(), TakeVideo.class);
                Intent intent = new Intent(cameraAct, TakeVideo.class);


                // add threading to record yaw
                isRecording = true;
                Thread recordingThread = new Thread(recordingTask);
                recordingThread.start();
                activityResultLauncher.launch(intent);
//                startActivity(intent);


                Button uploadButton = getView().findViewById(R.id.uploadButton);
                uploadButton.setText("Check for image!");
                Button takeVideo = getView().findViewById(R.id.takeVideo);
                takeVideo.setVisibility(View.GONE);
                taken = 1;
            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    private void saveVideoToInternalStorage(Uri localVideoUri) {
        try {
//            InputStream inputStream = getActivity().getContentResolver().openInputStream(localVideoUri);
            InputStream inputStream = cameraAct.getContentResolver().openInputStream(localVideoUri);

            if (inputStream != null) {
                // Get the original filename from the content resolver
                String fileName = getFileNameFromUri(localVideoUri);

                // Save the video to internal storage with its original filename
//                FileOutputStream outputStream = getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
                FileOutputStream outputStream = cameraAct.openFileOutput(fileName, Context.MODE_PRIVATE);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();

                // Get the file path of the saved video
//                File file = new File(getActivity().getFilesDir(), fileName);
                File file = new File(cameraAct.getFilesDir(), fileName);
                String filePath = file.getAbsolutePath();

                // Log success or display a Toast message
                Log.d("VideoFragment", "Video saved successfully: " + filePath);
//                Toast.makeText(getActivity(), "Video saved successfully", Toast.LENGTH_SHORT).show();
                Toast.makeText(cameraAct, "Video saved successfully", Toast.LENGTH_SHORT).show();


                Button uploadButton = getView().findViewById(R.id.uploadButton);
                if (uploadButton.getText().toString().equals("Check for image!")) {
//                    new APICallTask(false).execute("https://98e4-72-33-2-141.ngrok-free.app/check");
                    new APICallTask(false).execute("https://95d8-72-33-0-123.ngrok-free.app/check");
                }
                else {
                    sent = 1;
//                    new APICallTask(true).execute("https://98e4-72-33-2-141.ngrok-free.app/predict");
                    new APICallTask(true).execute("https://95d8-72-33-0-123.ngrok-free.app/predict");
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (imageEncoded != null && imageEncoded.length() > 0) {
                                JSONObject jsonObject = new JSONObject(imageEncoded);
                                String imageEncoded = jsonObject.getString("image_encoded");

                                // Convert the base64 string to a bitmap
//                                Bitmap bitmap = decodeBase64ToBitmap(imageEncoded);

//                                getView().setBackground(new BitmapDrawable(getResources(), bitmap));

                                ImageView imageView = getView().findViewById(R.id.imageView);
                                //imageView.setRotation(90.0f);
                                imageView.setImageResource(R.drawable.arrow);
                                imageView.setVisibility(View.VISIBLE);

                                Button uploadButton = getView().findViewById(R.id.uploadButton);
                                uploadButton.setVisibility(View.GONE);

                            }
                            else {
                                System.out.println("JSON string is empty or null.");
                                if(sent == 1) {
                                    Button uploadButton = getView().findViewById(R.id.uploadButton);
                                    uploadButton.setText("Check for image!");
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            } else {
                // Log an error or display an error Toast message
                Log.e("VideoFragment", "Error: InputStream is null");
//                Toast.makeText(getActivity(), "Error saving video", Toast.LENGTH_SHORT).show();
                Toast.makeText(cameraAct, "Error saving video", Toast.LENGTH_SHORT).show();

            }
        } catch (IOException e) {
            e.printStackTrace();

            // Log an error or display an error Toast message
            Log.e("VideoFragment", "Error saving video: " + e.getMessage());
//            Toast.makeText(getActivity(), "Error saving video", Toast.LENGTH_SHORT).show();
            Toast.makeText(cameraAct, "Error saving video", Toast.LENGTH_SHORT).show();
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
//            if (getActivity() != null && isAdded()) {
            if (cameraAct != null && isAdded()) {
                    String[] projection = {MediaStore.Images.Media.DISPLAY_NAME};
//                cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
                cursor = cameraAct.getContentResolver().query(uri, projection, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                    fileName = cursor.getString(index);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return fileName;
    }

    private class APICallTask extends AsyncTask<String, Void, String> {

        private final boolean send;

        public APICallTask(boolean send) {
            this.send = send;
        }
        @Override
        protected String doInBackground(String... params) {
            String apiUrl = params[0];

            try {
                if (!send) {
//                    CameraActivity camAct = (CameraActivity) getActivity();
                    // Append the username as a query parameter for GET requests
//                    apiUrl += "?username=" + URLEncoder.encode(camAct.userName, "UTF-8");

                    apiUrl += "?username=" + URLEncoder.encode(cameraAct.userName, "UTF-8");
                }
                // Create the connection
                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                if (send) {
                    // If send is true, configure the connection for a POST request
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + "*****");

                    // Create the output stream for writing the request body
                    OutputStream outputStream = urlConnection.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));


                    // Append the username as a part of the form data
                    writer.write("--" + "*****" + "\r\n");
                    writer.write("Content-Disposition: form-data; name=\"username\"\r\n\r\n");
//                    CameraActivity camAct = (CameraActivity) getActivity();
//                    writer.write(camAct.userName);
                    writer.write(cameraAct.userName);
                    writer.write("\r\n");

                    // Append ArrayList<Integer> as JSON or plain text
                    writer.write("--" + "*****" + "\r\n");
                    writer.write("Content-Disposition: form-data; name=\"integerList\"\r\n\r\n");
                    writer.write(arrayListToJSONString(yawList));
                    writer.write("\r\n");

                    // Append video file data to the request body
                    // appendVideoFile(writer, outputStream, videoUri);
//                    appendVideoFile(writer, outputStream, TakeVideo.uri);
                    appendVideoFile(writer, outputStream, VideoFragment.uri);

                    // Finish the request
                    writer.write("--" + "*****" + "--\r\n");
                    writer.flush();
                    writer.close();
                } else {
                    // If send is false, configure the connection for a GET request
                    urlConnection.setRequestMethod("GET");
                }

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

        private void appendVideoFile(BufferedWriter writer, OutputStream outputStream, Uri localVideoUri) throws IOException {
            String BOUNDARY = "*****";

            // Get the filename from the URI
            String fileName = getFileNameFromUri(localVideoUri);

            // Start the file part
            writer.write("--" + BOUNDARY + "\r\n");
            writer.write("Content-Disposition: form-data; name=\"video\"; filename=\"" + fileName + "\"\r\n");
            writer.write("Content-Type: video/mp4\r\n");
            writer.write("\r\n");
            writer.flush();

            // Open the video file using the URI
//            if (cameraAct != null && isAdded()) {
//                InputStream inputStream = getActivity().getContentResolver().openInputStream(localVideoUri);
            InputStream inputStream = cameraAct.getContentResolver().openInputStream(localVideoUri);

                // Write the video file content to the request body
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
//            outputStream.flush();
                inputStream.close();
//            }
//            else{
//                System.out.println("\n\n\n\n\n\n\n SOMEHOW APPEND VIDEO ERROR \n\n\n\n\n\n\n");
//            }
            outputStream.flush();


            // End the file part
            writer.write("\r\n");
//            writer.flush();
        }
        @Override
        protected void onPostExecute(String result) {
            // Handle the API response here
            if (result != null) {
                Log.d("API_RESPONSE", result);
//                VideoFragment.imageEncoded = result;

                try {
                    JSONObject jsonObject = new JSONObject(result);

                    String status = jsonObject.getString("status");
                    if(!status.equals("done")){
                        return;
                    }

                    // Use the value as needed
                    yawToFace = jsonObject.getInt("yaw");
                    cameraAct.arrow = true;
                    cameraAct.yawToFace = yawToFace;

//                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    FragmentManager fragmentManager = cameraAct.getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.remove(VideoFragment.this); // 'this' refers to the current Fragment
                    fragmentTransaction.commit();
//                    updateArrowDirection();

                } catch (JSONException e) {
                    e.printStackTrace();
                    // Handle JSON parsing error
                }

            } else {
                Log.e("API_RESPONSE", "Error in API response");
            }
        }
    }


    public ArrayList<Integer> yawList = new ArrayList<>();
    public static Uri uri;
    int yawToFace = 0;
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        // Handle the data returned from TakeVideo
                        uri = data.getData();
                        Toast.makeText(getActivity(), "Received Video", Toast.LENGTH_SHORT).show();
                    }
                    if( data == null){
                        Toast.makeText(getActivity(), "Video NULL", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private String arrayListToJSONString(ArrayList<Integer> arrayList) {
        Gson gson = new Gson();
        return gson.toJson(arrayList);
    }

    boolean isRecording = false;
    CameraActivity cameraAct; // = (CameraActivity) getActivity();
//    CustomSensorEventListener sensor; // = cameraAct.customSensorEventListener;
//    SensorEventListener sensor;

    private final Runnable recordingTask = new Runnable() {
        @Override
        public void run() {
            float yaw;

            while (isRecording && yawList.size() < 18) {
                yaw = cameraAct.getAngles()[2];

                // then take 18 photos every 20 degrees, store in arrays
                if(yaw >= -180 && yaw < -160 && !yawList.contains(-170)){
                    yawList.add(-170);
                }
                else if(yaw >= -160 && yaw < -140 && !yawList.contains(-150)){
                    yawList.add(-150);
                }
                else if(yaw >= -140 && yaw < -120 && !yawList.contains(-130)){
                    yawList.add(-130);
                }
                else if(yaw >= -120 && yaw < -100 && !yawList.contains(-110)){
                    yawList.add(-110);
                }
                else if(yaw >= -100 && yaw < -80 && !yawList.contains(-90)){
                    yawList.add(-90);
                }
                else if(yaw >= -80 && yaw < -60 && !yawList.contains(-70)){
                    yawList.add(-70);
                }
                else if(yaw >= -60 && yaw < -40 && !yawList.contains(-50)){
                    yawList.add(-50);
                }
                else if(yaw >= -40 && yaw < -20 && !yawList.contains(-30)){
                    yawList.add(-30);
                }
                else if(yaw >= -20 && yaw < 0 && !yawList.contains(-10)){
                    yawList.add(-10);
                }
                else if(yaw >= 0 && yaw < 20 && !yawList.contains(10)){
                    yawList.add(10);
                }
                else if(yaw >= 20 && yaw < 40 && !yawList.contains(30)){
                    yawList.add(30);
                }
                else if(yaw >= 40 && yaw < 60 && !yawList.contains(50)){
                    yawList.add(50);
                }
                else if(yaw >= 60 && yaw < 80 && !yawList.contains(70)){
                    yawList.add(70);
                }
                else if(yaw >= 80 && yaw < 100 && !yawList.contains(90)){
                    yawList.add(90);
                }
                else if(yaw >= 100 && yaw < 120 && !yawList.contains(110)){
                    yawList.add(110);
                }
                else if(yaw >= 120 && yaw < 140 && !yawList.contains(130)){
                    yawList.add(130);
                }
                else if(yaw >= 140 && yaw < 160 && !yawList.contains(150)){
                    yawList.add(150);
                }
                else if(yaw >= 160 && yaw <= 180 && !yawList.contains(170)){
                    yawList.add(170);
                }

                if(yawList.size() == 2){
                    if(yawList.get(0) == 170 && (yawList.get(1) == -170 || yawList.get(1) == -150)){
                        yawList.clear();
                        fillArray(170, 18, true);
                        isRecording = false;
                        return;
                    }
                    else if (yawList.get(0) == -170 && (yawList.get(1) == 170 || yawList.get(1) == 150)) {
                        yawList.clear();
                        fillArray(-170, 18, false);
                        isRecording = false;
                        return;
                    }
                    else if (yawList.get(0) > yawList.get(1)){
                        int start = yawList.get(0);
                        yawList.clear();
                        fillArray(start, 18, false);
                        isRecording = false;
                        return;
                    }
                    else if (yawList.get(0) < yawList.get(1)){
                        int start = yawList.get(0);
                        yawList.clear();
                        fillArray(start, 18, true);
                        isRecording = false;
                        return;
                    }
                }

//        }
                // iterate through every image and sort them based on yaw
                // index 0 = -170, 1 = -150, 2 = -130, etc increments of 20degrees
                // now each person has array of pics where indexes and yaws align
//        while(images.size() != 0){
//            int index = (yawList.remove(yawList.size()-1) + 170) / 20;
//            imagesOrdered[index] = images.remove(images.size()-1);
//        }
                // pass ordered image array to server for comparison


                // Sleep for a short duration to prevent tight looping
                try {
                    Thread.sleep(30); // Adjust the time as needed
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

    };
    public void fillArray(int startValue, int size, boolean increment) {
        int currentValue = startValue;

        for (int i = 0; i < size; i++) {
            yawList.add(currentValue);

            // Increment or decrement by 20
            if (increment) {
                currentValue += 20;
            } else {
                currentValue -= 20;
            }

            // Wrap around if out of bounds
            if (currentValue > 180) {
                currentValue = -180 + (currentValue - 180);
            } else if (currentValue < -180) {
                currentValue = 180 - (-180 - currentValue);
            }
        }
    }

//    public void updateArrowDirection() {
////        float currentYaw = (float)(Math.toDegrees(orientationAngles[0]) + 360) % 360;
//        float currentYaw = sensor.getAngles()[2];
//        float rotation = currentYaw - yawToFace;
//
//        // Offset by 270 degrees (or -90 degrees) to account for the arrow's rightward orientation
//        float adjustedRotation = (rotation + 270) % 360;
//
//        ImageView arrowImageView = getView().findViewById(R.id.arrowImageView);
//        arrowImageView.setRotation(adjustedRotation);
//    }

}