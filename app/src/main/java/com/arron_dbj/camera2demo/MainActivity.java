package com.arron_dbj.camera2demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraDevice;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arron_dbj.camera2demo.anim.Animator;
import com.arron_dbj.camera2demo.utils.Camera2Helper;
import com.arron_dbj.camera2demo.utils.FileHelper;
import com.arron_dbj.camera2demo.utils.Logger;
import com.arron_dbj.camera2demo.utils.PermissionUtil;
import com.arron_dbj.camera2demo.utils.StringUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private SurfaceView surfaceView;
    private SurfaceHolder mSurfaceHolder;
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };
    private static final int REQUEST_OK = 1;
    private static final String tag = "Camera2Demo";

    private Camera2Helper camera2Helper;
    private CameraDevice mCameraDevice;
    private FloatingActionButton floatingActionButton;
    private Configuration mConfiguration;
    private Logger logger;
    private ImageView ivFlash, ivMainButton, ivSwitchCamera, ivEditImage, ivUnSaveBack, ivSaveBack,
            ivRedDot, ivCapture;
    private TextView tvZoom;
    private Chronometer timer;

    private FrameLayout editImageVideoLayout;
    private RelativeLayout controlPanelLayout;
    private LinearLayout topControlPanelLayout;

    private boolean isPhoto = true;//是否是拍照
    private boolean isRecoding;//是否在录像
    private boolean isFacingBack = true;

    // 后置相机和前置相机的值
    private static final int CAMERA_BACK = 1;
    private static final int CAMERA_FRONT = 0;

    private Bitmap previewBitmap;
    private String mVideoPath;

    private File dir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        EventBus.getDefault().register(this);
        PermissionUtil.verifyPermissions(this, PERMISSIONS, 0);
        camera2Helper = Camera2Helper.getInstance(surfaceView);
        dir = FileHelper.getInstance(this).getDiskCacheDir(this, "Video");
        if (!dir.exists())
            dir.mkdirs();


        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    camera2Helper.initCamera2(MainActivity.this);
                    camera2Helper.adjustScreenSize(surfaceView);
                } else {
                    PermissionUtil.verifyPermissions(MainActivity.this, PERMISSIONS, 0);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                camera2Helper.adjustScreenSize(surfaceView);
                logger.debug(tag, "orientation: " +
                        MainActivity.this.getResources().getConfiguration().orientation);
                logger.debug(tag, "surface的宽：" + width + " surface的高：" + height);
                logger.debug(tag, "rotation: " + getWindowManager().getDefaultDisplay().getRotation());
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void initBitmap(Bitmap bitmap) {
        ivEditImage.setImageBitmap(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 0:
                if (permissions.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    camera2Helper.initCamera2(MainActivity.this);
                } else {
                    Toast.makeText(MainActivity.this, "请打开相机权限", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void initView() {
        surfaceView = findViewById(R.id.surface_view);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);
        surfaceView.setFocusable(true);

        // 主按钮，负责拍照录制
        ivMainButton = findViewById(R.id.take_photo);
        ivMainButton.setOnClickListener(this);
        // 开关闪光灯按钮
        ivFlash = findViewById(R.id.iv_flash);
        ivFlash.setOnClickListener(this);
        // 缩放按钮
        tvZoom = findViewById(R.id.tv_zoom);
        // 切换摄像头按钮
        ivSwitchCamera = findViewById(R.id.switch_camera);
        ivSwitchCamera.setOnClickListener(this);
        mConfiguration = this.getResources().getConfiguration();
        logger = Logger.getInstance();
        // 图片编辑保存界面
        editImageVideoLayout = findViewById(R.id.edit_layout);
        ivUnSaveBack = findViewById(R.id.back_unsave);
        ivSaveBack = findViewById(R.id.back_save);
        ivSaveBack.setOnClickListener(this);
        ivUnSaveBack.setOnClickListener(this);
        ivEditImage = findViewById(R.id.unhandled_image);

        controlPanelLayout = findViewById(R.id.control_panel_layout);
        topControlPanelLayout = findViewById(R.id.top_control_panel_layout);
        // 录像计时器
        timer = findViewById(R.id.timer);
        timer.setVisibility(View.GONE);
        timer.setFormat("%s");
        timer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {

            }
        });
        // 拍照按钮
        ivCapture = findViewById(R.id.capture);
        ivCapture.setOnClickListener(this);
        ivRedDot = findViewById(R.id.red_dot);
    }

    @Override
    public void onClick(View v) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (v.getId()) {
            case R.id.capture:
                ivEditImage.setImageBitmap(null);
                camera2Helper.takePhoto(rotation);
                showEditImageUI();
                camera2Helper.closePreviewSession();
                break;
            case R.id.take_photo:
                mVideoPath = new File(dir, StringUtil.getCurrentTime_yyyyMMddHHmmss() + ".mp4").getPath();
                if (isRecoding){
                    timer.setVisibility(View.GONE);
                    timer.stop();
                    timer.setBase(SystemClock.elapsedRealtime());
                    ivRedDot.setVisibility(View.GONE);
                    camera2Helper.stopRecord();
                    Toast.makeText(MainActivity.this, "视频储存在："+mVideoPath, Toast.LENGTH_SHORT).show();
                }else {
                    camera2Helper.startRecord(mVideoPath, rotation);
                    timer.start();
                    timer.setVisibility(View.VISIBLE);
                    timer.setBase(SystemClock.elapsedRealtime());
                    ivRedDot.setVisibility(View.VISIBLE);
                    Animator.flickerAnim(ivRedDot);
                    Toast.makeText(MainActivity.this, "开始录制视频", Toast.LENGTH_SHORT).show();
                }
                isRecoding = !isRecoding;
                break;
            case R.id.iv_flash:
                camera2Helper.openFlashLight(ivFlash);
                break;
            case R.id.switch_camera:
                if (isFacingBack) {
                    ivSwitchCamera.setImageTintList(ColorStateList.valueOf(0xffEFB90F));
                } else {
                    ivSwitchCamera.setImageTintList(ColorStateList.valueOf(0xffffffff));
                }
                Animator.switchCameraAnim(surfaceView);
                camera2Helper.switchCamera(isFacingBack ? CAMERA_BACK : CAMERA_FRONT);
                isFacingBack = !isFacingBack;
                break;
            case R.id.back_unsave:
                if (previewBitmap != null) {
                    previewBitmap = null;
                }
                showMainUI();
                camera2Helper.startPreview();
                logger.debug(tag, "不保存照片，返回预览");
                break;
            case R.id.back_save:
                showMainUI();
                camera2Helper.saveImage();
                camera2Helper.startPreview();
                break;
        }

    }

    public void onZoomClick(View view) {
        camera2Helper.zoomPreview(tvZoom);
    }

    @Override
    protected void onStop() {
        super.onStop();
        camera2Helper.releaseCamera2();
        EventBus.getDefault().unregister(this);
        if (previewBitmap != null) {
            previewBitmap = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camera2Helper.releaseCamera2();
        EventBus.getDefault().unregister(this);
        if (previewBitmap != null) {
            previewBitmap = null;
        }
    }

    private void showMainUI() {
        surfaceView.setVisibility(View.VISIBLE);
        editImageVideoLayout.setVisibility(View.GONE);
        controlPanelLayout.setVisibility(View.VISIBLE);
        topControlPanelLayout.setVisibility(View.VISIBLE);
    }

    private void showEditImageUI() {
        surfaceView.setVisibility(View.GONE);
        controlPanelLayout.setVisibility(View.GONE);
        topControlPanelLayout.setVisibility(View.GONE);
        editImageVideoLayout.setVisibility(View.VISIBLE);
    }
}
