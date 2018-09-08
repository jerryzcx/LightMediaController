package com.kas4.lightmediacontroller.video;


import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.kas4.lightmediacontroller.R;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

public class VideoControllerView extends FrameLayout implements VideoGestureListener {

    private static final String TAG = "VideoControllerView";

    private static final int HANDLER_ANIMATE_OUT = 1;// out animate
    private static final int HANDLER_UPDATE_PROGRESS = 2;//cycle update progress
    private static long PROGRESS_SEEK = 1000;
    private MediaPlayerControlListener mPlayer;// control media play
    private Activity mContext;
    private ViewGroup mAnchorView;//anchor view
    private View mRootView; // root view of this
    private SeekBar mSeekBar;
    private TextView mEndTime, mCurrentTime;
    private boolean mShowing;//controller view showing?
    private boolean mDragging;
    private StringBuilder mFormatBuilder;
    private Formatter mFormatter;
    private GestureDetector mGestureDetector;
    private VideoGestureListener mVideoGestureListener;
    //top layout
    private ImageButton mBackButton;
    private TextView mTitleText;
    //center layout
    private float mCurBrightness;
    private float mCurVolume;
    private AudioManager mAudioManager;
    private int mMaxVolume;
    //bottom layout
    private ImageButton mPauseButton;
    private Handler mHandler = new ControllerViewHandler(this);
    // by zj
    private Button btn_resol0;
    private Button btn_resol1;
    private SeekBar right_seekbar;
    private TextView mNextTitle;

    public VideoControllerView(Activity context, AttributeSet attrs) {
        super(context, attrs);
        mRootView = null;
        mContext = context;
        Log.i(TAG, TAG);
    }

    public VideoControllerView(Activity context, boolean useFastForward) {
        super(context);
        mContext = context;
        Log.i(TAG, TAG);
    }

    public VideoControllerView(Activity context) {
        this(context, true);
        Log.i(TAG, TAG);
    }


    /**
     * Handler prevent leak memory.
     */
    static class ControllerViewHandler extends Handler {
        private final WeakReference<VideoControllerView> mView;

        ControllerViewHandler(VideoControllerView view) {
            mView = new WeakReference<VideoControllerView>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            VideoControllerView view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            int pos;
            switch (msg.what) {
                case HANDLER_ANIMATE_OUT:
                    view.hide();
//                    ((FullPlayActivity)view.mContext).showNextTitle(true);
                    break;
                case HANDLER_UPDATE_PROGRESS://cycle update seek bar progress
                    pos = view.setSeekProgress();
                    if (!view.mDragging && view.mShowing && view.mPlayer.isPlaying()) {//just in case
                        //cycle update
                        msg = obtainMessage(HANDLER_UPDATE_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    }

    /**
     * init controller view
     *
     * @return
     */
    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRootView = inflate.inflate(R.layout.view_media_controller, null);
        initControllerView(mRootView);

        return mRootView;
    }


    final int MAX_PROGRESS_SEEKBAR = 1000;// by zj TODO 很多地方需要替换


    private void initControllerView(View v) {
        //top layout
        mBackButton = (ImageButton) v.findViewById(R.id.top_back);
        if (mBackButton != null) {
            mBackButton.requestFocus();
            mBackButton.setOnClickListener(mBackListener);
        }
        v.findViewById(R.id.zoom_out).setOnClickListener(mBackListener);

        mTitleText = (TextView) v.findViewById(R.id.top_title);
        mNextTitle = (TextView) v.findViewById(R.id.next_title);
        mPauseButton = (ImageButton) v.findViewById(R.id.bottom_pause);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mSeekBar = (SeekBar) v.findViewById(R.id.bottom_seekbar);
        if (mSeekBar != null) {
            SeekBar seeker = (SeekBar) mSeekBar;
            seeker.setOnSeekBarChangeListener(mSeekListener);
            mSeekBar.setMax(MAX_PROGRESS_SEEKBAR);
        }
        right_seekbar = (SeekBar) v.findViewById(R.id.right_seekbar);
        if (right_seekbar != null) {
            SeekBar seeker = (SeekBar) right_seekbar;
            seeker.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mAudioManager != null)
                        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, seekBar.getProgress(), 0);

                }
            });
            setSeekbarVolume();
        }

        mEndTime = (TextView) v.findViewById(R.id.bottom_time);
        mCurrentTime = (TextView) v.findViewById(R.id.bottom_time_current);

        //init formatter
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

        // by zj
        btn_resol0 = (Button) v.findViewById(R.id.btn_resol0);
        btn_resol1 = (Button) v.findViewById(R.id.btn_resol1);
    }

    void setSeekbarVolume() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        }
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        right_seekbar.setMax(mMaxVolume);

        mCurVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (mCurVolume < 0) {
            mCurVolume = 0;
        }

        right_seekbar.setProgress((int) mCurVolume);
    }

    public View getBtnResol0() {
        return btn_resol0;
    }

    public View getBtnResol1() {
        return btn_resol1;
    }

    /**
     * show controller view
     */
    private void show() {
        if (!mShowing && mAnchorView != null) {

            //animate anchorview when layout changes
            //equals android:animateLayoutChanges="true"
            mAnchorView.setLayoutTransition(new LayoutTransition());
            setSeekProgress();
            // by zj
            setSeekbarVolume();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
                if (!mPlayer.canPause()) {
                    mPauseButton.setEnabled(false);
                }
            }

            //add controller view to bottom of the AnchorView
            LayoutParams tlp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
//            (int) (mContext.getResources().getDisplayMetrics().density * 45)
            mAnchorView.addView(this, tlp);
            mShowing = true;//set view state
        }
        //Log.e(TAG,"togglePausePlay 271");
        togglePausePlay();
        //update progress
        mHandler.sendEmptyMessage(HANDLER_UPDATE_PROGRESS);
