package com.arron_dbj.camera2demo.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arron_dbj.camera2demo.R;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;
import static android.hardware.camera2.CameraMetadata.LENS_FACING_FRONT;


public class Camera2Helper {
    private Handler mainHandler, childHandler;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraCharacteristics mCameraCharacteristics;
    private ImageReader mImageReader;
    private volatile static Camera2Helper camera2Helper;
    private Context mContext;
    private SurfaceView surfaceView;
    private String mCameraId;

    private Size mVideoSize;
    private Size justSize;
    private Size mWinSize;

    private float mScale = 1.0f;

    private boolean isFlashLightOn;
    private static final String tag = "Camera2Demo";
    private MediaRecorder mMediaRecorder;
    private String storePath;
    private CameraCaptureSession mPreviewCaptureSession;
    private CaptureRequest mCaptureRequest;
    private CaptureRequest.Builder mPreviewRequestBuilder;

    private static Bitmap bitmap;
    private byte[] bytes;
    private int lensFacing = LENS_FACING_BACK;
    int sensorRotation;


    //信号量控制，防止在相机关闭前应用退出
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    // 打印器
    Logger logger = Logger.getInstance();

    // 旋转方向的集合
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    private Camera2Helper(SurfaceView view) {
        surfaceView = view;
    }

    public static Camera2Helper getInstance(SurfaceView view) {
        if (camera2Helper == null) {
            synchronized (Camera2Helper.class) {
                if (camera2Helper == null) {
                    camera2Helper = new Camera2Helper(view);
                }
            }
        }
        return camera2Helper;
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(tag, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }


    public void init(Context context) {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        mainHandler = new Handler(context.getMainLooper());
        childHandler = new Handler(handlerThread.getLooper());
        mContext = context.getApplicationContext();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void initCamera2(Context context) {
        init(context);
        File dir = FileHelper.getInstance(mContext).getDiskCacheDir(mContext, "Camera2");
        if (!dir.exists())
            dir.mkdirs();
        storePath = dir.getPath();
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            //遍历所有摄像头
            for (String cameraId : mCameraManager.getCameraIdList()) {
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                sensorRotation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
                // 默认打开后置摄像头，如果是前置摄像头就跳过
                if (mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == LENS_FACING_FRONT)
                    continue;
                mCameraId = cameraId;
                break;
            }
            open(mCameraId);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void open(String id) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            PermissionUtil.verifyPermissions(mContext, new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        try {
            mCameraManager.openCamera(id, stateCallback, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            mCameraOpenCloseLock.release();
            startPreview();
            logger.debug(tag, "摄像头打开成功");
        }


        @Override
        public void onDisconnected(CameraDevice camera) {
            mCameraOpenCloseLock.release();
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            logger.debug(tag, "摄像头断开连接");
        }

        @Override
        public void onError(CameraDevice camera, int error) {
//            mCameraOpenCloseLock.release();
            logger.debug(tag, "相机开启失败,错误码：" + error);
        }
    };

    /**
     * 处理拍照得到的临时照片
     */
    public void setupImageReader(SurfaceView view) {
        mImageReader = ImageReader.newInstance(view.getWidth(), view.getHeight(),
                ImageFormat.JPEG, 1);
        //在这里处理拍照得到的临时照片,拍照请求发送后回调该方法，对照片进行处理
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                // 拿到拍照获得的图片
                Image image = reader.acquireNextImage();
                // 获取图片的像素数组放入缓冲区
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                bytes = new byte[buffer.remaining()];
                // 由缓冲区存入字节数组
                buffer.get(bytes);
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                EventBus.getDefault().post(bitmap);
                image.close();
            }
        }, mainHandler);
    }

    public void startPreview() {
        setupImageReader(surfaceView);
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将SurfaceView的surface作为CaptureRequest.Builder的目标
            mPreviewRequestBuilder.addTarget(surfaceView.getHolder().getSurface());
            //设置自动曝光
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //设置自动对焦
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
                /**
                 *  当相机已经设置好了自身，会话（session)可以开始处理捕获请求时调用onConfigured方法
                 * @param session
                 */
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    // 显示预览是在子线程中进行，这就是为什么要创建handlerThread的原因
                    if (null == mCameraDevice) return;
                    // 当摄像头已经准备好时，开始显示预览
                    mCameraCaptureSession = session;
                    try {
                        // 预览请求，设置预览时连续捕获图像数据
                        mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                                null, childHandler);
                        // 打印显示当前线程名为Camera2，正是创建HandlerThread时设置的名字
                        logger.debug(tag, "相机配置成功");
                        logger.debug("Camera2Demo", "Current Thread: " + Thread.currentThread().getName());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                    if (mCameraCaptureSession != null) {
                        mCameraCaptureSession.close();
                    }
                    Toast.makeText(mContext, "配置失败", Toast.LENGTH_SHORT).show();
                    Log.d(tag, "配置失败");
                }
            };

