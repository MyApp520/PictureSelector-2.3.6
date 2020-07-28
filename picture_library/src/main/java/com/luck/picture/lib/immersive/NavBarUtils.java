package com.luck.picture.lib.immersive;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.view.Window;

/**
 * @author：luck
 * @date：2019-11-25 20:58
 * @describe：NavBar工具类
 */
public class NavBarUtils {
    /**
     * 动态设置 NavBar 色值
     *
     * @param activity
     * @param color
     */
    public static void setNavBarColor(@NonNull final Activity activity, @ColorInt final int color) {
        setNavBarColor(activity.getWindow(), color);
    }

    public static void setNavBarColor(@NonNull final Window window, @ColorInt final int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(color);
        }
    }
}
