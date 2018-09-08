package com.kas4.lightmediacontroller.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;

/**
 * Created by wesafari on 2017/5/2.
 */

public class VideoPlayView extends TextureView implements TextureView.SurfaceTextureListener{
    private MediaPlayer mMediaPlayer;
    private Uri mUri;
    private Context mContext;

    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnPreparedListener mOnPreparedListener;

    private MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {

            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
        }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener =
            new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    }
                }
     };

    public void setOnPreparedListener(MediaPlayer.OnPreparedListener l){
        mOnPreparedListener = l;
    }


    public void setOnCompletionListener(MediaPlayer.OnCompletionListener l){
        mOnCompletionListener = l;
    }

    public VideoPlayView(Context context) {
        super(context);
        initVideoPlayView(context);
    }

    public VideoPlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoPlayView(context);
    }

    public VideoPlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoPlayView(context);
    }

    private void initVideoPlayView(Context context) {
        mContext = context;
        setSurfaceTextureListener(this);
    }

    public void setVideoPath(String path) {
        if (!TextUtils.isEmpty(path)) {
            setVideoURI(Uri.parse(path));
        }
    }

    public void setVideoURI(Uri uri) {
        mUri = uri;
        openVideo();
        requestLayout();
        invalidate();
    }

    public boolean isPlaying(){
        if (mMediaPlayer !=null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    public void stopPlayback() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
//            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//            am.abandonAudioFocus(null);
        }
    }

    public void closeVolume(){
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(0, 0);
        }

    }

    public void start() {
        if (!mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
        }
    }

    public void resume() {
        openVideo();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setOutline() {
        setOutlineProvider(new TextureVideoViewOutlineProvider());
        setClipToOutline(true);
    }

    private void openVideo() {
        if (mUri == null || getSurfaceTexture() == null) {
            return;
        }
        release();

//        AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);


        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setSurface(new Surface(getSurfaceTexture())); // 添加到容器中
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnPreparedListener(mPreparedListener);
        mMediaPlayer.setOnCompletionListener(mCompletionListener);
        try {
            mMediaPlayer.setDataSource(mContext, mUri);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;

//            AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//            am.abandonAudioFocus(null);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        openVideo();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        release();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
