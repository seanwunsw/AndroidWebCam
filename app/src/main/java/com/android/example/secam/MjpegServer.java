package com.android.example.secam;
import android.util.Log;
import static android.content.ContentValues.TAG;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class MjpegServer extends Thread {
    private byte[] latestJpegFrameData = null;
    String boundary = "frame";
    String mimeType = "multipart/x-mixed-replace; boundary=" + boundary;
    InputStream inputStream = null;
    OutputStream outputStream = null;
    public MjpegServer(){

        try {
            ServerSocket server = new ServerSocket(8080);
            Socket socket = server.accept();
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        }catch (Exception e){
            Log.e(TAG, "constructor: ",e );
        }
    }

    public void setLatestJpegFrameData(byte[] latestJpegFrameData){
        this.latestJpegFrameData = latestJpegFrameData;
    }

    public void run(){
        try {
            outputStream.write("HTTP/1.0 200 OK\r\n".getBytes());
            outputStream.write("Connection: close\\r\\n".getBytes());
            outputStream.write(("--" + boundary + "\r\n").getBytes());
            outputStream.write(("Content-Type: image/jpeg\r\n").getBytes());
            outputStream.write("Cache-Control no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n".getBytes());
            outputStream.write("Expires:-1\r\n".getBytes());
            outputStream.write("Pragma: no-cache\r\n".getBytes());
            //stream.write();

            while(true){
                outputStream.write(("Content-Length: " +  latestJpegFrameData.length + "\r\n\r\n").getBytes());
                outputStream.write(latestJpegFrameData);
                outputStream.flush();
            }

        } catch (Exception e ) {
            Log.e(TAG, "run: ",e );
        }
    }

}