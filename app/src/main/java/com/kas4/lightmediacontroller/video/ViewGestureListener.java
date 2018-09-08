package com.kas4.lightmediacontroller.video;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.WindowManager;


public class ViewGestureListener implements GestureDetector.OnGestureListener {

    private static final String TAG = "ViewGestureListener";

    //    private static final int SWIPE_THRESHOLD = 60;//threshold of swipe
    private static final int SWIPE_THRESHOLD = 5;//threshold of swipe
    public static final int SWIPE_LEFT = 1;
    public static final int SWIPE_RIGHT = 2;
    private VideoGestureListener listener;
    private Context context;

    public ViewGestureListener(Context context, VideoGestureListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        listener.onSingleTap();
        return false;
    }

    boolean firstScroll = false;

    @Override
    public boolean onDown(MotionEvent e) {
        firstScroll = true;
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float deltaX = e2.getX() - e1.getX();
        float deltaY = e2.getRawY() - e1.getY();

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            // by zj
//            if (Math.abs(deltaX) > SWIPE_THRESHOLD) {
//            listener.onHorizontalScroll(e2, deltaX);
            if (Math.abs(distanceX) > SWIPE_THRESHOLD) {
                listener.onHorizontalScroll(e2, -distanceX, firstScroll);
                firstScroll = false;
                return true;
            }

        } else {
            if (Math.abs(distanceY) > SWIPE_THRESHOLD) {
                if (e1.getX() < getDeviceWidth(context) * 2.0 / 5) {//left edge
                    listener.onVerticalScroll(e2, distanceY, SWIPE_LEFT);
                } else if (e1.getX() > getDeviceWidth(context) * 3.0 / 5) {//right edge
                    listener.onVerticalScroll(e2, distanceY, SWIPE_RIGHT);
                }
                firstScroll = false;
                return true;
            }

            // by zj
//            if (Math.abs(deltaY) > SWIPE_THRESHOLD) {
//                if (e1.getX() < getDeviceWidth(context) * 1.0 / 5) {//left edge
//                    Log.e("-deltaY", "" + -deltaY);
//                    listener.onVerticalScroll(e2, -deltaY * 0.2f, SWIPE_LEFT);
//                } else if (e1.getX() > getDeviceWidth(context) * 4.0 / 5) {//right edge
//                    Log.e("-deltaY", "" + -deltaY);
//                    listener.onVerticalScroll(e2, -deltaY * 0.5f, SWIPE_RIGHT);
//                }
//                firstScroll = false;
//                return true;
//            }
        }
        firstScroll = false;
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }


    @Override
    public void onLongPress(MotionEvent e) {

    }


    @Override
    public void onShowPress(MotionEvent e) {

    }

    public static int getDeviceWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(mDisplayMetrics);
        return mDisplayMetrics.widthPixels;
    }

    public static int getDeviceHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(mDisplayMetrics);
        return mDisplayMetrics.heightPixels;
    }

}
