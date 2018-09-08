package com.kas4.lightmediacontroller.video;

import android.view.MotionEvent;


public interface VideoGestureListener {
    /**
     * single tap controller view
     */
    void onSingleTap();

    /**
     * Horizontal scroll to control progress of video
     * @param event
     * @param delta
     * @param firstScroll
     */
    void onHorizontalScroll(MotionEvent event, float delta, boolean firstScroll);

    /**
     * vertical scroll listen
     * @param motionEvent
     * @param delta
     * @param direction  left or right edge for control brightness or volume
     */
    void onVerticalScroll(MotionEvent motionEvent, float delta, int direction);
}
