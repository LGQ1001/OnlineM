package com.zhangshirong.onlinem;

import android.media.MediaPlayer;

import java.io.File;

/**
 * Created by jarvis on 17-8-19.
 */

public interface OnlineMInterface {
    public String cacheFileName(String i);
    public String downloadFileName(String i);
    public boolean isSameCache(File file,long newSize);
    public void onCacheProgress(String url,String cacheUri,float progress);
    public void onMediaPlayProgress(float progress,long cur,long duration);
    public void onDonwloadProgress(float progress,long downloaded,long filesize);
    public void onMediaPrepared();
    public void onMediaBufferingUpdate(float progress);
    public void onMediaCompleted();
}