//        ((FullPlayActivity)mContext).showNextTitle(false);
    }

    /**
     * show controller view
     */
    private void show(boolean ignorePause) {
        if (!mShowing && mAnchorView != null) {

            //animate anchorview when layout changes
            //equals android:animateLayoutChanges="true"
            mAnchorView.setLayoutTransition(new LayoutTransition());
            setSeekProgress();
            // by zj
            setSeekbarVolume();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
                if (!mPlayer.canPause()) {
                    mPauseButton.setEnabled(false);
                }
            }

            //add controller view to bottom of the AnchorView
            LayoutParams tlp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
//            (int) (mContext.getResources().getDisplayMetrics().density * 45)
            mAnchorView.addView(this, tlp);
            mShowing = true;//set view state
        }
        //Log.e(TAG,"togglePausePlay 271");
        if (!ignorePause)
            togglePausePlay();
        //update progress
        mHandler.sendEmptyMessage(HANDLER_UPDATE_PROGRESS);

    }

    /**
     * Control if show controllerview
     */
    public void toggleContollerView() {
        if (!isShowing()) {
            show();
        } else {
            //animate out controller view
            Message msg = mHandler.obtainMessage(HANDLER_ANIMATE_OUT);
            mHandler.removeMessages(HANDLER_ANIMATE_OUT);
            mHandler.sendMessageDelayed(msg, 100);
        }
    }

    private void animateOut() {
        TranslateAnimation trans = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1);
        trans.setInterpolator(new AccelerateInterpolator());
        setAnimation(trans);
    }

    /**
     * get isShowing?
     *
     * @return
     */
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * hide controller view with animation
     * Just use LayoutTransition
     * mAnchorView.setLayoutTransition(new LayoutTransition());
     * equals android:animateLayoutChanges="true"
     */
    public void hide() {
        if (mAnchorView == null) {
            return;
        }

        try {
            mAnchorView.removeView(this);
            mHandler.removeMessages(HANDLER_UPDATE_PROGRESS);
        } catch (IllegalArgumentException ex) {
            Log.w("MediaController", "already removed");
        }
        mShowing = false;
    }

    /**
     * convert string to time
     *
     * @param timeMs
     * @return
     */
    private String stringToTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    /**
     * set seekbar progress
     *
     * @return
     */
    private int setSeekProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }

        // by zj 更新音量
        setSeekbarVolume();

        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mSeekBar != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mSeekBar.setProgress((int) pos);
            }
            //get buffer percentage
            int percent = mPlayer.getBufferPercentage();
            //set buffer progress
            mSeekBar.setSecondaryProgress(percent * 10);
        }

        int lastTime = duration - position;
        if (mEndTime != null)
            mEndTime.setText(stringToTime(lastTime));// by zj
        if (mCurrentTime != null)
            mCurrentTime.setText(stringToTime(position));

        mTitleText.setText(mPlayer.getTopTitle());

        return position;
    }


    private int changeSeekProgress(long change) {
        if (mPlayer == null) {
            return 0;
        }

//        // by zj 更新音量
//        setSeekbarVolume();


        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        long change_progress = (long) (change * 1.0f / duration * 1000L);
        mSeekBar.setProgress((int) (mSeekBar.getProgress() + change_progress));
//        if (mSeekBar != null) {
//            if (duration > 0) {
//                // use long to avoid overflow
//                long pos = 1000L * position / duration;
//                mSeekBar.setProgress((int) pos);
//            }
//            //get buffer percentage
//            int percent = mPlayer.getBufferPercentage();
//            //set buffer progress
//            mSeekBar.setSecondaryProgress(percent * 10);
//        }

//        int lastTime = duration - position;
//        if (mEndTime != null)
//            mEndTime.setText(stringToTime(lastTime));// by zj
//        if (mCurrentTime != null)
//            mCurrentTime.setText(stringToTime(position));

//        mTitleText.setText(mPlayer.getTopTitle());
        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isScrollProgress) {
                isScrollProgress = false;
                long duration = mPlayer.getDuration();
                long newposition = (duration * mSeekBar.getProgress()) / 1000L;
                mPlayer.seekTo((int) newposition);
            }
            mDragging = false;
            setSeekProgress();
            //Log.e(TAG,"togglePausePlay 480");
            togglePausePlay();
            show();
            mHandler.sendEmptyMessage(HANDLER_UPDATE_PROGRESS);
        }
        if (null != mGestureDetector) {
            mGestureDetector.onTouchEvent(event);
        }
