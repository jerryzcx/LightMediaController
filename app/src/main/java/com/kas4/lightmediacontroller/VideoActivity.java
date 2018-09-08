package com.kas4.lightmediacontroller;


import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;

import com.kas4.lightmediacontroller.video.MediaController;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;
import com.pili.pldroid.player.widget.PLVideoTextureView;


public class VideoActivity extends AppCompatActivity {
    String mVideoPath="http://video.wesafari.cn/d88b124c3c/574a1f02e7a0b_bf58e56ca5e42bd1f56574a1f02e79ba6.62401308";

    private PLVideoTextureView mVideoView;
    private MediaController mMediaController;
    private boolean isPause;
    private View mLoadingView;
    //    VideoControllerView controller;

    protected <VT extends View> VT getViewById(@IdRes int id) {
        return (VT) findViewById(id);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        initVideo();

    }

    private void initVideo() {

        mMediaController = new MediaController(this, false, false);
        mMediaController.setAnchorView((ViewGroup) findViewById(R.id.videoSurfaceContainer));
        mMediaController.setMediaPlayer(mMediaPlayerControl);
        mMediaController.setOnShownListener(new MediaController.OnShownListener() {
            @Override
            public void onShown() {
            }
        });

        mLoadingView = getViewById(R.id.loading);
        mVideoView = (PLVideoTextureView) findViewById(R.id.video_view);
        mVideoView.setBufferingIndicator(mLoadingView);
        mVideoView.setMediaController(mMediaController);
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mMediaController.showController();
                return true;
            }
        });

        mVideoView.setOnPreparedListener(mPreparedListener);
        mVideoView.setOnErrorListener(mErrorListener);
        mVideoView.setOnCompletionListener(mCompletionListener);

        AVOptions options = new AVOptions();
        // the unit of timeout is msi
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000);
        options.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, 10 * 1000);
        // Some optimization with buffering mechanism when be set to 1
        options.setInteger(AVOptions.KEY_LIVE_STREAMING, 0);
//            if (isLiveStreaming == 1) {
//                options.setInteger(AVOptions.KEY_DELAY_OPTIMIZATION, 1);
//            }

        // 1 -> hw codec enable, 0 -> disable [recommended]
//            int codec = getIntent().getIntExtra("mediaCodec", 0);
        options.setInteger(AVOptions.KEY_MEDIACODEC, 0);

        // whether start play automatically after prepared, default value is 1
        options.setInteger(AVOptions.KEY_START_ON_PREPARED, 0);
        mVideoView.setAVOptions(options);

        mVideoView.setVolume(1.0f, 1.0f);

        mVideoView.setVideoPath(mVideoPath);

    }

    long mStartTime = 0;

    private MediaController.MediaPlayerControl mMediaPlayerControl = new MediaController.MediaPlayerControl() {

        @Override
        public void start() {
            if (mVideoView != null) {
                mVideoView.start();
            }

        }

        @Override
        public void pause() {
            if (mVideoView != null) {
                mVideoView.pause();
            }
        }

        @Override
        public long getDuration() {
            if (mVideoView != null) {
                return mVideoView.getDuration();
            }
            return 0;
        }

        @Override
        public long getCurrentPosition() {
            if (mVideoView != null) {
                return mVideoView.getCurrentPosition();
            }
            return 0;
        }

        @Override
        public void seekTo(long l) {
            if (mVideoView != null) {
                mVideoView.seekTo(l);
            }

        }

        @Override
        public boolean isPlaying() {
            if (mVideoView != null) {
                return mVideoView.isPlaying();
            }
            return false;
        }

        @Override
        public int getBufferPercentage() {
            if (mVideoView != null) {
                return mVideoView.getBufferPercentage();
            }
            return 0;
        }

        @Override
        public boolean canPause() {
            return true;
        }

        @Override
        public boolean canSeekBackward() {
            return false;
        }

        @Override
        public boolean canSeekForward() {
            return false;
        }

        @Override
        public void exit() {
            onBackPressed();
        }

        @Override
        public String getTopTitle() {
            return "TEST";
        }

        @Override
        public String getNextVideoTitle() {
            if (mVideoView != null) {
                long position = mVideoView.getCurrentPosition();
                long duration = mVideoView.getDuration();
                if (duration - position < 5000) {
                }
            }
            return null;
        }

        @Override
        public void shwoNextVideoTitle(boolean show) {
        }
    };

    private PLMediaPlayer.OnPreparedListener mPreparedListener = new PLMediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(PLMediaPlayer mp) {
            // play
            mVideoView.start();
            mVideoView.seekTo(mStartTime);

            mMediaController.setGestureListener(VideoActivity.this);

        }
    };

    private PLMediaPlayer.OnErrorListener mErrorListener = new PLMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(PLMediaPlayer plMediaPlayer, int i) {
            mVideoView.stopPlayback();
            finish();
            return true;
        }
    };

    private PLMediaPlayer.OnCompletionListener mCompletionListener = new PLMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(PLMediaPlayer mp) {
            mStartTime = 0;
            mMediaController.hide();
            initPlayEndView();

        }
    };

    private void initPlayEndView() {
        if (vs_play_end == null) {
            vs_play_end = (ViewStub) findViewById(R.id.vs_play_end);
            vs_play_end.inflate();

            btn_back = getViewById(R.id.btn_back);

            btn_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            btn_replay = getViewById(R.id.btn_replay);
            btn_replay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    vs_play_end.setVisibility(View.GONE);
                    mVideoView.seekTo(mStartTime);
                    mVideoView.start();

                }
            });

        }
        vs_play_end.setVisibility(View.VISIBLE);

    }

    ViewStub vs_play_end;
    View btn_back;
    View btn_replay;


    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView != null && isPause) {
            mVideoView.start();
//            mHandler.removeMessages(ShowTitleHandler.SHOW_NEXT_TITLE);
//            mHandler.sendEmptyMessage(ShowTitleHandler.SHOW_NEXT_TITLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mHandler.removeMessages(ShowTitleHandler.SHOW_NEXT_TITLE);
        if (mVideoView != null && mVideoView.isPlaying()) {
            isPause = true;
            mVideoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        mVideoView.stopPlayback();
        mVideoView = null;
        super.onDestroy();
    }

}
