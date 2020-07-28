package com.common.fragment;


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.eighteengray.procameralibrary.common.TakePictureActionType;
import com.eighteengray.procameralibrary.util.DataConvertUtil;
import com.eighteengray.procameralibrary.util.FileUtils;
import com.luck.picture.lib.PictureBaseActivity;
import com.luck.picture.lib.R;

import java.io.File;

/**
 * 全屏dialog
 */
public class FullScreenDialogFragment extends DialogFragment implements View.OnClickListener
        , DialogInterface.OnKeyListener, DialogInterface.OnDismissListener {

    private final String TAG = getClass().getSimpleName();

    private PictureBaseActivity mPictureBaseActivity;

    /**
     * 设置图片
     */
    private byte[] mPictureByteData;
    private String mPicturePath;
    private Bitmap mPictureBitmap;

    private ImageView mImageView;
    private Button btn_cancel, btn_manual_select_face, btn_auto_select_face;

    private long currentClickTimeMillis;
    private long lastClickTimeMillis;

    public static FullScreenDialogFragment newInstance() {
        // Required empty public constructor
        FullScreenDialogFragment fullScreenDialogFragment = new FullScreenDialogFragment();
        Bundle bundle = new Bundle();
        fullScreenDialogFragment.setArguments(bundle);
        return fullScreenDialogFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPictureBaseActivity = (PictureBaseActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_full_screen_dialog, container, false);

        mImageView = rootView.findViewById(R.id.image_view);
        btn_cancel = rootView.findViewById(R.id.btn_cancel);
        btn_manual_select_face = rootView.findViewById(R.id.btn_manual_select_face);
        btn_auto_select_face = rootView.findViewById(R.id.btn_auto_select_face);

        btn_cancel.setOnClickListener(FullScreenDialogFragment.this);
        btn_manual_select_face.setOnClickListener(FullScreenDialogFragment.this);
        btn_auto_select_face.setOnClickListener(FullScreenDialogFragment.this);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        super.onActivityCreated(savedInstanceState);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0x00000000));
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

        getDialog().setOnKeyListener(this);
        getDialog().setOnDismissListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e(TAG, "onResume: 准备显示照片");
        if (mPictureBitmap == null) {
            mPictureBitmap = DataConvertUtil.bytes2Bimap(mPictureByteData);
            mImageView.setImageBitmap(mPictureBitmap);
        }
    }

    /**
     * 设置图片
     *
     * @param data
     */
    public void setPictureBytes(byte[] data) {
        this.mPictureByteData = data;
    }

    private void createPicturePath() {
        // 创建图片file文件的路径
        mPicturePath = mPictureBaseActivity.getApplicationContext().getExternalFilesDir(null)
                + File.separator + "pictureSelector" + File.separator + System.currentTimeMillis() + ".jpg";
        FileUtils.writeByte2File(mPictureByteData, mPicturePath);
        Log.e(TAG, "getImageBitmap: mPicturePath = " + mPicturePath);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (R.id.btn_cancel == viewId) {
            mPictureBaseActivity.needFinish(TakePictureActionType.ACTION_CANCEL, mPicturePath);
        } else if (R.id.btn_manual_select_face == viewId) {
            createPicturePath();
            mPictureBaseActivity.needFinish(TakePictureActionType.ACTION_MANUAL_SELECT_FACE, mPicturePath);
        } else if (R.id.btn_auto_select_face == viewId) {
            createPicturePath();
            mPictureBaseActivity.needFinish(TakePictureActionType.ACTION_AUTO_SELECT_FACE, mPicturePath);
        }
        dismiss();
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            currentClickTimeMillis = System.currentTimeMillis();
            if (currentClickTimeMillis - lastClickTimeMillis > 500) {
                Log.e(TAG, "onKey: 请求关闭图片 DialogFragment");
                btn_cancel.performClick();// 模拟btn_cancel控件点击事件，会调用btn_cancel控件的onClick监听事件
                lastClickTimeMillis = currentClickTimeMillis;
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPictureBitmap != null) {
            mPictureBitmap.recycle();
            mPictureBitmap = null;
        }
        mPictureByteData = null;
    }
}