            mCameraDevice.createCaptureSession(Arrays.asList(surfaceView.getHolder().getSurface(),
                    mImageReader.getSurface()), stateCallback, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //适应屏幕，按宽高比3:4来设置
    public void adjustScreenSize(SurfaceView surfaceView) {
        int orientation = mContext.getResources().getConfiguration().orientation;
        float width = surfaceView.getWidth();
        float height = surfaceView.getHeight();
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            //以width为准求需要的height
            float requestHeight = 4 * width / 3;
            surfaceView.setScaleX(height / requestHeight);
            surfaceView.setScaleY(1);
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            float requestWidth = 4 * height / 3;
            surfaceView.setScaleY(width / requestWidth);
            surfaceView.setScaleX(1);
        }
    }

    /**
     * 拍照方法
     */
    public void takePhoto(int rotation) {
        if (mCameraDevice != null) {
            try {
                CaptureRequest.Builder takePhotoBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                // 将SurfaceView的surface作为CaptureRequest.Builder的目标
                takePhotoBuilder.addTarget(mImageReader.getSurface());
                // 设置自动曝光
                takePhotoBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                // 设置自动对焦
                takePhotoBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 获取手机设备的方向,根据设备方向计算并设置照片的方向
                takePhotoBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                        ORIENTATIONS.get(rotation));
                // 拍照
                mCameraCaptureSession.capture(takePhotoBuilder.build(), null, childHandler);

                Toast.makeText(mContext, "拍照", Toast.LENGTH_SHORT).show();
                Logger.getInstance().debug(tag, "拍照");
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 设置MediaRecorder
     */
    //参考的谷歌官方demo的设置顺序,顺序很重要
    private void setUpMediaRecorder(int rotation) {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.reset();

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(storePath);


        mMediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
        mMediaRecorder.setVideoFrameRate(30);
        /**
         * 录制视频尺寸设置要正确，如果太大，很有可能录制失败。
         * 表现为画面静止，录得的视频大小为0kb，停止录制时程序崩溃退出
         */
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());

        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);


        if (mCameraId.equals("0")) {
            mMediaRecorder.setOrientationHint(90);
        } else {
            mMediaRecorder.setOrientationHint(270);
        }
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(tag, "Camera2 has set MediaRecorder VideoSize:" + mVideoSize);
        Log.i(tag, "Camera2 OutputFilePath:" + storePath);

        mMediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Log.d(tag, "MediaRecorder error:" + what + "-" + extra);
            }
        });

    }

    public void startRecord(String path, int rotation) {
        if (TextUtils.isEmpty(path)) {
            Log.d(tag, "Camera2 Record path is empty");
            return;
        }
        closePreviewSession();
        storePath = path;
        setUpMediaRecorder(rotation);
        try {
            final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            Surface previewSurface = surfaceView.getHolder().getSurface();
            builder.addTarget(previewSurface);
            Surface recordSurface = mMediaRecorder.getSurface();
            builder.addTarget(recordSurface);
            mCaptureRequest = builder.build();

            List<Surface> surfaces = Arrays.asList(previewSurface, recordSurface);
            //   List<Surface> surfaces = Arrays.asList(previewSurface);

            //摄像
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mPreviewCaptureSession = session;
                    //设置反复捕获数据的请求，这样预览界面就会一直有数据显示
                    try {
                        mPreviewCaptureSession.setRepeatingRequest(mCaptureRequest, null, childHandler);
                        mMediaRecorder.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    logger.debug(tag, "视频录制失败");
                    Toast.makeText(mContext, "视频录制失败", Toast.LENGTH_SHORT).show();
                }
            }, childHandler);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(tag, "Camera2 startRecord failed:" + e.getMessage());
        }

    }

    //清除预览Session
    public void closePreviewSession() {
        if (mPreviewCaptureSession != null) {
            mPreviewCaptureSession.close();
            mPreviewCaptureSession = null;
        }

        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }

    /**
     * 结束录制
     */
    public void stopRecord() {
        try {
            //解决startPreview failed:Illegal state encountered in camera service
            //https://stackoverflow.com/questions/27907090/android-camera-2-api
            if (mPreviewCaptureSession != null) {
                mPreviewCaptureSession.stopRepeating();
                mPreviewCaptureSession.abortCaptures();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.release();
            mMediaRecorder = null;
            Log.d(tag, "Camera2 has stop record");
        }
        // startPreview();
        releaseCamera2();
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            mCameraManager.openCamera(mCameraId, stateCallback, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 计算该手机合适的照片尺寸
     */
    public void fitPhotoSize() {
        // 获取指定摄像头的特性
        CameraCharacteristics characteristics = null;
        try {
            characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // 获取摄像头支持的最大尺寸
            List<Size> sizes = Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
            int minIndex = 0;//差距最小的索引
            int minDx = Integer.MAX_VALUE;
            int minDy = Integer.MAX_VALUE;
            int[] dxs = new int[sizes.size()];
            int justW = mWinSize.getHeight() * 2;//相机默认是横向的，so
            int justH = mWinSize.getWidth() * 2;
            for (int i = 0; i < sizes.size(); i++) {
                dxs[i] = sizes.get(i).getWidth() - justW;
            }
            for (int i = 0; i < dxs.length; i++) {
                int abs = Math.abs(dxs[i]);
                if (abs < minDx) {
                    minIndex = i;//获取高的最适索引
                    minDx = abs;
                }
            }
            for (int i = 0; i < sizes.size(); i++) {
                Size size = sizes.get(i);
                if (size.getWidth() == sizes.get(minIndex).getWidth()) {
                    int dy = Math.abs(justH - size.getHeight());
                    if (dy < minDy) {
                        minIndex = i;//获取宽的最适索引
                        minDy = dy;
                    }
                }
            }

            justSize = sizes.get(minIndex);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开闪光灯
     *
     * @constant FLASH_MODE_TORCH 一直打开闪光灯
     * @constant FLASH_MODE_OFF 关闭闪光灯
     */

    public void openFlashLight(ImageView imageView) {
        if (!isFlashLightOn) {
            imageView.setImageResource(R.drawable.flash_on);
            imageView.setImageTintList(ColorStateList.valueOf(0xffEFB90F));
        } else {
            imageView.setImageResource(R.drawable.flash_off);
            imageView.setImageTintList(ColorStateList.valueOf(0xffffffff));
        }
        isFlashLightOn = !isFlashLightOn;

        try {
            CaptureRequest.Builder openFlashLightBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            openFlashLightBuilder.addTarget(surfaceView.getHolder().getSurface());
            // 设置自动曝光
            openFlashLightBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            // 根据标志位设置闪光灯是否打开
            openFlashLightBuilder.set(CaptureRequest.FLASH_MODE,
                    isFlashLightOn ? CameraMetadata.FLASH_MODE_TORCH : CameraMetadata.FLASH_MODE_OFF);

            mCameraCaptureSession.setRepeatingRequest(openFlashLightBuilder.build(), null, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 缩放预览图像
     */
    public void zoomPreview(TextView textView) {
        if (mScale > 2.0f) {
            mScale = 1f;
        }
        String rate = mScale + "x";
        textView.setText(rate);
        try {
            CaptureRequest.Builder zoomPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            zoomPreviewBuilder.addTarget(surfaceView.getHolder().getSurface());
            zoomPreviewBuilder.set(CaptureRequest.SCALER_CROP_REGION,
                    new Rect(0, 0, (int) (surfaceView.getWidth() / mScale), (int) (surfaceView.getHeight() / mScale)));
            mCameraCaptureSession.setRepeatingRequest(zoomPreviewBuilder.build(), null, childHandler);
            mScale += 0.25;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 切换摄像头注意一定要及时在CameraDevice的StateCallback中
     * 释放mCameraOpenCloseLock信号量，否则只能切换一次
     *
     * @param id
     */
    public void switchCamera(int id) {
        mCameraId = String.valueOf(id);
        releaseCamera2();
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
//            mCameraManager.openCamera(id + "", stateCallback, childHandler);
        open(mCameraId);
    }

    /**
     * 保存拍得的图片到本地
     */

    public void saveImage() {
        //将图片保存到文件中
        File dir = FileHelper.getInstance(mContext).getDiskCacheDir(mContext, "Camera2");
        if (!dir.exists())
            dir.mkdirs();
        File file = new File(dir, StringUtil.getCurrentTime_yyyyMMddHHmmss() + ".jpg");
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Toast.makeText(mContext, "保存照片" + storePath + "/Camera2", Toast.LENGTH_SHORT).show();
            logger.debug(tag, "照片储存在：" + storePath + "/Camera2");
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            startPreview();
        }
    }

    /**
     * 关闭当前相机
     */
    public void releaseCamera2() {
        try {
            mCameraOpenCloseLock.acquire();
            if (mCameraCaptureSession != null) {
                mCameraCaptureSession.close();
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
            }
            if (mImageReader != null) {
                mImageReader.close();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }

    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    public void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            CaptureRequest.Builder mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

}
