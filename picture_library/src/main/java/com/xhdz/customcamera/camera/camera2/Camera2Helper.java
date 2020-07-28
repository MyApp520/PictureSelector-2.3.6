package com.xhdz.customcamera.camera.camera2;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.common.listeners.TakePictureCallBackListener;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Camera2Helper {

    private final String TAG = getClass().getSimpleName();

    private Activity mActivity;
    private Context mContext;

    private HandlerThread mHandlerThread;
    private Handler mCameraHandler;

    /**
     * 拍照结果监听
     */
    private TakePictureCallBackListener mTakePictureCallBackListener;

    /**
     * 当前使用的camera对应的id
     */
    private String mCurrentUseCameraId;

    /**
     * 当前使用的camera对应的CameraCharacteristics
     */
    private CameraCharacteristics mCurrentUseCharacteristics;

    /**
     * 前置相机
     */
    private String frontCameraId;

    /**
     * 后置相机
     */
    private String backCameraId;

    /**
     * 预览界面尺寸大小
     */
    private Size mPreviewSize;

    private CameraManager mCameraManager;

    /**
     * CameraDevice 代表当前连接的相机设备，它的职责有以下四个：
     * <p>
     * 根据指定的参数创建 CameraCaptureSession。
     * 根据指定的模板创建 CaptureRequest。
     * 关闭相机设备。
     * 监听相机设备的状态，例如断开连接、开启成功和开启失败等。
     */
    private CameraDevice mCurrentCameraDevice;

    /**
     * CameraDevice 的功能则十分的单一，就是只负责建立相机连接的事务，而更加细化的相机操作则交给了CameraCaptureSession。
     * 一个 CameraDevice 一次只能开启一个 CameraCaptureSession，
     * 绝大部分的相机操作都是通过向 CameraCaptureSession 提交一个 Capture 请求实现的，
     * <p>
     * 注意：1、CameraCaptureSession是相机(摄像头)的操作对象；
     * 2、CameraCaptureSession负责相机所有的具体操作，例如：拍照、连拍、设置闪光灯模式、触摸对焦、显示预览画面等等。
     */
    private CameraCaptureSession mCameraCaptureSession;

    /**
     * 相机预览 管理对象
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * 相机拍照 管理对象
     */
    private CaptureRequest.Builder mCaptureRequestBuilder;

    /**
     * ImageReader用于读取相机数据
     */
    private ImageReader mImageReader;

    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    /**
     * 用于接收预览画面的 Surface
     */
    private Surface mPreviewSurface;

    private Surface mImageReaderSurface;


    private void initCameraThreadHandler() {
        // 创建线程
        mHandlerThread = new HandlerThread("cameraThread");
        // 启动线程，HandlerThread对象start后可以获得其Looper对象
        mHandlerThread.start();

        // 将mHandlerThread.getLooper()传给mCameraHandler，之后mCameraHandler就运行在 mHandlerThread 线程中了
        mCameraHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                // 注：消息处理操作 handleMessage() 方法的执行线程 = mHandlerThread线程
            }
        };
    }

    public void initCameraManager(Activity activity, TextureView textureView) {
        if (activity == null) {
            throw new NullPointerException("Camera2Helper 必须设置 activity");
        }
        mActivity = activity;
        mContext = mActivity.getApplicationContext();
        mTextureView = textureView;
        mSurfaceTexture = mTextureView.getSurfaceTexture();

        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

        getCameraId();
        collectCameraOutPutSizes();
        openCamera();
    }

    /**
     * 相机拍照（CameraCaptureSession负责相机所有的具体操作，例如：拍照、连拍、设置闪光灯模式、触摸对焦、显示预览画面等等。）
     */
    public void takePicture() {
        try {
            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), mCameraCaptureSessionCaptureCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始相机预览（CameraCaptureSession负责相机所有的具体操作，例如：拍照、连拍、设置闪光灯模式、触摸对焦、显示预览画面等等。）
     */
    private void startCameraPreview() {
        try {
            // 调用setRepeatingRequest方法，请求不断重复捕获图像，即实现预览
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCameraCaptureSessionCaptureCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getCameraId() {
        try {
            // CameraCharacteristics 是相机信息的提供者
            CameraCharacteristics cameraCharacteristics;
            for (String cameraId : mCameraManager.getCameraIdList()) {
                cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);

                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == null) {
                    continue;
                }

                if (CameraCharacteristics.LENS_FACING_FRONT == cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)) {
                    frontCameraId = cameraId;
                } else if (CameraCharacteristics.LENS_FACING_BACK == cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)) {
                    backCameraId = cameraId;
                }
            }
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "getCameraId: frontCameraId = " + frontCameraId + ",  backCameraId = " + backCameraId);
        mCurrentUseCameraId = TextUtils.isEmpty(backCameraId) ? frontCameraId : backCameraId;
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        AndPermission.with(mContext)
                .runtime()
                .permission(Permission.CAMERA, Permission.WRITE_EXTERNAL_STORAGE)
                .onGranted(new Action<List<String>>() {
                    @SuppressLint("MissingPermission")
                    @Override
                    public void onAction(List<String> data) {
                        try {
                            mCameraManager.openCamera(mCurrentUseCameraId, mCameraDeviceStateCallback, null);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .onDenied(new Action<List<String>>() {
                    @Override
                    public void onAction(List<String> data) {
                        Toast.makeText(mContext, "没有相机操作权限，请退出重新进入", Toast.LENGTH_SHORT).show();
                    }
                })
                .start();
    }

    /**
     * 获取相机支持的预览尺寸:
     * <p>
     * 1、通过 CameraCharacteristics 获取相机支持的预览尺寸列表。
     * <p>
     * 2、所谓的预览尺寸，指的就是相机把画面输出到手机屏幕上供用户预览的尺寸。
     * <p>
     * 3、一个比较特别的情况，就是预览尺寸的宽是长边，高是短边，例如 1920x1080，而不是 1080x1920，这是因为相机 Sensor 的宽是长边，而高是短边。
     * <p>
     * 4、在获取适合的预览尺寸之后，接下来就是配置预览尺寸使其生效了
     */
    private void collectCameraOutPutSizes() {
        try {
            mCurrentUseCharacteristics = mCameraManager.getCameraCharacteristics(mCurrentUseCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        StreamConfigurationMap streamConfigurationMap = mCurrentUseCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (streamConfigurationMap == null) {
            throw new IllegalStateException("Failed to get configuration map: " + mCurrentUseCameraId);
        }
        // StreamConfigurationMap.getOutputSizes() 方法获取尺寸列表
        // 一个比较特别的情况，就是预览尺寸的宽是长边，高是短边，例如 1920x1080，而不是 1080x1920，这是因为相机 Sensor 的宽是长边，而高是短边。
        Size[] getOutputSizesArray = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
        for (Size size : getOutputSizesArray) {
            if (size.getWidth() == 1920 && size.getHeight() == 1080) {
                mPreviewSize = size;
            }
        }
        Log.e(TAG, "collectCameraOutPutSizes: mPreviewSize = " + mPreviewSize);
    }

    /**
     * ImageReader用于读取相机数据
     *
     * @param largest
     */
    private void initImageReader(Size largest) {
        if (largest == null) {
            largest = mPreviewSize;
        }
        mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /* maxImages */ 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);
        mImageReaderSurface = mImageReader.getSurface();
    }

    /**
     * 创建相机预览管理对象(注意：是在 mCameraCaptureSessionStateCallback 监听器里启动相机预览)
     */
    private void createPreviewRequestBuilder() {
        // 设置预览尺寸
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

        // 创建用于接收预览画面的 Surface
        mPreviewSurface = new Surface(mSurfaceTexture);

        try {
            mPreviewRequestBuilder = mCurrentCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 使用CaptureRequest.Builder.addTarget()，配置一个 Surface（任何相机操作的本质都是为了捕获图像
            // 并且配置的 Surface 必须要和创建 createCaptureSession 时添加的那些 Surface
            // 你可以多次调用该方法添加多个Surface
            // 注意：至少要添加一个Surface
            mPreviewRequestBuilder.addTarget(mPreviewSurface);
            // 设置自动对焦
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建相机拍照管理对象
     */
    private void createCaptureRequestBuilder() {
        try {
            mCaptureRequestBuilder = mCurrentCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mPreviewSurface);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());

            // 自动对焦
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 闪光灯
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 根据摄像头方向对保存的照片进行旋转，使其为"自然方向"
//            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mCameraSensorOrientation);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建CameraCaptureSession：相机操作对象
     */
    private void createCameraCaptureSession() {
        try {
            // 创建 CameraCaptureSession
            // CameraCaptureSession负责具体的相机操作，比如：拍照、连拍、设置闪光灯模式、触摸对焦、显示预览画；
            // mCameraCaptureSessionStateCallback负责监听mCameraCaptureSession的状态
            // 注意：是在mCameraCaptureSessionStateCallback监听器里启动相机预览
            List<Surface> surfaceList = new ArrayList<>();
            surfaceList.add(mPreviewSurface);
            surfaceList.add(mImageReaderSurface);
            mCurrentCameraDevice.createCaptureSession(surfaceList, mCameraCaptureSessionStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 相机状态监听
     */
    private final CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.e(TAG, "相机状态监听 onOpened:");
            mCurrentCameraDevice = camera;
            // ImageReader用于读取相机数据
            initImageReader(null);
            // 创建相机预览管理对象
            createPreviewRequestBuilder();
            // 创建相机拍照管理对象
            createCaptureRequestBuilder();
            // 创建CameraCaptureSession：该对象负责操作相机
            createCameraCaptureSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.e(TAG, "相机已断开 onDisconnected:");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "相机状态监听 onError: error = " + error);
            try {
                camera.close();
                mCurrentCameraDevice.close();
                mCurrentCameraDevice = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * 监听CameraCaptureSession的状态
     */
    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "mCameraCaptureSessionStateCallback onConfigured: session = " + session);
            mCameraCaptureSession = session;
            // 开始相机预览
            startCameraPreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "mCameraCaptureSessionStateCallback onConfigureFailed: session = " + session);
        }

        @Override
        public void onReady(@NonNull CameraCaptureSession session) {
            super.onReady(session);
            Log.e(TAG, "mCameraCaptureSessionStateCallback onReady: session = " + session);
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
            Log.e(TAG, "mCameraCaptureSessionStateCallback onClosed: session = " + session);
        }

        @Override
        public void onSurfacePrepared(@NonNull CameraCaptureSession session, @NonNull Surface surface) {
            super.onSurfacePrepared(session, surface);
            Log.e(TAG, "onSurfacePrepared: session = " + session);
        }
    };

    /**
     * 对相机进行某个操作(比如：开启预览，开启拍照等等)后，都会回调到这个接口来
     */
    private CameraCaptureSession.CaptureCallback mCameraCaptureSessionCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            Log.i(TAG, "onCaptureCompleted: request = " + request);
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.e(TAG, "onImageAvailable: 准备解析相机数据了");
            Image image = null;
            byte[] data = null;
            try {
                image = reader.acquireNextImage();
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    data = new byte[buffer.remaining()];
                    buffer.get(data);
                }
                if (data == null) {
                    Log.e(TAG, "onImageAvailable: 获取到相机拍照的数据了 data == null");
                }
                if (mTakePictureCallBackListener != null) {
                    mTakePictureCallBackListener.onTakePictureResult(data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    };

    public void setTakePictureCallBackListener(TakePictureCallBackListener takePictureCallBackListener) {
        mTakePictureCallBackListener = takePictureCallBackListener;
    }

    /**
     * 释放资源
     */
    public void release() {
        if (mPreviewSurface != null) {
            mPreviewSurface.release();
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        if (mImageReaderSurface != null) {
            mImageReaderSurface.release();
        }
        if (mImageReader != null) {
            mImageReader.close();
        }
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
        }
        if (mCurrentCameraDevice != null) {
            mCurrentCameraDevice.close();
        }
    }
}
