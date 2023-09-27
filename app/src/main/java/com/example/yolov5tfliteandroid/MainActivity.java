package com.example.yolov5tfliteandroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.view.PreviewView;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.lifecycle.ProcessCameraProvider;

import com.example.yolov5tfliteandroid.analysis.FullImageAnalyse;
import com.example.yolov5tfliteandroid.analysis.FullScreenAnalyse;
import com.example.yolov5tfliteandroid.detector.Yolov5TFLiteDetector;
import com.example.yolov5tfliteandroid.utils.CameraProcess;
import com.example.yolov5tfliteandroid.utils.ImageProcess;
import com.google.android.material.tabs.TabLayout;
import com.google.common.util.concurrent.ListenableFuture;
import android.view.OrientationEventListener;


public class MainActivity extends AppCompatActivity {

    private boolean IS_FULL_SCREEN = false;
    private OrientationEventListener mOrientationEventListener;
    private PreviewView cameraPreviewMatch;
    private PreviewView cameraPreviewWrap;
    private ImageView boxLabelCanvas;
    private Spinner modelSpinner;
    private Switch immersive;
    private TextView inferenceTimeTextView;
    private TextView frameSizeTextView;
    private Yolov5TFLiteDetector mYolov5TFLiteDetector;
    private Boolean addGPU = true;
    private CameraProcess cameraProcess = new CameraProcess();
    private ImageAnalysis.Analyzer mImageAnalyse;
    public int rotaion = 0 ;

//    /**
//     * 获取屏幕旋转角度,0表示拍照出来的图片是横屏
//     *
//     */
//    protected int getScreenOrientation() {
//        switch (getWindowManager().getDefaultDisplay().getRotation()) {
//            case Surface.ROTATION_270:
//                return 270;
//            case Surface.ROTATION_180:
//                return 180;
//            case Surface.ROTATION_90:
//                return 90;
//            default:
//                return 0;
//        }
//    }

    @Override
    public void onConfigurationChanged(Configuration config){
        super.onConfigurationChanged(config);
        // 检测屏幕方向并设置变量
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横屏时更新变量
            this.rotaion = 90;
            Log.i("Orientation", "Changed to Landscape");
        } else if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 竖屏时更新变量
            this.rotaion = 0;
            Log.i("Orientation", "Changed to Portrait");
        }

        setContentView(R.layout.activity_main);
        // 初始化视图
        this.initialView();

        // 初始化加载yolov5s模型，并默认启动全图分析
        initModelActivity("yolov5s");
        mImageAnalyse = new FullImageAnalyse(MainActivity.this,
                cameraPreviewWrap,
                boxLabelCanvas,
                rotaion,
                inferenceTimeTextView,
                frameSizeTextView,
                mYolov5TFLiteDetector);
        cameraProcess.startCamera(MainActivity.this, mImageAnalyse, cameraPreviewWrap);

        /*
         *监听模型切换按钮
         */
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String model = (String) adapterView.getItemAtPosition(i);
                Toast.makeText(MainActivity.this, "loading model: " + model, Toast.LENGTH_LONG).show();
                initModelActivity (model);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        /*
         * 监听视图变化按钮
         */
        immersive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                IS_FULL_SCREEN = b;
                if (b) {
                    // 进入全屏模式
                    Toast.makeText(MainActivity.this, "rotation: " + rotaion, Toast.LENGTH_LONG).show();
                    cameraPreviewWrap.removeAllViews();
                    ImageAnalysis.Analyzer analyse = new FullScreenAnalyse(MainActivity.this,
                            cameraPreviewMatch,
                            boxLabelCanvas,
                            rotaion,
                            inferenceTimeTextView,
                            frameSizeTextView,
                            mYolov5TFLiteDetector);
                    cameraProcess.startCamera(MainActivity.this, analyse, cameraPreviewMatch);
                } else {
                    // 进入全图模式
                    Toast.makeText(MainActivity.this, "rotation: " + rotaion, Toast.LENGTH_LONG).show();

                    cameraPreviewMatch.removeAllViews();
                    ImageAnalysis.Analyzer analyse = new FullImageAnalyse(
                            MainActivity.this,
                            cameraPreviewWrap,
                            boxLabelCanvas,
                            rotaion,
                            inferenceTimeTextView,
                            frameSizeTextView,
                            mYolov5TFLiteDetector);
                    cameraProcess.startCamera(MainActivity.this, analyse, cameraPreviewWrap);
                }
            }
        });
    }
    /**
     * 主线程UI渲染
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // 打开app的时候隐藏顶部状态栏
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);


        // 初始化视图
        this.initialView();

        // 申请摄像头权限
        if (!cameraProcess.allPermissionsGranted(this)) {
            cameraProcess.requestPermissions(this);}

        // 获取手机摄像头拍照旋转参数a
//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Log.i("image", "rotation: " + this.rotaion);
        Log.i("image","ScreenOrientation: " + getResources().getConfiguration().orientation);

        //注册相机，摄像头硬件初始化
        this.cameraProcess.showCameraSupportSize(MainActivity.this);

        // 初始化加载yolov5s模型，并默认启动全图分析
        initModelActivity("yolov5s");
        mImageAnalyse = new FullImageAnalyse(MainActivity.this,
                cameraPreviewWrap,
                boxLabelCanvas,
                rotaion,
                inferenceTimeTextView,
                frameSizeTextView,
                mYolov5TFLiteDetector);
        cameraProcess.startCamera(MainActivity.this, mImageAnalyse, cameraPreviewWrap);


        /*
         *监听模型切换按钮
         */
        modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String model = (String) adapterView.getItemAtPosition(i);
                Toast.makeText(MainActivity.this, "loading model: " + model, Toast.LENGTH_LONG).show();
                initModelActivity (model);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        /*
         * 监听视图变化按钮
         */
        immersive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                IS_FULL_SCREEN = b;
