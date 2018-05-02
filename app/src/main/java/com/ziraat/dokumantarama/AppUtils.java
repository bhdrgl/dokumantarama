package com.ziraat.dokumantarama;

import android.app.Activity;
import android.graphics.Point;
import android.view.Display;

/**
 * Created by bhdr on 24/12/2017.
 */

public class AppUtils {

    public static int getScreenWidth(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return size.x;
    }

    public static int getScreenHeight(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return size.y;
    }
}
