package com.zhangshirong.onlinem;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;


/**
 * Created by jarvis on 17-8-19.
 */

public class OnlineM {
    protected MediaPlayer media;
    protected boolean mediaIsReady;
    protected OnlineMInterface listener;
    protected String cacheDir;
    protected String downloadDir;
    protected OnlineMCacheManager cacheManager;
    protected OnlineMServer server;
    private Context context;
    public OnlineM(Context context,int port,OnlineMInterface listener){
        this.listener = listener;
        this.context = context;
        cacheManager = new OnlineMCacheManager(this);
        server = new OnlineMServer(this,port);
        media = new MediaPlayer();
    }
    public void serverStart(){
        server.start();
    }
    public void serverStop(){
        server.stop();
    }
    public void setConfig(String cacheDir,String downloadDir){
        this.cacheDir = cacheDir;
        this.downloadDir = downloadDir;
    }
    public void load(final String url){
        new Thread(new Runnable() {
            @Override
            public void run() {
                cacheManager.startCache(url);
            }
        }).start();
    }

    public void mediaPause(){
        if(media != null && mediaIsReady && media.isPlaying()){
            media.pause();
        }
    }
    public void mediaSeekTo(int msec){
        if(media != null && mediaIsReady){
            media.seekTo(msec);
        }
    }
    public void mediaStart(){
        if(media != null && mediaIsReady && !media.isPlaying()){
            media.start();
        }
    }
    public int mediaCurrentPosition(){
        if(media!=null && mediaIsReady){
            return media.getCurrentPosition();
        }
        return 0;
    }
    public int mediaDuration(){
        if(media!=null && mediaIsReady){
            return media.getDuration();
        }
        return 0;
    }
    public void release(){
        if(media != null){
            media.reset();
            media.release();
            media = null;
        }
        mediaIsReady = false;
        listener = null;
        server.release();
    }
    public void mediaStop(){
        if(media != null){
            media.stop();
            mediaIsReady = false;
        }
    }
    protected void mediaPrepare(String uri){
        if(media == null || listener == null)return;
        media.reset();
        System.out.println("preparing "+uri);
        try {
            System.out.println(server.getLocalAddress() + uri);
            media.setDataSource(this.context.getApplicationContext(),Uri.parse(server.getLocalAddress() + uri));
            media.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(final MediaPlayer media) {
                    mediaIsReady = true;
                    OnlineM.this.listener.onMediaPrepared();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            int cur = media.getCurrentPosition();
                            int duration = media.getDuration();
                            while (cur < duration && mediaIsReady){
                                if(mediaIsReady)listener.onMediaPlayProgress((float)cur/duration,cur,duration);
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(mediaIsReady)cur = media.getCurrentPosition();
                            }
                            listener.onMediaPlayProgress(1f,duration,duration);
                        }
                    }).start();
                }
            });
            media.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    OnlineM.this.listener.onMediaBufferingUpdate((float)percent/100);
                }
            });
            media.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    int duration = OnlineM.this.media.getDuration();
                    listener.onMediaPlayProgress(1f,duration,duration);
                }
            });
            media.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
