package com.example.yolov5tfliteandroid.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.yolov5tfliteandroid.MainActivity;
import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

import io.reactivex.rxjava3.core.Observable;

public class CameraProcess {

    /**
     *申明实例
     */
    private ListenableFuture<ProcessCameraProvider> mCameraProviderFuture;
    private int REQUEST_CODE_PERMISSIONS = 1001;
    private final String[] REQUIRED_PERMISSIONS = new String[]{
            "android.permission.CAMERA",
            "android.permission.WRITE_EXTERNAL_STORAGE"};
    private ImageAnalysis imageAnalysis;

    /**
     * 判断摄像头权限
     * @param context
     * @return
     */
    public boolean allPermissionsGranted(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 申请摄像头权限
     * @param activity
     */
    public void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    /**
     * 打开摄像头，提供对应的previewView, 并且注册analyse事件, analyse就是要对摄像头每一帧进行分析的操作
     */
    public void startCamera(Context context, ImageAnalysis.Analyzer analyzer, PreviewView previewView) {

        mCameraProviderFuture = ProcessCameraProvider.getInstance(context);
        mCameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = mCameraProviderFuture.get();
                // 加多这一步是为了切换不同视图的时候能释放上一视图所有绑定事件
                cameraProvider.unbindAll();
                imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                            .setTargetResolution(new Size(1080, 1920))
//                            .setTargetAspectRatioCustom(new Rational(16,9))
//                            .setTargetRotation(Surface.ROTATION_90)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer);
                bindPreview(context, cameraProvider, imageAnalysis, previewView);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));
    }


    /**
     * 选择相机绑定相机事件到视图
     */
    public void bindPreview(Context context, ProcessCameraProvider cameraProvider, ImageAnalysis imageAnalysis, PreviewView previewView){

        // 加多这一步是为了切换不同视图的时候能释放上一视图所有绑定事件
        cameraProvider.unbindAll();

        if (previewView != null) {
            //创建视图
            Preview preview = new Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .build();
            //选择相机
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            preview.setSurfaceProvider(previewView.createSurfaceProvider());
            cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, imageAnalysis, preview);

        }
        else {
            Toast.makeText(context, "erro", Toast.LENGTH_LONG).show();
            Log.e("CameraProcess", "previewView is null");
        }
    }


    /**
     * 打印输出摄像头支持的宽和高
     * @param activity
     */
    public void showCameraSupportSize(Activity activity) {
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String id : manager.getCameraIdList()) {
                CameraCharacteristics cc = manager.getCameraCharacteristics(id);
                if (cc.get(CameraCharacteristics.LENS_FACING) == 1) {
                    Size[] previewSizes = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            .getOutputSizes(SurfaceTexture.class);
                    for (Size s : previewSizes){
                        Log.i("camera", s.getHeight()+"/"+s.getWidth());
                        Toast toast = Toast.makeText(activity, "camera size :" + s.getHeight() + "/" + s.getWidth(), Toast.LENGTH_LONG);
                        View toastView = toast.getView();
                        TextView toastMessage = toastView.findViewById(android.R.id.message);
                        toastMessage.setTextColor(Color.BLACK);  // 设置文本颜色为红色
                        toast.show();
                    }
                    break;

                }
            }
        } catch (Exception e) {
            Log.e("image", "can not open camera", e);
        }
    }

}