//        toggleContollerView();
        return true;
    }

    /**
     * toggle pause or play
     */
    private void togglePausePlay() {
        if (mRootView == null || mPauseButton == null || mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(R.drawable.pause);
            //Log.e(TAG, " to pause 403");
        } else {
            mPauseButton.setImageResource(R.drawable.play);
            //Log.e(TAG, " to play 406");
        }
    }

    private void doPauseResume() {
        if (mPlayer == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            mPauseButton.setImageResource(R.drawable.play);// by zj
            //Log.e(TAG," to play 431");
        } else {
            mPlayer.start();
            mPauseButton.setImageResource(R.drawable.pause);// by zj
            //Log.e(TAG, " to pause 435");
        }
        // by zj
//        togglePausePlay();
    }

    private void doToggleFullscreen() {
        if (mPlayer == null) {
            return;
        }

        mPlayer.toggleFullScreen();
    }

    /**
     * Seek bar drag listener
     */
    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            show();
            mDragging = true;
            mHandler.removeMessages(HANDLER_UPDATE_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mPlayer == null) {
                return;
            }

            if (!fromuser) {
                return;
            }

//            long duration = mPlayer.getDuration();
//            long newposition = (duration * progress) / 1000L;
//            mPlayer.seekTo((int) newposition);
//            if (mCurrentTime != null)
//                mCurrentTime.setText(stringToTime((int) newposition));
//
//            long lastTime = duration - newposition;
//            if (mEndTime != null)
//                mEndTime.setText(stringToTime((int) lastTime));

        }

        public void onStopTrackingTouch(SeekBar bar) {
            // by zj
            long duration = mPlayer.getDuration();
            long newposition = (duration * bar.getProgress()) / 1000L;
            mPlayer.seekTo((int) newposition);
            // end
            mDragging = false;
            setSeekProgress();
            //Log.e(TAG,"togglePausePlay 480");
            togglePausePlay();
            show();
            mHandler.sendEmptyMessage(HANDLER_UPDATE_PROGRESS);
        }
    };

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mSeekBar != null) {
            mSeekBar.setEnabled(enabled);
        }
        super.setEnabled(enabled);
    }


    /**
     * set top back click listener
     */
    private OnClickListener mBackListener = new OnClickListener() {
        public void onClick(View v) {
            mPlayer.exit();
        }
    };


    /**
     * set pause click listener
     */
    private OnClickListener mPauseListener = new OnClickListener() {
        public void onClick(View v) {

            doPauseResume();
            // by zj
            show(true);
        }
    };

    /**
     * set full screen click listener
     */
    private OnClickListener mFullscreenListener = new OnClickListener() {
        public void onClick(View v) {
            doToggleFullscreen();
            show();
        }
    };

    /**
     * setMediaPlayerControlListener update play state
     *
     * @param player self
     */
    public void setMediaPlayerControlListener(MediaPlayerControlListener player) {
        mPlayer = player;
        //Log.e(TAG,"togglePausePlay 538");
        togglePausePlay();
    }

    /**
     * set anchor view
     *
     * @param view view that hold controller view
     */
    public void setAnchorView(ViewGroup view) {
        mAnchorView = view;
        LayoutParams frameParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        //remove all before add view
        removeAllViews();
//        setBackgroundColor(Color.BLUE);
        View v = makeControllerView();
        addView(v, frameParams);
    }

    public void showNextTitle(String title, boolean isShow){
        if (isShow) {
            mNextTitle.setText(title);
            mNextTitle.setVisibility(View.VISIBLE);
        } else {
            mNextTitle.setVisibility(View.GONE);
        }

    }

    /**
     * set gesture listen to control media player
     * include screen brightness and volume of video
     *
     * @param context
     */
    public void setGestureListener(Context context) {
        mVideoGestureListener = this;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mGestureDetector = new GestureDetector(context, new ViewGestureListener(context, mVideoGestureListener));
    }


    //implement ViewGestureListener
    @Override
    public void onSingleTap() {
        toggleContollerView();
    }

    boolean isScrollProgress = false;

    @Override
    public void onHorizontalScroll(MotionEvent event, float delta, boolean firstScroll) {
        if (mPlayer == null) {
            return;
        }
        if (event.getPointerCount() == 1 && mPlayer.canSeekProgress()) {
            isScrollProgress = true;
            if (firstScroll) {
                mDragging = true;
                mHandler.removeMessages(HANDLER_UPDATE_PROGRESS);
                int sec = mPlayer.getDuration() / 1000;
                if (sec <= 20) {
                    PROGRESS_SEEK = 1000;
                } else if (sec <= 120) {
                    PROGRESS_SEEK = 2000;
                } else {
                    PROGRESS_SEEK = 5000;
                }
            }

//        // by zj
//        if (delta > 0) {// seek forward
//            seekForWard();
//        } else {  //seek backward
//            seekBackWard();
//        }

            mSeekBar.setProgress((int) (mSeekBar.getProgress() + delta));
        }

    }


    @Deprecated
    private void seekBackWard() {
        if (mPlayer == null) {
            return;
        }

        int pos = mPlayer.getCurrentPosition();
        pos -= PROGRESS_SEEK;
//        mPlayer.seekTo(pos);
        changeSeekProgress(-PROGRESS_SEEK);
// by zj
//        show();
    }

    @Deprecated
    private void seekForWard() {
        if (mPlayer == null) {
            return;
        }

        int pos = mPlayer.getCurrentPosition();
        pos += PROGRESS_SEEK;
//        mPlayer.seekTo(pos);
        changeSeekProgress(PROGRESS_SEEK);
// by zj
//        show();
    }

    @Override
    public void onVerticalScroll(MotionEvent motionEvent, float delta, int direction) {
        if (motionEvent.getPointerCount() == 1) {
// by zj
//               if(direction == ViewGestureListener.SWIPE_LEFT){
//                   mCenterImage.setImageResource(R.drawable.video_bright_bg);
//                   updateBrightness(delta);
//               }else {
//                   mCenterImage.setImageResource(R.drawable.video_volume_bg);
//                   updateVolume(delta);
//               }
//               postDelayed(new Runnable() {
//                   @Override
//                   public void run() {
//                       mCenterLayout.setVisibility(GONE);
//                   }
//               },1000);

            if (direction == ViewGestureListener.SWIPE_LEFT) {
                updateBrightness(delta);
            } else {
                updateVolume(delta);
            }


        }
    }

    private void updateVolume(float delta) {

//        mCenterLayout.setVisibility(VISIBLE);
        mCurVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (mCurVolume < 0) {
            mCurVolume = 0;
        }

        int change = (int) (delta / (ViewGestureListener.getDeviceHeight(mContext) * 0.8f) * mMaxVolume);
        if (change <= 0) {
            if (delta > 0) {
                change = 1;
            } else if (delta < 0) {
                change = -1;
            }
        }
        //Log.e("mCurVolume:", "" + mCurVolume);
        //Log.e("delta:", "" + delta);
        int volume = (int) (mCurVolume + change);
//        int volume = (int) (delta / (ViewGestureListener.getDeviceHeight(mContext) * 0.8f) * mMaxVolume + mCurVolume);
        if (volume > mMaxVolume) {
            volume = mMaxVolume;
        }
//        Log.e("zj", "change:" + change);
//        Log.e("zj", "mCurVolume:" + mCurVolume);
//        Log.e("zj", "delta:" + delta);
//        Log.e("zj", "mMaxVolume:" + mMaxVolume);
//        Log.e("zj", "volume:" + volume);
//        Log.e("zj", "-------------------------:" + ViewGestureListener.getDeviceHeight(mContext));

        if (volume < 0) {
            volume = 0;
        }
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);

