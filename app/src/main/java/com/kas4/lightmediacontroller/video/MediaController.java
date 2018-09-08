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
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kas4.lightmediacontroller.R;
import com.pili.pldroid.player.IMediaController;

import java.lang.ref.WeakReference;
import java.util.Locale;

/**
 * You can write a custom MediaController instead of this class
 * A MediaController widget must implement all the interface defined by com.pili.pldroid.player.IMediaController
 */
public class MediaController extends FrameLayout implements IMediaController, VideoGestureListener  {

    private boolean mUseFastForward;
    private MediaPlayerControl mPlayer;
    private Context mContext;
    private PopupWindow mWindow;
    private int mAnimStyle;
    private ViewGroup mAnchorView;
    private View mRoot;
    private TextView mEndTime, mCurrentTime;
    private boolean mShowing;
    private boolean mDragging;
    private boolean mInstantSeeking = true;
    private static int sDefaultTimeout = 5000;
    private static final int SEEK_TO_POST_DELAY_MILLIS = 200;
    private static long PROGRESS_SEEK = 1000;

    private static final int FADE_OUT = 1;// out animate
    private static final int SHOW_PROGRESS = 2;//cycle update progress
    private boolean mFromXml = false;

    private boolean mDisableProgress = false;

    private final int MAX_PROGRESS_SEEKBAR = 1000;


    private ImageButton mBackButton;
    private TextView mTitleText;
    private float mCurBrightness;
    private float mCurVolume;
    private AudioManager mAudioManager;
    private int mMaxVolume;

    private ImageButton mPauseButton;
    private ControllerViewHandler mHandler;
    // by zj
    private Button btn_resol0;
    private Button btn_resol1;
    private SeekBar right_seekbar;
    private SeekBar mSeekBar;
    private TextView mNextTitle;

    /**
     * set top back click listener
     */
    private OnClickListener mBackListener = new OnClickListener() {
        public void onClick(View v) {
            if (mPlayer != null) {
                mPlayer.exit();
            }
        }
    };
    private boolean isScrollProgress;
    private GestureDetector mGestureDetector;
    private Activity mActivity;

    public MediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRoot = this;
        mFromXml = true;
        initController(context);
    }

    public MediaController(Context context) {
        super(context);
        mHandler = new ControllerViewHandler(this);
        if (!mFromXml && initController(context)){
            //            initFloatingWindow();
        }
    }

    public MediaController(Context context, boolean useFastForward, boolean disableProgressBar) {
        this(context);
        mUseFastForward = useFastForward;
        mDisableProgress = disableProgressBar;
    }

    public MediaController(Context context, boolean useFastForward) {
        this(context);
        mUseFastForward = useFastForward;
    }

    private boolean initController(Context context) {
        mUseFastForward = true;
        mContext = context.getApplicationContext();
        return true;
    }

