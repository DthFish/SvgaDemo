package com.dthfish.svgademo;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.WindowManager;

/**
 * Description
 * Author DthFish
 * Date  2020/5/22.
 */
public class AppUtils {

    public static Point getScreenSize(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point pt = new Point();
        manager.getDefaultDisplay().getSize(pt);
        return pt;
    }


    public static Point getRealScreenSize(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Point pt = new Point();
            manager.getDefaultDisplay().getRealSize(pt);
            return pt;
        } else {
            return getScreenSize(context);
        }
    }
}
