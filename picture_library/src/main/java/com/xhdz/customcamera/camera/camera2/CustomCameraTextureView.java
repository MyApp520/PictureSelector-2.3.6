package com.xhdz.customcamera.camera.camera2;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

public class CustomCameraTextureView extends TextureView {

    private Context mContext;

    public CustomCameraTextureView(Context context) {
        this(context, null);
    }

    public CustomCameraTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomCameraTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context.getApplicationContext();
    }
}
