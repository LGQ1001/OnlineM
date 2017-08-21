package com.zhangshirong.onlinem;

import android.os.Looper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by jarvis on 17-8-19.
 */

public class OnlineMCacheManager {
    private HashMap<String,Object> map = new HashMap<>();
    private OnlineM onlineM;
    public OnlineMCacheManager(OnlineM onlineM){
        this.onlineM = onlineM;
    }
    public CacheData get(String key){
        return (CacheData) map.get(key);
    }
    public void put(String key,Object obj){
        map.put(key,obj);
    }
    public class CacheData{
        public long size;
        public long loaded;
        public File file;
        public String uri;
        public String url;
    }
    public void startCache(String origUrl){
        if(onlineM.listener == null)return;
        CacheData cacheData = new CacheData();
        String dir = onlineM.cacheDir;
        String uri = onlineM.listener.cacheFileName(origUrl);
        File file = new File(dir);
        //这里要考虑权限问题
        if(!file.exists()){
            file.mkdirs();
        }
        file = new File(file,uri);
        System.out.println(file.getAbsolutePath());
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        long fullSize = 0;
        long loadSize = 0;
        if(file.canWrite() && file.canRead()){
            URL url = null;
            InputStream inputStream = null;

            try {
                url = new URL(origUrl);
                HttpURLConnection urlCon = (HttpURLConnection) url.openConnection();
                urlCon.setReadTimeout(5000);
                fullSize = urlCon.getContentLength();
                if(fullSize == 0 )return;
                loadSize = file.length();
                if(!onlineM.listener.isSameCache(file,fullSize)){//这里验证缓存，按需修改
                    file.createNewFile();
                    loadSize = 0;
                    inputStream = urlCon.getInputStream();
                }

                cacheData.loaded = loadSize;
                cacheData.size = fullSize;
                cacheData.file = file;
                cacheData.url = origUrl;
                cacheData.uri = uri;
                put(cacheData.uri,cacheData);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(inputStream != null){
                int block = -1;
                int blockSize = 10240;
                byte[] buf = new byte[blockSize];
                try {
                    boolean isReady = false;
                    FileOutputStream fos = new FileOutputStream(file);
                    //线程安全 for unexpected end of stream
                    synchronized (inputStream){
                        block = inputStream.read(buf);
                        while (block != -1) {
                            fos.write(buf,0,block);
                            fos.flush();
                            loadSize += block;
                            cacheData.loaded = loadSize;
                            //通知事件
                            if(!isReady && cacheData.loaded > 102400){
                                onlineM.mediaPrepare(uri);
                                isReady = true;
                            }
                            onlineM.listener.onCacheProgress(origUrl,uri,(float)loadSize/fullSize);
                            block = inputStream.read(buf);
                        }

                    }
                    inputStream.close();
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
            else{
                System.out.println("cache is exist.");
                onlineM.mediaPrepare(uri);
                onlineM.listener.onCacheProgress(origUrl,uri,1f);
            }
        }
    }
}
