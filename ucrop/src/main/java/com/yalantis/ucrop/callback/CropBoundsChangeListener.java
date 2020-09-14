package com.yalantis.ucrop.callback;

import android.graphics.RectF;

/**
 * Interface for crop bound change notifying.
 */
public interface CropBoundsChangeListener {

    /**
     * @param cropRatio
     * @param cropRect  当前裁剪框四个点的坐标
     */
    void onCropAspectRatioChanged(float cropRatio, RectF cropRect);

}