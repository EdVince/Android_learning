package com.journaldev.androidcamerax;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;


import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {


    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    TextureView textureView; // 这个就是我们用来preview的textureview

    // 检查所有的需要的权限是否都拿到了
    private boolean allPermissionsGranted(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 设置我们的布局文件activity_main

        textureView = findViewById(R.id.view_finder); // 将我们定义的textureview跟实际控件绑定起来，这个view_finder就是我们在activity_main.xml里面写的那个

        // 权限检查
        if(allPermissionsGranted()){
            startCamera(); // 用户同意给权限的话就开启摄像头
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS); // 请求两个权限
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 这是个请求权限的回调函数，就是在前面onCreate里面请求权限，我们用户选择了权限究竟给不给后，就进入这个权限回调函数，在这里处理有权限跟没权限分别都做什么
        if(requestCode == REQUEST_CODE_PERMISSIONS){
            if(allPermissionsGranted()){
                startCamera(); // 权限都函数就开摄像头
            } else{
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish(); // 权限不满足就直接滚
            }
        }
    }

    private void startCamera() {

        CameraX.unbindAll(); // 解除所有绑定，我查了一下，这好像是Alpha版API的东西，现在新的API好像不用这个了，有空看一下新的怎么写

        Rational aspectRatio = new Rational (textureView.getWidth(), textureView.getHeight()); // 获取textureview的宽高比
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); // 获取屏幕的尺寸

        // 这是配置预览的地方
        PreviewConfig pConfig = new PreviewConfig.Builder()
                                    .setTargetAspectRatio(aspectRatio) // 设置宽高比
                                    .setTargetResolution(screen) // 设置实际屏幕
                                    .setLensFacing(CameraX.LensFacing.FRONT)
                                    .build();
        Preview preview = new Preview(pConfig); // 用预览配置生成preview
        // 设置监听
        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput output){
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);
                        // 相机预览的是通过SurfaceTexture来返回的
                        // 通过把TextureView的SurfaceTexture替换成相机的SurfaceTexture就实现了Textureview控件显示Camera预览内容了
                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                        updateTransform(); // 这是是要做横竖屏切换的
                    }
                });

        // 这是配置图像捕获配置
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                        .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY) // 设置捕获模式为最小延迟，还有最高质量:MAX_QUALITY
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()) // 这是设置拍出来照片是角度的
                        .setLensFacing(CameraX.LensFacing.FRONT)
                        .build();
        // 用捕获配置生成capture
        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

        // 给布局中的那个捕获案件绑定点击监听事件，有点类似于Qt的connect槽函数的味道
        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 用外部存储的目录+当前事件拼了一个.png后缀的文件名出来，图像就保存在这里了
                File file = new File(Environment.getExternalStorageDirectory() + "/" + System.currentTimeMillis() + ".png");
                // 拍照存储图像
                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    // 保存成功显示一下保存的路径
                    @Override
                    public void onImageSaved(@NonNull File file) {
                        String msg = "Pic captured at " + file.getAbsolutePath();
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                    }
                    // 保存错误的时候显示一下信息
                    @Override
                    public void onError(@NonNull ImageCapture.UseCaseError useCaseError, @NonNull String message, @Nullable Throwable cause) {
                        String msg = "Pic capture failed : " + message;
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                        if(cause != null){
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });

        // 绑定到生命周期
        CameraX.bindToLifecycle((LifecycleOwner)this, preview, imgCap);
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();

        switch(rotation){
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

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setTransform(mx);
    }


}
