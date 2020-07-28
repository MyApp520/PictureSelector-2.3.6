package com.xhdz.customcamera.activity;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import com.common.fragment.FullScreenDialogFragment;
import com.eighteengray.procameralibrary.common.TakePictureActionType;
import com.luck.picture.lib.PictureBaseActivity;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.R;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.tools.SdkVersionUtils;
import com.xhdz.customcamera.camera.camera1.Camera1Helper;
import com.yalantis.ucrop.model.CutInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用camera1 api完成相机功能
 */
public class XhCustomSurfaceViewCameraActivity extends PictureBaseActivity {

    private SurfaceView mSurfaceView;
    private ImageView mImageViewTakePicture;

    private SurfaceHolder mSurfaceHolder;
    private Camera1Helper mCamera1Helper;

    private int mSurfaceWidth, mSurfaceHeight;

    private FullScreenDialogFragment mFullScreenDialogFragment;

    @Override
    public int getResourceId() {
        return R.layout.activity_xh_custom_surface_view_camera;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
        mSurfaceView = findViewById(R.id.xh_surfaceView);
        mImageViewTakePicture = findViewById(R.id.iv_xh_take_picture);

        setImageViewTakePictureClickListener();
        initSurfaceView();
        initCamera1Helper();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: ");
        if (mImageViewTakePicture != null && mCamera1Helper != null) {
            mCamera1Helper.initCamera(getApplicationContext(), mSurfaceHolder);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: ");
        if (mImageViewTakePicture != null && mCamera1Helper != null) {
            mCamera1Helper.release();
        }
    }

    private void setImageViewTakePictureClickListener() {
        mImageViewTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCamera1Helper != null) {
                    mCamera1Helper.takePicture();
                }
            }
        });
    }

    private void initSurfaceView() {
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(mSurfaceHolderCallback);
        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: 点击画面了");
            }
        });
    }

    private void initCamera1Helper() {
        mCamera1Helper = new Camera1Helper();
        mCamera1Helper.setCameraPictureCallback(mCameraPictureCallback);
    }

    /**
     * SurfaceHolder状态监听回调
     */
    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.e(TAG, "surfaceCreated: ");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (mSurfaceWidth <= 0 || mSurfaceHeight <= 0) {
                Log.e(TAG, "surfaceChanged: width = " + width + ", height = " + height);
                mSurfaceWidth = width;
                mSurfaceHeight = height;
                mCamera1Helper.startPreview(true);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.e(TAG, "surfaceDestroyed: ");
            mSurfaceWidth = 0;
        }
    };

    /**
     * camera1相机数据回调
     */
    private Camera.PictureCallback mCameraPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.e(TAG, "onTakePictureResult: 拍照数据回调");
            if (data != null && data.length > 10) {
                mFullScreenDialogFragment = FullScreenDialogFragment.newInstance();
                mFullScreenDialogFragment.setPictureBytes(data);
                mFullScreenDialogFragment.show(getSupportFragmentManager(), FullScreenDialogFragment.class.getSimpleName());
            }
        }
    };

    /**
     * @param actionType  操作类型
     * @param picturePath 拍照图片的存储路径
     */
    @Override
    public void needFinish(int actionType, String picturePath) {
        super.needFinish(actionType, picturePath);
        if (TakePictureActionType.ACTION_CANCEL == actionType) {
            // 重新开启预览
            mCamera1Helper.startPreview(false);
        } else if (TakePictureActionType.ACTION_MANUAL_SELECT_FACE == actionType) {
            // 手动扣脸
            ArrayList<CutInfo> cuts = new ArrayList<>();
            CutInfo cutInfo = new CutInfo();
            if (SdkVersionUtils.checkedAndroid_Q()) {
                cutInfo.setPath("file://" + picturePath);
            } else {
                cutInfo.setPath(picturePath);
            }
            cuts.add(cutInfo);

            startCropList(cuts);
            mImageViewTakePicture = null;
        } else if (TakePictureActionType.ACTION_AUTO_SELECT_FACE == actionType) {
            // 自动扣脸
            List<LocalMedia> result = new ArrayList<>();
            LocalMedia media = new LocalMedia();
            media.setPath(picturePath);
            result.add(media);

            setResult(RESULT_OK, PictureSelector.putIntentResult(result));
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mCamera1Helper != null) {
                mCamera1Helper.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mCameraPictureCallback = null;
        if (mSurfaceHolder != null) {
            mSurfaceHolder.removeCallback(mSurfaceHolderCallback);
        }
    }
}
