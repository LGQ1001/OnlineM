package com.zhangshirong.onlinem;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.zip.GZIPOutputStream;

/**
 * Created by jarvis on 17-8-19.
 */

public class OnlineMServer{
    private OnlineM onlineM;
    private Boolean state = false;
    private ServerSocket server;
    private String localAddress;
    private int port;
    public OnlineMServer(OnlineM onlineM,int port){
        this.onlineM = onlineM;
        try {
            server = new ServerSocket(port);
            this.port = port;
            this.localAddress = "http://127.0.0.1:" + port + "/";
            new Thread(new Runnable() {
                @Override
                public void run() {
                    start();
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String getLocalAddress(){
        return localAddress;
    }
    public void start(){
        state = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(state){
                    try {
                        process(server.accept());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }
    public void stop(){
        this.state = false;
    }
    public void release(){
        stop();
        try {
            server.close();
            server = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        localAddress = null;
    }
    private void process(Socket client){
        try {
            InputStream inputStream = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            char[] requestBuffer = new char[32];
            String requestHeader = "";
            while (requestHeader.lastIndexOf("\r\n\r\n")==-1){
                br.read(requestBuffer);
                requestHeader += String.valueOf(requestBuffer);
            }
            requestHeader = requestHeader.substring(0,requestHeader.lastIndexOf("\r\n\r\n"));

            int skipSize = 0;
            int contentLength = 0;
            String uri = "";
            int p;
            try {
                //获取数据范围
                p = requestHeader.indexOf("Range: bytes=");
                if(p != -1){
                    int m = requestHeader.indexOf("-",p);
                    skipSize = Integer.parseInt(requestHeader.substring(p + 13,m));
                    p = requestHeader.indexOf("\r\n",m);
                    contentLength = Integer.parseInt(requestHeader.substring(m + 1,p)) - skipSize;
                }
                //获取请求的uri，用于查找文件
                p = requestHeader.indexOf("GET /");
                if(p != -1){
                    int m = requestHeader.indexOf("HTTP/",p);
                    if(m!=-1)uri = requestHeader.substring(p + 5,m).trim();
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
            if(uri.isEmpty())return;
            String headers= "";
            OnlineMCacheManager.CacheData cacheInfo = onlineM.cacheManager.get(uri);
            DataOutputStream out = new DataOutputStream(client.getOutputStream());
            //int maxLength = client.getSendBufferSize();
            if(cacheInfo!=null){
                if(contentLength == 0)contentLength = (int) cacheInfo.size;
                //构建协议头
                headers += "HTTP/1.1 200 OK\r\n";
                headers += "Content-Type: audio/mpeg\r\n";
                headers += "Accept-Ranges: bytes " + skipSize + "-" + (contentLength - 1) + "/" + cacheInfo.size + "\r\n";
                headers += "Content-Length: " + contentLength + "\r\n";
                headers += "Connection: Keep-Alive\r\n";
                headers += "\r\n";
                out.write(headers.getBytes());

                int block = 0;
                int blockSize = 52100;
                byte[] buffer = new byte[blockSize];
                int passingSize = 0;
                FileInputStream fis = new FileInputStream(cacheInfo.file);
                fis.skip(skipSize);
                block = fis.read(buffer);
                while (block!= -1 && !client.isClosed() && passingSize < contentLength) {
                    if(skipSize + passingSize < cacheInfo.loaded - blockSize || cacheInfo.loaded == cacheInfo.size){
                        if(!client.isConnected() || client.isClosed())break;
                        out.write(buffer,0,block);
                        passingSize += block;
                        block = fis.read(buffer);
                    }
                    else{
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                fis.close();
            }
            else {
                headers += "HTTP/1.1 404 Not Found\r\n\r\n";
                out.write(headers.getBytes());
            }
            out.close();
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