//                rotaion = 0;
                if (b) {
                    // 进入全屏模式
                    cameraPreviewWrap.removeAllViews();
                    FullScreenAnalyse fullScreenAnalyse = new FullScreenAnalyse(MainActivity.this,
                            cameraPreviewMatch,
                            boxLabelCanvas,
                            rotaion,
                            inferenceTimeTextView,
                            frameSizeTextView,
                            mYolov5TFLiteDetector);
                    cameraProcess.startCamera(MainActivity.this, fullScreenAnalyse, cameraPreviewMatch);

                } else {
                    // 进入全图模式
                    cameraPreviewMatch.removeAllViews();
                    FullImageAnalyse fullImageAnalyse = new FullImageAnalyse(
                            MainActivity.this,
                            cameraPreviewWrap,
                            boxLabelCanvas,
                            rotaion,
                            inferenceTimeTextView,
                            frameSizeTextView,
                            mYolov5TFLiteDetector);
                    cameraProcess.startCamera(MainActivity.this, fullImageAnalyse, cameraPreviewWrap);
                }
            }
        });
    }


    /**
     * 定义初始化视图方法
     *
     * @param
     */
    private void initialView(){
        // 全屏画面
        cameraPreviewMatch = findViewById(R.id.camera_preview_match);
        cameraPreviewMatch.setScaleType(PreviewView.ScaleType.FILL_START);
        // 全图画面
        cameraPreviewWrap = findViewById(R.id.camera_preview_wrap);
//        cameraPreviewWrap.setScaleType(PreviewView.ScaleType.FILL_START);
        // box/label画面
        boxLabelCanvas = findViewById(R.id.box_label_canvas);
        // 下拉按钮
        modelSpinner = findViewById(R.id.model);
        // 沉浸式体验按钮
        immersive = findViewById(R.id.immersive);
        // 实时更新的一些view
        inferenceTimeTextView = findViewById(R.id.inference_time);
        frameSizeTextView = findViewById(R.id.frame_size);
    }


    /**
     * 定义初始化模型检测器Yolov5TFLiteDetector的方法
     *
     * @param modelName
     */
    private void initModelActivity(String modelName) {
        // 加载模型
        try {
            this.mYolov5TFLiteDetector = new Yolov5TFLiteDetector();
            this.mYolov5TFLiteDetector.setModelFile(modelName);
//            this.mYolov5TFLiteDetector.addNNApiDelegate();
            if(addGPU) {
                this.mYolov5TFLiteDetector.addGPUDelegate();
            } else return;
            this.mYolov5TFLiteDetector.initialModel(this);
            Log.i("model", "Success loading model" + this.mYolov5TFLiteDetector.getModelFile());
        } catch (Exception e) {
            Log.e("image", "load model error: " + e.getMessage() + e.toString());
        }
    }
}