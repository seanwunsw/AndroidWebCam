package com.android.example.secam;

import static android.content.ContentValues.TAG;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.*;


public class MainActivity extends AppCompatActivity {

    private byte[] latestJpegFrameData = null;
    private PreviewView mPreviewView;
    private TextView ipTV;
    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private CameraSelector mCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    private FloatingActionButton fabcam,fabst,fabsetting,fab;
    private boolean visable = false;
    private boolean started = false;
    private MjpegServer mjpegServer;
    private Camera camera;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get a reference to the PreviewView
        mPreviewView = findViewById(R.id.previewView);
        ipTV = findViewById(R.id.ipTV);
        String ip = "IP address:" + NetworkHelper.getIPAddress(true);
        ip+="\nPort:"+8080+" (Not finish yet~)";
        ipTV.setText(ip);
        ipTV.setTextSize(32);
        fabst = findViewById(R.id.fabst);
        fabcam = findViewById(R.id.fabcam);
        fabsetting = findViewById(R.id.fabsetting);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!visable){
                    fabcam.setVisibility(View.VISIBLE);
                    fabsetting.setVisibility(View.VISIBLE);
                    fabst.setVisibility(View.VISIBLE);
                    visable = true;
                }else{
                    fabcam.setVisibility(View.GONE);
                    fabsetting.setVisibility(View.GONE);
                    fabst.setVisibility(View.GONE);
                    visable = false;
                }
            }
        });

        fabcam.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                changeCamera();
            }
        });

        fabst.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(!started){
                    fabst.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_pause));
                    started = true;
                    startServer();
                }else{
                    fabst.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_media_play));
                    started = false;
                    stopServer();
                }
            }
        });

        // Initialize the CameraProviderFuture
        mCameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            startCameraPreview();
        }
        //Get all available camera
        try{
            List<CameraInfo> availableCameraInfos = mCameraProviderFuture.get().getAvailableCameraInfos();
            Log.d(TAG, "[startCamera] available cameras:"+availableCameraInfos);
        }catch(Exception e){e.printStackTrace();}
    }


    //--------------------------end of oncreate---------------------


    private void stopServer(){
        if (mjpegServer != null) {
            mjpegServer.cancel(true);
            mjpegServer = null;
            Log.d(TAG, "MJPEG server stoped");
        }
    }

    private void startServer() {
        try {
            mjpegServer = new MjpegServer();
            mjpegServer.execute();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void changeCamera(){
        CameraSelector newCameraSelector = mCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;
        mCameraSelector = newCameraSelector;
        startCameraPreview();
    }

    // Start the camera preview
    private void startCameraPreview() {
        // Wait for the CameraProvider to be available

        mCameraProviderFuture.addListener(() -> {
            try {
                // Get the CameraProvider
                ProcessCameraProvider cameraProvider = mCameraProviderFuture.get();

                // Must unbind the use-cases before rebinding them
                cameraProvider.unbindAll();

                // Set up the Preview use case
                @SuppressLint("RestrictedApi")
                Preview preview = new Preview.Builder().setCameraSelector(mCameraSelector).build();

                // Set up the ImageAnalysis use case
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        //.setTargetResolution(new Size(1080, 1920))
                        //.setTargetResolution(new Size(720, 1280))
                        //.setOutputImageRotationEnabled(true)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this),this::processImage);

                preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
                camera = cameraProvider.bindToLifecycle(this, mCameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }


    private void processImage(ImageProxy image) {
        // Process the live video frames here
        if(started){
            int rotation = image.getImageInfo().getRotationDegrees();
            ByteBuffer rgbaBuffer = image.getPlanes()[0].getBuffer();
            int rgbaSize = rgbaBuffer.remaining();
            byte[] rgbaData = new byte[rgbaSize];

            // Reset buffer position
            rgbaBuffer.position(0);

            // Copy RGBA data to the rgbaData array
            rgbaBuffer.get(rgbaData, 0, rgbaSize);

            // Convert RGBA to JPEG
            Bitmap bitmap = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(rgbaData));
            ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, jpegStream);
            latestJpegFrameData = jpegStream.toByteArray();
            mjpegServer.setLatestJpegFrameData(latestJpegFrameData);
            //jpegServer.setJPGframedata(latestJpegFrameData);
        }
        image.close();
    }

    // Handle camera permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraPreview();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

}
