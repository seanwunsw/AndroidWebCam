package com.android.example.secam;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class MjpegServer extends Thread {
    private byte[] latestJpegFrameData = null;
    String boundary = "frame";
    String mimeType = "multipart/x-mixed-replace; boundary=" + boundary;
    public MjpegServer(){
        try {
            ServerSocket server = new ServerSocket(8080);
             Socket clientSocket = server.accept();
        }catch (Exception e){

        }
    }

    public void run(){
        OutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(("--" + boundary + "\r\n").getBytes());
            stream.write(("Content-Type: image/jpeg\r\n").getBytes());
            stream.write("Cache-Control no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n".getBytes());
            stream.write("Expires:-1\r\n".getBytes());
            stream.write("Pragma: no-cache\r\n".getBytes());
            //stream.write();

            while(true){
                stream.write(("Content-Length: " +  latestJpegFrameData.length + "\r\n\r\n").getBytes());
                stream.write(latestJpegFrameData);
            }

        } catch (Exception e ) {
        throw new RuntimeException(e);
    }
    }

}