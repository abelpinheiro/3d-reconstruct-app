package com.example.reconstruct_sfm;

import static org.opencv.core.CvType.CV_8U;
import static org.opencv.core.CvType.CV_8UC1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.calib3d.StereoSGBM;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FastFeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.features2d.SIFT;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button select, sfmButton;
    private ImageView imgViewLeft, imgViewRight, imgViewResult;
    List<String> imagesEncodedList;
    public static final int CAM_PERMISSIONS_REQUEST = 0;
    int PICK_IMAGE_MULTIPLE = 1;
    ArrayList<Uri> mArrayUri;
    int position = 0;
    private float[] focalLength;
    private float[] distortions;
    private float[] intrisics;
    public Mat internal_dist;
    public float internal_f;
    public Mat internal_k;


    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("OPENCV", "OpenCV Loading Status: " + OpenCVLoader.initDebug());

        select = findViewById(R.id.button);
        sfmButton = findViewById(R.id.sfm);
        imgViewLeft = findViewById(R.id.imageViewLeft);
        imgViewResult = findViewById(R.id.imageViewResult);
        imgViewRight = findViewById(R.id.imageViewRight);
        mArrayUri = new ArrayList<Uri>();

        Context context = getApplicationContext();
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        CameraSelector cameraSelector = new CameraSelector(manager);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            //String physicalCameraId = cameraSelector.physicalCameraId(0);
            //Log.e(getCallingPackage(), "physical camera id: " + physicalCameraId);
        }
        String depthCameraId = cameraSelector.depthCameraId();
        String stereoCameraId = cameraSelector.stereoCameraId();
        Log.e(getCallingPackage(), "depth camera id: " + depthCameraId + " stereo camera id: " + stereoCameraId);
        cameraSelector.logDepthSupport();
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);
                float[] intrinsic = new float[5];
                intrinsic = characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION);
                Log.e(getCallingPackage(), "valor de intrisic é: " + Arrays.toString(intrinsic));
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mArrayUri.clear();
                // initialising intent
                Intent intent = new Intent();

                // setting type to select to be image
                intent.setType("image/*");

                // allowing multiple image to be selected
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_MULTIPLE);

            }
        });

        checkCamPermissions();

    }



    private void checkCamPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAM_PERMISSIONS_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            // When an Image is picked
            if (requestCode == PICK_IMAGE_MULTIPLE && resultCode == RESULT_OK && null != data) {
                // Get the Image from data
                if (data.getClipData() != null) {
                    ClipData mClipData = data.getClipData();
                    int cout = data.getClipData().getItemCount();
                    for (int i = 0; i < cout; i++) {
                        // adding imageuri in array
                        Uri imageurl = data.getClipData().getItemAt(i).getUri();
                        mArrayUri.add(imageurl);
                    }
                    // setting 1st selected image into image switcher
                    imgViewLeft.setImageURI(mArrayUri.get(0));
                    imgViewRight.setImageURI(mArrayUri.get(1));
                    position = 0;
                }
            } else {
                // show this if no image is selected
                Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG)
                    .show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCameraParameters();
    }

    private void getExifData() throws JpegParser.JpegMarkerNotFound, JpegParser.DepthImageNotFound {
        Bitmap bitmap = ((BitmapDrawable) imgViewLeft.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageInByte = baos.toByteArray();
        JpegParser jpegParser = new JpegParser(imageInByte);
        ByteBuffer depthMap = jpegParser.getDepthMap();
        Log.e(getCallingPackage(), "getExifData: " + depthMap );
//save your stuff
    }

    public void performSfm(View view) {
        Log.d("deu bom", "ENTROU");
        try {
            getExifData();
        } catch (JpegParser.JpegMarkerNotFound jpegMarkerNotFound) {
            jpegMarkerNotFound.printStackTrace();
        } catch (JpegParser.DepthImageNotFound depthImageNotFound) {
            depthImageNotFound.printStackTrace();
        }
        getCameraParameters();
        try {
            Bitmap bmp1 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mArrayUri.get(0));
            Bitmap bmp2 = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mArrayUri.get(1));
            Bitmap newBmp1 = getResizedBitmap(bmp1, 700, 500);
            Bitmap newBmp2 = getResizedBitmap(bmp2, 700, 500);

            Mat img1 = new Mat();
            Mat img2 = new Mat();
            Bitmap bmp132 = newBmp1.copy(Bitmap.Config.ARGB_8888, true);
            Bitmap bmp232 = newBmp2.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp132, img1);
            Utils.bitmapToMat(bmp232, img2);
            Log.e(getCallingPackage(), "tamanho do mat: " + img1.size());

            //Convert to gray scale
            Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGR2GRAY);
            Imgproc.cvtColor(img2, img2, Imgproc.COLOR_BGR2GRAY);

            Mat undistortedImg1 = new Mat(img1.size(), CV_8UC1);
            Mat undistortedImg2 = new Mat(img2.size(), CV_8UC1);
            Mat newK = Calib3d.getOptimalNewCameraMatrix(internal_k, internal_dist, img1.size(), 1);

            Log.e(getCallingPackage(), "DIST " + internal_dist.dump() + " dist d: " + internal_dist);
            Log.e(getCallingPackage(), "K " + internal_k.dump() + " k d: " + internal_k);
            //Log.e(getCallingPackage(), "undistortedImg1 " + undistortedImg1.dump() + " undistortedImg1 d: " + undistortedImg1);
            Calib3d.undistort(img1, undistortedImg1, internal_k, internal_dist, newK);
            Log.e(getCallingPackage(), "DEU UNDISTORT DO 1");
            Calib3d.undistort(img2, undistortedImg2, internal_k, internal_dist, newK);
            Log.e(getCallingPackage(), "DEU UNDISTORT DO 2");

            Mat disparity = getDisparity(undistortedImg1, undistortedImg2);
            Log.e(getCallingPackage(), "DEU DISPARITY");

            //img1 = sift(img1, img2);

            showResult(disparity);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("dpegou", "pegou");


    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private void getCameraParameters() {
        SharedPreferences sharedPref = this.getSharedPreferences("PREF_CAM_CONFIG", Context.MODE_PRIVATE);
        //String focalLength = sharedPref.getString("focal_length", "0");
        String cameraId = sharedPref.getString("camera_id", "0");
        Log.e(getCallingPackage(), "focal length: " + focalLength + " camera id: " + cameraId);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        CameraCharacteristics characteristics = null;
        try {
            characteristics = manager.getCameraCharacteristics(cameraId);
            focalLength = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            intrisics = characteristics.get(CameraCharacteristics.LENS_INTRINSIC_CALIBRATION);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                distortions = characteristics.get(CameraCharacteristics.LENS_DISTORTION);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        float f_x = intrisics[0];
        float f_y = intrisics[1];
        float c_x = intrisics[2];
        float c_y = intrisics[3];
        float s = intrisics[4];

        float[] camera_intr = {f_x, s, c_x, 0, f_y, c_y, 0, 0, 1};
        internal_k = new Mat(3, 3, CvType.CV_32F);
        internal_k.put(0,0, camera_intr);

        internal_f = focalLength[0];

        float[] camera_dist = {distortions[0], distortions[1], distortions[3], distortions[4], distortions[2]};
        internal_dist = new Mat(1, 5, CvType.CV_32FC1);
        internal_dist.put(0,0,camera_dist);

        Log.e(getCallingPackage(), "focal length: " + Arrays.toString(focalLength) + " intrinsics: " + internal_k.dump() + " distortions normal: " + Arrays.toString(distortions) + " distortions no mat: " + internal_dist.dump());
    }

    private Mat getDisparity(Mat left, Mat right) {
        Mat disparity = new Mat(left.size(), left.type());
        int numDisparity = (int) left.size().width/8;

        StereoSGBM stereoAlgo = StereoSGBM.create(
                0,
                2*16,
                5,
                8*3*15 , //8 * numero de canais da imagem * block size
                32*3*15, //
                12,
                63,
                5,
                50,
                2,
                StereoSGBM.MODE_SGBM
        );

        stereoAlgo.compute(left, right, disparity);
        Core.normalize(disparity, disparity, 0, 256, Core.NORM_MINMAX, CV_8U);
        return disparity;
    }

    private Mat sift(Mat img1, Mat img2) {

        MatOfKeyPoint keyPointsImg1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPointsImg2 = new MatOfKeyPoint();
        //detector.detect(img1, keyPointsImg1);
        //detector.detect(img2, keyPointsImg2);
        Log.d("Key points img 1: ",keyPointsImg1.toString());
        Features2d.drawKeypoints(img1, keyPointsImg1, img1);

        return img1;
    }

    private void showResult(Mat img1) {
        Bitmap result = Bitmap.createBitmap(img1.cols(), img1.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img1, result);
        imgViewResult.setImageBitmap(result);
    }

    public void openCamera(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }
}