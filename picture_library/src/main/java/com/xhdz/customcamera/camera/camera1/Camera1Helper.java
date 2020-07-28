package com.xhdz.customcamera.camera.camera1;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

public class Camera1Helper {

    private final String TAG = getClass().getSimpleName();

    private Context mContext;

    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private Camera.Parameters mCameraParameters;
    private SurfaceHolder mSurfaceHolder;

    /**
     * 相机拍照回调接口
     */
    private Camera.PictureCallback mCameraPictureCallback;

    /**
     * 当前操作的相机对应的id
     */
    private int currentCameraId;

    private int faceBackCameraId;
    private int faceBackCameraOrientation;

    private int faceFrontCameraId;
    private int faceFrontCameraOrientation;

    /**
     * 相机的方向（有：0，90，180，270），默认设置为：0
     */
    private int mDisplayOrientation = 0;

    public void initCamera(Context context, SurfaceHolder surfaceHolder) {
        mContext = context;
        mSurfaceHolder = surfaceHolder;

        getCamera();
        getCameraId();
    }

    /**
     * 获取系统相机
     *
     * @return
     */
    private Camera getCamera() {
        try {
            mCamera = Camera.open();
            // 获取相机参数
            mCameraParameters = mCamera.getParameters();
            // 获取相机其他信息
            mCameraInfo = new Camera.CameraInfo();
            // 获取相机信息
            Camera.getCameraInfo(currentCameraId, mCameraInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mCamera;
    }

    private void getCameraId() {
        //有多少个摄像头
        int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

            Camera.getCameraInfo(i, cameraInfo);
            //后置摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                faceBackCameraId = i;
                faceBackCameraOrientation = cameraInfo.orientation;
            }
            //前置摄像头
            else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                faceFrontCameraId = i;
                faceFrontCameraOrientation = cameraInfo.orientation;
            }
        }

        currentCameraId = faceBackCameraId;
    }

    /**
     * 获取camera1支持的预览尺寸大小和拍照图片大小
     */
    private void getSupportedSizes() {
        // 支持的预览尺寸大小
        List<Camera.Size> supportedPreviewSizes = mCameraParameters.getSupportedPreviewSizes();
        for (Camera.Size previewSize : supportedPreviewSizes) {
            Log.e(TAG, "supportedPreviewSize: = " + previewSize.width + " x " + previewSize.height);
        }

        Log.e(TAG, "-------------------------------------------------------------------------------------------------");

        // 支持的拍照图片大小
        List<Camera.Size> supportedPictureSizes = mCameraParameters.getSupportedPictureSizes();
        for (Camera.Size pictureSize : supportedPictureSizes) {
            Log.e(TAG, "supportedPictureSize: = " + pictureSize.width + " x " + pictureSize.height);
        }
    }

    /**
     * @return true: 则mCamera != null
     */
    private boolean isCameraOpened() {
        return mCamera != null;
    }

    public void startPreview(boolean isFirst) {
        if (!isCameraOpened()) {
            return;
        }
        try {
            if (isFirst) {
                // 设置surfaceHolder
                // 重点：应该在调用完 Camera#setPreviewDisplay 方法绑定好 SurfaceHolder 之后。再通过 Camera#setDisplayOrientation 来改变其方向。
                try {
                    mCamera.setPreviewDisplay(mSurfaceHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.e(TAG, "takePicture: mDisplayOrientation = " + mDisplayOrientation);
                // 设置相片格式
                mCameraParameters.setPictureFormat(ImageFormat.JPEG);
                // 设置预览大小，必须是相机支持的宽高大小
                mCameraParameters.setPreviewSize(1920, 1080);
                // 设置拍照大小，必须是相机支持的宽高大小
                mCameraParameters.setPictureSize(1920, 1080);
                // 设置对焦方式，这里开启自动对焦
                setAutoFocusInternal(true);
                // 设置拍照图片的方向
                mCameraParameters.setRotation(calcCameraRotation(mDisplayOrientation));
                // 设置预览界面的方向
                mCamera.setDisplayOrientation(calcDisplayOrientation(mDisplayOrientation));

                // 设置相机参数
                mCamera.setParameters(mCameraParameters);
                Log.e(TAG, "startPreview: mCameraInfo.orientation = " + mCameraInfo.orientation);
            } else {
                mCamera.cancelAutoFocus();
            }
            // 开启预览
            mCamera.startPreview();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void stopPreview() {
        if (isCameraOpened()) {
            mCamera.cancelAutoFocus();
            mCamera.stopPreview();
        }
    }

    public void takePicture() {
        if (!isCameraOpened()) {
            Log.e(TAG, "takePicture无法进行拍照: mCamera == null");
            return;
        }

        mCamera.cancelAutoFocus();
        mCamera.autoFocus(new Camera.AutoFocusCallback() {

            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                // 判断是否对焦成功
                Log.e(TAG, "onAutoFocus: 判断是否对焦成功 = " + success);
                if (success) {
                    // 拍照 第三个参数为拍照回调
                    mCamera.takePicture(null, null, mCameraPictureCallback);
                }
            }
        });
    }

    /**
     * 设置对焦方式
     *
     * @param autoFocus 是否启用自动对焦的方式
     * @return
     */
    private boolean setAutoFocusInternal(boolean autoFocus) {
        if (isCameraOpened()) {
            final List<String> modes = mCameraParameters.getSupportedFocusModes();
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                // 用于拍摄照片的连续自动对焦模式。 相机不断尝试对焦
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            } else {
                mCameraParameters.setFocusMode(modes.get(0));
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Calculate display orientation
     * https://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
     * <p>
     * This calculation is used for orienting the preview
     * <p>
     * Note: This is not the same calculation as the camera rotation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees required to rotate preview
     */
    private int calcDisplayOrientation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (mCameraInfo.orientation + screenOrientationDegrees) % 360) % 360;
        } else {  // back-facing
            return (mCameraInfo.orientation - screenOrientationDegrees + 360) % 360;
        }
    }

    /**
     * Calculate camera rotation
     * <p>
     * This calculation is applied to the output JPEG either via Exif Orientation tag
     * or by actually transforming the bitmap. (Determined by vendor camera API implementation)
     * <p>
     * Note: This is not the same calculation as the display orientation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees to rotate image in order for it to view correctly.
     */
    private int calcCameraRotation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (mCameraInfo.orientation + screenOrientationDegrees) % 360;
        } else {  // back-facing
            final int landscapeFlip = isLandscape(screenOrientationDegrees) ? 180 : 0;
            return (mCameraInfo.orientation + screenOrientationDegrees + landscapeFlip) % 360;
        }
    }

    /**
     * Test if the supplied orientation is in landscape.
     *
     * @param orientationDegrees Orientation in degrees (0,90,180,270)
     * @return True if in landscape, false if portrait
     */
    private boolean isLandscape(int orientationDegrees) {
        return orientationDegrees == 90 || orientationDegrees == 270;
    }

    public void setCameraPictureCallback(Camera.PictureCallback cameraPictureCallback) {
        mCameraPictureCallback = cameraPictureCallback;
    }

    public void release() {
        stopPreview();
        try {
            if (mCamera != null) {
                mCamera.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCamera = null;
    }
}
