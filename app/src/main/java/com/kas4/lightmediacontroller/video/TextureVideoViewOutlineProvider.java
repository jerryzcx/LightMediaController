package com.kas4.lightmediacontroller.video;

import android.annotation.TargetApi;
import android.graphics.Outline;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * Created by wesafari on 2017/5/4.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class TextureVideoViewOutlineProvider extends ViewOutlineProvider {
    @Override
    public void getOutline(View view, Outline outline) {

//        Rect rect = new Rect();
//
//        view.getGlobalVisibleRect(rect);
//        int leftMargin = 0;
//        int topMargin = 0;
//
//        Rect selfRect = new Rect(leftMargin, topMargin,
//                rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin);

//        outline.setRoundRect(selfRect, 500);
        int w = view.getWidth();
        int h = view.getHeight();
        outline.setOval(0, 0, w>h?h:w, w>h?h:w);
    }
}
