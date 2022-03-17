package com.example.reconstruct_sfm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SizeConfigActivity extends AppCompatActivity {

    public String currentCameraId;
    public Size[] cameraSizeList;
    public ListView cameraSizeListView;
    public CameraCharacteristics characteristics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_size_config);
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            currentCameraId = extras.getString("camera_id");
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            characteristics = manager.getCameraCharacteristics(String.valueOf(currentCameraId));
            cameraSizeList = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        ArrayAdapter<Size> camSizeAdapter = new ArrayAdapter<Size>(this, android.R.layout.simple_list_item_1, cameraSizeList);
        //ArrayAdapter<String> camSizeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        cameraSizeListView.setAdapter(camSizeAdapter);
        //cameraSizeListView.setOnItemClickListener(new );
    }

    public void saveCameraConfig(View view) {
    }
}