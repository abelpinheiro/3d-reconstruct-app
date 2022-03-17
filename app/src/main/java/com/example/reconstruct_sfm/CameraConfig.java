package com.example.reconstruct_sfm;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.Arrays;
import java.util.Set;

public class CameraConfig extends AppCompatActivity {

    public String[] cameraIdList;
    public String[] cameraTypeList;
    public Size[] cameraSizeList;
    public ListView cameraTypeListView;
    public ListView cameraSizeListView;
    public int currentCameraId = 0;
    public CameraCharacteristics characteristics;
    public float[] intrisics = new float[5];
    public float[] maximumIntrisics = new float[5];
    public float[] poseRotation = new float[5];
    public float[] poseTranslation = new float[5];
    public int[] distortionModes = new int[3];
    public float[] distortions = new float[5];
    public float[] focalLength = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_config);
        cameraTypeListView = findViewById(R.id.camera_ids_list);
        cameraSizeListView = findViewById(R.id.camera_sizes_list);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            cameraIdList = extras.getStringArray("cam_list");
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try{
            cameraTypeList = manager.getCameraIdList();
            characteristics = manager.getCameraCharacteristics(String.valueOf(currentCameraId));
            cameraSizeList = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            int levelSupport = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            Size[] outputSize = map.getOutputSizes(SurfaceTexture.class);
            Log.e(getCallingPackage(), "level de support " + levelSupport);
            Log.e(getCallingPackage(), " output: " + Arrays.asList(outputSize));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        ArrayAdapter<String> camTypeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, cameraIdList);
        //ArrayAdapter<String> camSizeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        cameraTypeListView.setAdapter(camTypeAdapter);
        cameraTypeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.e(getCallingPackage(), "vocÊ clicou em: " + i + "o que isignica l:" + l );
                currentCameraId = i;
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        CameraCharacteristics characteristics = manager.getCameraCharacteristics(String.valueOf(currentCameraId));
                        focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                        intrisics = characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION);
                        distortions = characteristics.get(CameraCharacteristics.LENS_DISTORTION);

                        int valor = characteristics.get(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION);
                        Log.e(getCallingPackage(), "valor: " + valor);

                        AlertDialog alertDialog = new AlertDialog.Builder(CameraConfig.this).create(); //Read Update
                        alertDialog.setTitle("Características da câmera");
                        alertDialog.setMessage("focal length: " + Arrays.toString(focalLength) +
                                "\n\n\nIntrinsics: " + Arrays.toString(intrisics) +
                                "\n\n\nDistortions: " + Arrays.toString(distortions));
                        alertDialog.show();

                        cameraSizeList = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        ArrayAdapter<Size> camSizeAdapter = new ArrayAdapter<Size>(this, android.R.layout.simple_list_item_1, cameraSizeList);
    }

    public void saveCameraConfig(View view) {
        Log.e(getCallingPackage(), "saveCameraConfig: " + currentCameraId);
        SharedPreferences sharedPref = this.getSharedPreferences("PREF_CAM_CONFIG", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("focal_length", String.valueOf(focalLength[0]));
        editor.putString("camera_id", String.valueOf(currentCameraId));
        editor.putString("camera_distortions", String.valueOf(distortions));
        editor.putString("camera_intrinsics", String.valueOf(intrisics));
        editor.apply();
        Log.e(getCallingPackage(), "TA INDO " + focalLength[0] + " e " + currentCameraId);
        finish();
    }
}