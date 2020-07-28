package com.eighteengray.procameralibrary.activity;

import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.common.fragment.FullScreenDialogFragment;
import com.eighteengray.procameralibrary.camera.Camera2TextureView;
import com.eighteengray.procameralibrary.camera.TextureViewTouchEvent;
import com.eighteengray.procameralibrary.common.Constants;
import com.eighteengray.procameralibrary.common.TakePictureActionType;
import com.eighteengray.procameralibrary.dataevent.ImageAvailableEvent;
import com.eighteengray.procameralibrary.widget.FocusView;
import com.eighteengray.procameralibrary.widget.ImageSaver;
import com.eighteengray.procameralibrary.widget.TextureViewTouchListener;
import com.luck.picture.lib.PictureBaseActivity;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.R;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.tools.SdkVersionUtils;
import com.luck.picture.lib.tools.ToastUtils;
import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropMulti;
import com.yalantis.ucrop.model.CutInfo;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class ProCustomCameraActivity extends PictureBaseActivity {

    private Camera2TextureView cameraTextureView;
    private FocusView mFocusView;

    private TextureViewTouchListener textureViewTouchListener;
    private ImageView ivTakePicture;

    private float mRawX, mRawY; //触摸聚焦时候的中心点

    /**
     * 聚焦图像是否显示的标志位
     */
    private boolean mFlagShowFocusImage = false;

    /**
     * 当前拍照后的图片路径
     */
    private String mPicturePath;

    private FullScreenDialogFragment mFullScreenDialogFragment;

    @Override
    public int getResourceId() {
        return R.layout.activity_pro_custom_camera;
    }

    @Override
    protected void initWidgets() {
        super.initWidgets();
        EventBus.getDefault().register(this);

        cameraTextureView = findViewById(R.id.cameraTextureView);
        ivTakePicture = findViewById(R.id.iv_take_picture);
        mFocusView = findViewById(R.id.focusview_camera2);

        setIvTakePictureOnClickListener();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: ");
        if (ivTakePicture != null) {
            openCamera();
        }
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause: ");
        closeCamera();
        super.onPause();
    }

    private void setIvTakePictureOnClickListener() {
        ivTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 点击拍照
                cameraTextureView.takePicture();
            }
        });
    }

    private void openCamera() {
        if (cameraTextureView != null) {
            cameraTextureView.openCamera();
            textureViewTouchListener = new TextureViewTouchListener(cameraTextureView);
            cameraTextureView.setOnTouchListener(textureViewTouchListener);
        }
    }

    private void closeCamera() {
        if (cameraTextureView != null) {
            cameraTextureView.closeCamera();
            textureViewTouchListener = null;
            cameraTextureView.setOnTouchListener(null);
        }
    }

    /**
     * @param actionType  操作类型
     * @param picturePath 拍照图片的存储路径
     */
    @Override
    public void needFinish(int actionType, String picturePath) {
        super.needFinish(actionType, picturePath);
        mPicturePath = picturePath;
        if (TakePictureActionType.ACTION_CANCEL == actionType) {
            // 取消操作
            mPicturePath = null;
        } else if (TakePictureActionType.ACTION_MANUAL_SELECT_FACE == actionType) {
            // 手动扣脸
            ArrayList<CutInfo> cuts = new ArrayList<>();
            CutInfo cutInfo = new CutInfo();
            if (SdkVersionUtils.checkedAndroid_Q()) {
                cutInfo.setPath("file://" + mPicturePath);
            } else {
                cutInfo.setPath(mPicturePath);
            }
            cuts.add(cutInfo);

            startCropList(cuts);
            ivTakePicture = null;
        } else if (TakePictureActionType.ACTION_AUTO_SELECT_FACE == actionType) {
            // 自动扣脸
            List<LocalMedia> result = new ArrayList<>();
            LocalMedia media = new LocalMedia();
            media.setPath(mPicturePath);
            result.add(media);

            setResult(RESULT_OK, PictureSelector.putIntentResult(result));
            finish();
        }
    }

    /**
     * 存储图像完成后，拿到 pictureBitmap
     */
    private ImageSaver.SaveImageDataListener mSaveImageDataListener = new ImageSaver.SaveImageDataListener() {
        @Override
        public void getImageByteData(byte[] pictureByteData) {
            Log.e(TAG, "getImageBitmap: 线程 = " + Thread.currentThread().getName());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFullScreenDialogFragment = FullScreenDialogFragment.newInstance();
                    mFullScreenDialogFragment.setPictureBytes(pictureByteData);
                    mFullScreenDialogFragment.show(getSupportFragmentManager(), FullScreenDialogFragment.class.getSimpleName());
                }
            });
        }
    };

    //EventBus--TextureView触摸事件
    @Subscribe(threadMode = ThreadMode.MAIN)  //轻按：显示焦点，完成聚焦和测光。
    public void onTextureClick(TextureViewTouchEvent.TextureClick textureClick) throws CameraAccessException {
        mRawX = textureClick.getRawX();
        mRawY = textureClick.getRawY();
        cameraTextureView.focusRegion(textureClick.getX(), textureClick.getY());
    }

    @Subscribe(threadMode = ThreadMode.MAIN) // 单指滑动，如果是向右下则进度环增加，否则减小，用于调节焦点白平衡。
    public void onTextureOneDrag(TextureViewTouchEvent.TextureOneDrag textureOneDrag) {
        mFocusView.dragChangeAWB(textureOneDrag.getDistance());
    }


    //聚焦的四种状态，对应的显示的View
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onShowFocus(TextureViewTouchEvent.FocusState focusState) {
        switch (focusState.getFocusState()) {
            case Constants.FOCUS_FOCUSING:
                if (mFlagShowFocusImage == false) {
                    //聚焦图片显示在手点击的位置
                    if (mRawX == 0 || mRawY == 0) {
                        mRawX = cameraTextureView.getMeasuredWidth() / 2;
                        mRawY = cameraTextureView.getMeasuredHeight() / 2;
                    }
                    mFocusView.showFocusing(mRawX, mRawY, textureViewTouchListener);
                    mFlagShowFocusImage = true;
                }
                break;

            case Constants.FOCUS_SUCCEED:
                if (mFlagShowFocusImage == true) {
                    mFocusView.showFocusSucceed(textureViewTouchListener);
                    mFlagShowFocusImage = false;
                }
                break;

            case Constants.FOCUS_INACTIVE:
                mFocusView.setVisibility(View.GONE);
                mFlagShowFocusImage = false;
                break;

            case Constants.FOCUS_FAILED:
                if (mFlagShowFocusImage == true) {
                    mFocusView.showFocusFailed();
                    mFlagShowFocusImage = false;
                }
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN) //拍照完成后，拿到ImageReader，然后做保存图片的操作
    public void onImageReaderAvailable(ImageAvailableEvent.ImageReaderAvailable imageReaderAvailable) {

        new Thread(new ImageSaver(imageReaderAvailable.getImageReader(), getApplicationContext(), mSaveImageDataListener)).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case UCropMulti.REQUEST_MULTI_CROP:
                    multiCropHandleResult(data);
                    break;
                default:
                    break;
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable throwable = (Throwable) data.getSerializableExtra(UCrop.EXTRA_ERROR);
            ToastUtils.s(getContext(), throwable.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        try {
            closeCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (mFullScreenDialogFragment != null && mFullScreenDialogFragment.isVisible()) {
                mFullScreenDialogFragment.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