//        float percent = (float) ((volume * 1.0 / mMaxVolume) * 100);
        //Log.e("volume:", "" + volume);
        //Log.e("percent:", "" + percent);
//        mCenterPorgress.setProgress((int) percent);
        right_seekbar.setProgress(volume);
    }

    /**
     * update brightness
     *
     * @param delta
     */
    private void updateBrightness(float delta) {
        // by zj
//        mCenterLayout.setVisibility(VISIBLE);

        mCurBrightness = mContext.getWindow().getAttributes().screenBrightness;
        if (mCurBrightness <= 0.01f) {
            mCurBrightness = 0.01f;
        }


        WindowManager.LayoutParams attributes = mContext.getWindow().getAttributes();
        attributes.screenBrightness = mCurBrightness + delta / (ViewGestureListener.getDeviceHeight(mContext) * 0.8f);
        if (attributes.screenBrightness >= 1.0f) {
            attributes.screenBrightness = 1.0f;
        } else if (attributes.screenBrightness <= 0.01f) {
            attributes.screenBrightness = 0.01f;
        }
        mContext.getWindow().setAttributes(attributes);

        float percent = attributes.screenBrightness * 100;
//        mCenterPorgress.setProgress((int) percent);

    }

//end of ViewGestureListener

    /**
     * Interface of Media Controller View
     */
    public interface MediaPlayerControlListener {
        /**
         * start play video
         */
        void start();

        /**
         * pause video
         */
        void pause();

        /**
         * get video total time
         *
         * @return
         */
        int getDuration();

        /**
         * get current position
         *
         * @return
         */
        int getCurrentPosition();

        /**
         * seek to position
         *
         * @param pos
         */
        void seekTo(int pos);

        /**
         * video is playing state
         *
         * @return
         */
        boolean isPlaying();

        /**
         * get buffer date
         *
         * @return
         */
        int getBufferPercentage();

        /**
         * if the video can pause
         *
         * @return
         */
        boolean canPause();

        /**
         * can seek video progress
         *
         * @return
         */
        boolean canSeekProgress();

        /**
         * video is full screen
         * in order to control image src...
         *
         * @return
         */
        boolean isFullScreen();

        /**
         * toggle fullScreen
         */
        void toggleFullScreen();

        /**
         * exit media player
         */
        void exit();

        /**
         * get top title name
         */
        String getTopTitle();
    }

}