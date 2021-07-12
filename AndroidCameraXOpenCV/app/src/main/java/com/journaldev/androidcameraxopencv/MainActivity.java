package com.journaldev.androidcameraxopencv;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    TextureView textureView;// 用来preview的textureview
    ImageView ivBitmap;     // 叠加在textureview上面的bitmap
    LinearLayout llBottom;  // 下来多选

    int currentImageType = Imgproc.COLOR_RGB2GRAY; // 先确定一开始的模式是RGB2GRAY

    // CameraX三大金刚：预览、分析、捕获
    Preview preview;
    ImageAnalysis imageAnalysis;
    ImageCapture imageCapture;

    // 拍照要用到的三个案件
    FloatingActionButton btnCapture, btnOk, btnCancel;

    // 加载OpenCV
    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }

    // 查询是否满足当前所有权限需求
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCapture = findViewById(R.id.btnCapture); // 绑定拍照按键
        btnOk = findViewById(R.id.btnAccept);       // 绑定确定照片按键
        btnCancel = findViewById(R.id.btnReject);   // 绑定取消照片按键

        btnOk.setOnClickListener(this);
        btnCancel.setOnClickListener(this);

        llBottom = findViewById(R.id.llBottom);         // 绑定下拉按键
        textureView = findViewById(R.id.textureView);   // 绑定textureview
        ivBitmap = findViewById(R.id.ivBitmap);         // 绑定bitmap

        // 检查权限并启动摄像头
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 权限请求回调函数
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void updateTransform() {
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int) textureView.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float) rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }

    private void startCamera() {
        CameraX.unbindAll(); // 解绑CameraX
        preview = setPreview(); // 设置预览
        imageCapture = setImageCapture();   // 设置图像捕获
        imageAnalysis = setImageAnalysis(); // 设置图像分析
        // 绑定CameraX到生命周期，并喂入预览、捕获、分析
        CameraX.bindToLifecycle(this, preview, imageCapture, imageAnalysis);
    }

    private Preview setPreview() {
        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight()); // 获取textureview的宽高比
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); // 获取屏幕的尺寸

        // 配置预览设置
        PreviewConfig pConfig = new PreviewConfig.Builder()
                                    .setTargetAspectRatio(aspectRatio)
                                    .setTargetResolution(screen)
                                    .build();
        Preview preview = new Preview(pConfig); // 用预览配置生成preview
        // 设置预览监听
        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output) {
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);
                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform();
                    }
                });
        return preview;
    }

    // 捕获按键和接受拒绝按键的切换
    private void showAcceptedRejectedButton(boolean acceptedRejected) {
        if (acceptedRejected) {
            CameraX.unbind(preview, imageAnalysis);
            llBottom.setVisibility(View.VISIBLE);
            btnCapture.hide();
            textureView.setVisibility(View.GONE);
        } else {
            btnCapture.show();
            llBottom.setVisibility(View.GONE);
            textureView.setVisibility(View.VISIBLE);
            textureView.post(new Runnable() {
                @Override
                public void run() {
                    startCamera();
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnReject:
                showAcceptedRejectedButton(false); // 切换回捕获按键
                break;
            case R.id.btnAccept:
                // 获取照片名字
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "" + System.currentTimeMillis() + "_JDCameraX.jpg");
                // 拍照监听函数
                imageCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        showAcceptedRejectedButton(false); // 切换回捕获按键
                        Toast.makeText(getApplicationContext(),
                                "Image saved successfully in Pictures Folder", Toast.LENGTH_LONG).show();
                    }
                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                    }
                });
                break;
        }
    }

    private ImageCapture setImageCapture() {
        // 设置捕获配置并生成capture
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())
                .build();
        final ImageCapture imgCapture = new ImageCapture(imageCaptureConfig);

        // 给拍照按键设置个监听事件
        btnCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgCapture.takePicture(new ImageCapture.OnImageCapturedListener() {
                    @Override
                    public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                        Bitmap bitmap = textureView.getBitmap(); // 从textureview中把gitmap取出来
                        showAcceptedRejectedButton(true); // 这里主要做按键切换，点了拍照按键后，拍照按键隐藏，显示接受和拒绝按键
                        ivBitmap.setImageBitmap(bitmap); // 不太清楚为什么要这样做
                    }
                    @Override
                    public void onError(ImageCapture.UseCaseError useCaseError, String message, @Nullable Throwable cause) {
                        super.onError(useCaseError, message, cause);
                    }
                });
            }
        });
        return imgCapture;
    }

    private ImageAnalysis setImageAnalysis() {
        // 设置用来做图像分析的线程
        HandlerThread analyzerThread = new HandlerThread("OpenCVAnalysis");
        analyzerThread.start();

        // 配置图像分析并生成imageanalysis
        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE) // 接收最新的图
                .setCallbackHandler(new Handler(analyzerThread.getLooper())) // 设置回调
                .setImageQueueDepth(1).build(); // 设置图像队列深度为1
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);

        // 设置分析器
        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy image, int rotationDegrees) {
                        // 获取摄像头实时返回的textureview的bitmap
                        final Bitmap bitmap = textureView.getBitmap();
                        if(bitmap==null)
                            return;
                        Mat mat = new Mat(); // 新建一个OpenCV的Mat类用来处理图像
                        Utils.bitmapToMat(bitmap, mat); // OpenCV提供的功能，bitmap转Mat
                        Imgproc.cvtColor(mat, mat, currentImageType); // mat原地做色彩转换
                        Utils.matToBitmap(mat, bitmap); // 将图从mat再转回去bitmap
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ivBitmap.setImageBitmap(bitmap); // 将计算后的bitmao喂回去
                            }
                        });
                    }
                });
        return imageAnalysis;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.black_white:
                currentImageType = Imgproc.COLOR_RGB2GRAY;
                startCamera();
                return true;
            case R.id.hsv:
                currentImageType = Imgproc.COLOR_RGB2HSV;
                startCamera();
                return true;
            case R.id.lab:
                currentImageType = Imgproc.COLOR_RGB2Lab;
                startCamera();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
