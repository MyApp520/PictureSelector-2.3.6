package com.xhdz.customcamera.activity;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import com.common.fragment.FullScreenDialogFragment;
import com.common.listeners.TakePictureCallBackListener;
import com.eighteengray.procameralibrary.common.TakePictureActionType;
import com.luck.picture.lib.PictureBaseActivity;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.R;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.tools.SdkVersionUtils;
import com.xhdz.customcamera.camera.camera2.Camera2Helper;
import com.yalantis.ucrop.model.CutInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用camera2 api完成相机功能
 */
public class XhCustomTextureViewCameraActivity extends PictureBaseActivity {

    private TextureView mTextureView;
    private ImageView ivTakePicture;

    /**
     * 相机操作对象
     */
    private Camera2Helper mCamera2Helper;

    private FullScreenDialogFragment mFullScreenDialogFragment;

    @Override
    public int getResourceId() {
        return R.layout.activity_xh_custom_texture_view_camera;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();

        mTextureView = findViewById(R.id.xh_textureView);
        ivTakePicture = findViewById(R.id.iv_xh_take_picture);

        // 相机操作对象
        mCamera2Helper = new Camera2Helper();

        setIvTakePictureOnClickListener();
        setTakePictureCallBackListener();

        initTextureView();
    }

    private void initTextureView() {
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.e(TAG, "onSurfaceTextureAvailable: 相机界面可用了 width = " + width + ", height = " + height);
                mCamera2Helper.initCameraManager(XhCustomTextureViewCameraActivity.this, mTextureView);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.e(TAG, "onSurfaceTextureSizeChanged: 相机界面发生变化 width = " + width + ", height = " + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.e(TAG, "onSurfaceTextureDestroyed: 相机界面销毁");
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // 正常状态下，这条日志是会一直打印的
//                Log.e(TAG, "onSurfaceTextureUpdated: 相机界面正在更新");
            }
        });
    }

    private void setIvTakePictureOnClickListener() {
        ivTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 点击拍照
                Log.e(TAG, "onClick: 点击拍照按钮");
                mCamera2Helper.takePicture();
            }
        });
    }

    private void setTakePictureCallBackListener() {
        mCamera2Helper.setTakePictureCallBackListener(new TakePictureCallBackListener() {
            @Override
            public void onTakePictureResult(byte[] data) {
                Log.e(TAG, "onTakePictureResult: 拍照数据回调");
                if (data != null && data.length > 10) {
                    mFullScreenDialogFragment = FullScreenDialogFragment.newInstance();
                    mFullScreenDialogFragment.setPictureBytes(data);
                    mFullScreenDialogFragment.show(getSupportFragmentManager(), FullScreenDialogFragment.class.getSimpleName());
                }
            }
        });
    }

    /**
     * @param actionType  操作类型
     * @param picturePath 拍照图片的存储路径
     */
    @Override
    public void needFinish(int actionType, String picturePath) {
        super.needFinish(actionType, picturePath);
        if (TakePictureActionType.ACTION_CANCEL == actionType) {
            // 取消操作
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
            ivTakePicture = null;
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
        mCamera2Helper.setTakePictureCallBackListener(null);
        mCamera2Helper.release();
    }
}