//    @Override
//    public void onFinishInflate() {
//        if (mRoot != null)
//            initControllerView(mRoot);
//        super.onFinishInflate();
//    }

    private void initFloatingWindow() {
        mWindow = new PopupWindow(mContext);
        mWindow.setFocusable(false);
        mWindow.setBackgroundDrawable(null);
        mWindow.setOutsideTouchable(true);
        mAnimStyle = android.R.style.Animation;
    }

    /**
     * Create the view that holds the widgets that control playback. Derived
     * classes can override this to create their own.
     *
     * @return The controller view.
     */
    protected View makeControllerView() {
        return ((LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.view_media_controller, null);
    }

    private void initControllerView(View v) {

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
            right_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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

        // by zj
        btn_resol0 = (Button) v.findViewById(R.id.btn_resol0);
        btn_resol1 = (Button) v.findViewById(R.id.btn_resol1);
    }

    private void setSeekbarVolume() {
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


    /**
     * Control the action when the seekbar dragged by user
     *
     * @param seekWhenDragging
     * True the media will seek periodically
     */
    public void setInstantSeeking(boolean seekWhenDragging) {
        mInstantSeeking = seekWhenDragging;
    }

    private void disableUnsupportedButtons() {
        try {
            if (mPauseButton != null && !mPlayer.canPause())
                mPauseButton.setEnabled(false);
        } catch (IncompatibleClassChangeError ex) {
        }
    }

    /**
     * <p>
     * Change the animation style resource for this controller.
     * </p>
     *
     * <p>
     * If the controller is showing, calling this method will take effect only
     * the next time the controller is shown.
     * </p>
     *
     * @param animationStyle
     * animation style to use when the controller appears and disappears.
     * Set to -1 for the default animation, 0 for no animation,
     * or a resource identifier for an explicit animation.
     *
     */
    public void setAnimationStyle(int animationStyle) {
        mAnimStyle = animationStyle;
    }


    public interface OnShownListener {
        public void onShown();
    }

    private OnShownListener mShownListener;

    public void setOnShownListener(OnShownListener l) {
        mShownListener = l;
    }

    public interface OnHiddenListener {
        public void onHidden();
    }

    private OnHiddenListener mHiddenListener;

    public void setOnHiddenListener(OnHiddenListener l) {
        mHiddenListener = l;
    }

    /**
     * Handler prevent leak memory.
     */
    static class ControllerViewHandler extends Handler {
        private final WeakReference<MediaController> mView;

        ControllerViewHandler(MediaController view) {
            mView = new WeakReference<MediaController>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaController view = mView.get();
            if (view == null || view.mPlayer == null) {
                return;
            }

            long pos;
            switch (msg.what) {
                case FADE_OUT:
                    view.hideController();
//                    ((FullPlayActivity)view.mContext).showNextTitle(true);
                    break;
                case SHOW_PROGRESS://cycle update seek bar progress
                    String nextTitle = view.mPlayer.getNextVideoTitle();
                    if (isEmpty(nextTitle)) {
                        view.mNextTitle.setVisibility(View.GONE);
                    } else {
                        view.mNextTitle.setText(nextTitle);
                        view.mNextTitle.setVisibility(View.VISIBLE);
                    }
                    pos = view.setSeekProgress();
                    if (!view.mDragging && view.mShowing && view.mPlayer.isPlaying()) {//just in case
                        //cycle update
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    }

    /**
     * set seekbar progress
     *
     * @return
     */
    private long setSeekProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }

        // by zj 更新音量
        setSeekbarVolume();

        long position = mPlayer.getCurrentPosition();
        long duration = mPlayer.getDuration();
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

        long lastTime = duration - position;
        if (mEndTime != null)
            mEndTime.setText(generateTime(lastTime));// by zj
        if (mCurrentTime != null)
            mCurrentTime.setText(generateTime(position));

        mTitleText.setText(mPlayer.getTopTitle());

        return position;
    }

    private static String generateTime(long position) {
        int totalSeconds = (int) (position / 1000);

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes,
                    seconds).toString();
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
                    .toString();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        show(sDefaultTimeout);
//        if (event.getAction() == MotionEvent.ACTION_UP){
//            if (mShowing) {
//                hideController();
//            } else {
//                showController();
//            }
//        }


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
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
        if (null != mGestureDetector) {
            mGestureDetector.onTouchEvent(event);
        }
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
//        show(sDefaultTimeout);
        if (mShowing) {
            hideController();
        } else {
            showController();
        }
        return true;
    }


    private OnClickListener mPauseListener = new OnClickListener() {
        public void onClick(View v) {
            doPauseResume();
//            show(sDefaultTimeout);
            showController();
        }
    };

    private void togglePausePlay() {
        if (mRoot == null || mPauseButton == null || mPlayer == null) {
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
        if (mPlayer.isPlaying()){
            mPlayer.pause();
            mPauseButton.setImageResource(R.drawable.play);
        } else {
            mPlayer.start();
            mPauseButton.setImageResource(R.drawable.pause);
        }
    }


    /**
     * Seek bar drag listener
     */
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        public void onStartTrackingTouch(SeekBar bar) {
            showController();
            mDragging = true;
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (mPlayer == null) {
                return;
            }

            if (!fromuser) {
                return;
            }

        }

        public void onStopTrackingTouch(SeekBar bar) {
            // by zj
            long duration = mPlayer.getDuration();
            long newposition = (duration * bar.getProgress()) / 1000L;
            mPlayer.seekTo(newposition);
            // end
            mDragging = false;
            setSeekProgress();
//            togglePausePlay();
            showController();
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };



    /**
     * Set the view that acts as the anchor for the control view.
     *
     * - This can for example be a VideoView, or your Activity's main view.
     * - AudioPlayer has no anchor view, so the view parameter will be null.
     *
     * @param view
     * The view to which to anchor the controller when it is visible.
     */
    @Override
    public void setAnchorView(View view) {
//        mAnchorView = view;
//        if (mAnchorView == null) {
//            sDefaultTimeout = 0; // show forever
//        }
//        if (!mFromXml) {
//            removeAllViews();
//            mRoot = makeControllerView();
//            mWindow.setContentView(mRoot);
//            mWindow.setWidth(LayoutParams.MATCH_PARENT);
//            mWindow.setHeight(LayoutParams.WRAP_CONTENT);
//        }

    }

    public void setAnchorView(ViewGroup v) {
        mAnchorView = v;
        LayoutParams frameParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        //remove all before add view
        removeAllViews();
        mRoot = makeControllerView();
        addView(mRoot, frameParams);

        initControllerView(mRoot);
    }

    /**
     * set gesture listen to control media player
     * include screen brightness and volume of video
     */
    public void setGestureListener(Activity activity) {
        mActivity = activity;
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mGestureDetector = new GestureDetector(mContext, new ViewGestureListener(mContext, this));
    }


    @Override
    public void setMediaPlayer(IMediaController.MediaPlayerControl player) {
        if (player instanceof  MediaPlayerControl) {
            mPlayer = (MediaPlayerControl) player;
        }
//        togglePausePlay();
    }

    @Override
    public void show() {
        show(sDefaultTimeout);
    }

    /**
     * Show the controller on screen. It will go away automatically after
     * 'timeout' milliseconds of inactivity.
     *
     * @param timeout
     * The timeout in milliseconds. Use 0 to show the controller until hide() is called.
     */
    @Override
    public void show(int timeout) {
        /*
        if (!mShowing) {
            if (mAnchor != null && mAnchor.getWindowToken() != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    mAnchor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                }
            }
            if (mPauseButton != null)
                mPauseButton.requestFocus();
            disableUnsupportedButtons();

            if (mFromXml) {
                setVisibility(View.VISIBLE);
            } else {
                int[] location = new int[2];

                if (mAnchor != null) {
                    mAnchor.getLocationOnScreen(location);
                    Rect anchorRect = new Rect(location[0], location[1],
                            location[0] + mAnchor.getWidth(), location[1]
                            + mAnchor.getHeight());

                    mWindow.setAnimationStyle(mAnimStyle);
                    mWindow.showAtLocation(mAnchor, Gravity.BOTTOM,
                            anchorRect.left, 0);
                } else {
                    Rect anchorRect = new Rect(location[0], location[1],
                            location[0] + mRoot.getWidth(), location[1]
                            + mRoot.getHeight());

                    mWindow.setAnimationStyle(mAnimStyle);
                    mWindow.showAtLocation(mRoot, Gravity.BOTTOM,
                            anchorRect.left, 0);
                }
            }
            mShowing = true;
            if (mShownListener != null)
                mShownListener.onShown();
        }
        updatePausePlay();
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        if (timeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT),
                    timeout);
        }
        */
    }

    /**
     * show controller view
     */
    public void showController() {
        if (!mShowing && mAnchorView != null) {
            mPlayer.shwoNextVideoTitle(false);

            //animate anchorview when layout changes
            //equals android:animateLayoutChanges="true"
            mAnchorView.setLayoutTransition(new LayoutTransition());
            setSeekProgress();
            // by zj
//            setSeekbarVolume();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
                if (!mPlayer.canPause()) {
                    mPauseButton.setEnabled(false);
                }
            }

            //add controller view to bottom of the AnchorView
            LayoutParams tlp = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
//            (int) (mContext.getResources().getDisplayMetrics().density * 45)
            mAnchorView.addView(this, tlp);
            mShowing = true;//set view state
            if (mShownListener != null)
                mShownListener.onShown();
            mHandler.removeMessages(SHOW_PROGRESS);
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
            togglePausePlay();
        }
        //update progress

        if (sDefaultTimeout != 0) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.sendEmptyMessageDelayed(FADE_OUT, sDefaultTimeout);
        }

    }


    @Override
    public boolean isShowing() {
        return mShowing;
    }

    @Override
    public void hide() {
        /*
        if (mShowing) {
            if (mAnchor != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    //mAnchor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                }
            }
            try {
                mHandler.removeMessages(SHOW_PROGRESS);
                if (mFromXml)
                    setVisibility(View.GONE);
                else
                    mWindow.dismiss();
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "MediaController already removed");
            }
            mShowing = false;
            if (mHiddenListener != null)
                mHiddenListener.onHidden();
        }
        */
    }

    public void hideController(){
        if (mShowing) {
            mNextTitle.setVisibility(View.GONE);
            mHandler.removeMessages(FADE_OUT);
            mHandler.removeMessages(SHOW_PROGRESS);
            if (mAnchorView == null) {
                return;
            }

            try {
                mAnchorView.removeView(this);
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }

            mShowing = false;
            mPlayer.shwoNextVideoTitle(true);
            if (mHiddenListener != null)
                mHiddenListener.onHidden();
        }
    }


    @Override
    public void setEnabled(boolean enabled) {
//        if (mPauseButton != null) {
//            mPauseButton.setEnabled(enabled);
//        }
//        if (mFfwdButton != null) {
//            mFfwdButton.setEnabled(enabled);
//        }
//        if (mRewButton != null) {
//            mRewButton.setEnabled(enabled);
//        }
//        if (mProgress != null && !mDisableProgress)
//            mProgress.setEnabled(enabled);
//        disableUnsupportedButtons();
//        super.setEnabled(enabled);
    }

    public View getBtnResol0() {
        return btn_resol0;
    }

    public View getBtnResol1() {
        return btn_resol1;
    }

    @Override
    public void onSingleTap() {
        if (mShowing) {
                hideController();
            } else {
                showController();
            }
    }

    @Override
    public void onHorizontalScroll(MotionEvent event, float delta, boolean firstScroll) {
        if (mPlayer == null) {
            return;
        }
        if (event.getPointerCount() == 1) {
            isScrollProgress = true;
            if (firstScroll) {
                mDragging = true;
                mHandler.removeMessages(SHOW_PROGRESS);
                long sec = mPlayer.getDuration() / 1000;
                if (sec <= 20) {
                    PROGRESS_SEEK = 1000;
                } else if (sec <= 120) {
                    PROGRESS_SEEK = 2000;
                } else {
                    PROGRESS_SEEK = 5000;
                }
            }
            mSeekBar.setProgress((int) (mSeekBar.getProgress() + delta));
        }

    }

    @Override
    public void onVerticalScroll(MotionEvent motionEvent, float delta, int direction) {
        if (motionEvent.getPointerCount() == 1) {

            if (direction == ViewGestureListener.SWIPE_LEFT) {
                updateBrightness(delta);
            } else {
                updateVolume(delta);
            }


        }
    }

    /**
     * update brightness
     *
     * @param delta
     */
    private void updateBrightness(float delta) {
        if (mActivity == null) {
            return;
        }
        mCurBrightness = mActivity.getWindow().getAttributes().screenBrightness;
        if (mCurBrightness <= 0.01f) {
            mCurBrightness = 0.01f;
        }

        WindowManager.LayoutParams attributes = mActivity.getWindow().getAttributes();
        attributes.screenBrightness = mCurBrightness + delta / (ViewGestureListener.getDeviceHeight(mContext) * 0.8f);
        if (attributes.screenBrightness >= 1.0f) {
            attributes.screenBrightness = 1.0f;
        } else if (attributes.screenBrightness <= 0.01f) {
            attributes.screenBrightness = 0.01f;
        }
        mActivity.getWindow().setAttributes(attributes);

//        float percent = attributes.screenBrightness * 100;

    }

    private void updateVolume(float delta) {

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
        int volume = (int) (mCurVolume + change);
        if (volume > mMaxVolume) {
            volume = mMaxVolume;
        }

        if (volume < 0) {
            volume = 0;
        }
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        right_seekbar.setProgress(volume);
    }


    public interface MediaPlayerControl extends IMediaController.MediaPlayerControl{

        /**
         * exit media player
         */
        void exit();

        /**
         * get top title name
         */
        String getTopTitle();


        String getNextVideoTitle();


        void shwoNextVideoTitle(boolean show);

    }
    public static boolean isEmpty(String input) {
        if (input == null || "".equals(input))
            return true;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c != ' ' && c != '\t' && c != '\r' && c != '\n') {
                return false;
            }
        }
        return true;
    }
}
