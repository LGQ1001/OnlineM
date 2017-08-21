package com.zhangshirong.onlinem;

import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {
    private OnlineM onlineM;
    private EditText editText;
    private TextView cacheInfo;
    private TextView bufferingInfo;
    private TextView playInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onlineM = new OnlineM(this, 6781, new OnlineMInterface() {
            @Override
            public String cacheFileName(String i) {
                return md5(i);
            }

            @Override
            public String downloadFileName(String i) {
                return null;
            }

            @Override
            public boolean isSameCache(File file, long newSize) {
                return (file.length() == newSize);
            }


            @Override
            public void onCacheProgress(String url, final String cacheUri, final float progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cacheInfo.setText("CacheURI: " + cacheUri + "\nLoaded:" + (progress * 100) + "%");
                    }
                });

            }


            @Override
            public void onMediaPlayProgress(final float progress, final long cur, final long duration) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playInfo.setText("MediaPlaying: " + cur + "ms | " + duration + "ms");
                    }
                });

            }


            @Override
            public void onDonwloadProgress(float progress, long downloaded, long filesize) {

            }

            @Override
            public void onMediaPrepared() {
                onlineM.mediaStart();
            }

            @Override
            public void onMediaBufferingUpdate(final float progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bufferingInfo.setText("MediaBuffering: " + (progress * 100) + "%");
                    }
                });
            }

            @Override
            public void onMediaCompleted() {

            }
        });
        String dirBase = Environment.getExternalStorageDirectory().getAbsolutePath();
        onlineM.setConfig(dirBase+"/OnlineM/Cache",dirBase+"/OnlineM/Download");
        onlineM.startServer();
        ((Button)findViewById(R.id.loadStart)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cacheInfo.setText("");
                playInfo.setText("");
                bufferingInfo.setText("");
                onlineM.load(editText.getText().toString());
            }
        });
        onlineM.setConfig(dirBase+"/OnlineM/Cache",dirBase+"/OnlineM/Download");
        ((Button)findViewById(R.id.playGo)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onlineM.mediaStart();
            }
        });
        ((Button)findViewById(R.id.playPause)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onlineM.mediaPause();
            }
        });
        ((Button)findViewById(R.id.playFoward)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onlineM.mediaSeekTo(onlineM.mediaCurrentPosition()+10000);
            }
        });
        editText = (EditText) findViewById(R.id.musicUrl);
        cacheInfo = (TextView) findViewById(R.id.cacheInfo);
        bufferingInfo = (TextView) findViewById(R.id.bufferingInfo);
        playInfo = (TextView) findViewById(R.id.playInfo);

    }
    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            String result = "";
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result += temp;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
