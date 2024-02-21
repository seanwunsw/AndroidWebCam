package com.android.example.secam;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class test extends Thread {
    private byte[] latestJpegFrameData = null;
    String boundary = "frame";
    String mimeType = "multipart/x-mixed-replace; boundary=" + boundary;
    public test(){
        try {
            ServerSocket server = new ServerSocket(8080);
            Socket socket = server.accept();
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
        }catch (Exception e){

        }
    }

    public void setLatestJpegFrameData(byte[] latestJpegFrameData){
        this.latestJpegFrameData = latestJpegFrameData;
    }

    public void run(){
        OutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write("HTTP/1.0 200 OK\r\n".getBytes());
            stream.write(("--" + boundary + "\r\n").getBytes());
            stream.write(("Content-Type: image/jpeg\r\n").getBytes());
            stream.write("Cache-Control no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n".getBytes());
            stream.write("Expires:-1\r\n".getBytes());
            stream.write("Pragma: no-cache\r\n".getBytes());
            //stream.write();

            while(true){
                stream.write(("Content-Length: " +  latestJpegFrameData.length + "\r\n\r\n").getBytes());
                stream.write(latestJpegFrameData);
                stream.flush();
            }

        } catch (Exception e ) {

        }
    }

}