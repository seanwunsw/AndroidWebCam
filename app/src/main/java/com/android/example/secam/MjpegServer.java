package com.android.example.secam;

import android.os.AsyncTask;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MjpegServer extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "MjpegServerTask";
    private byte[] latestJpegFrameData = null;
    private InputStream inputStream = null;
    private OutputStream outputStream = null;

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            ServerSocket server = new ServerSocket(8080);
            while (!isCancelled()) {
                Socket socket = server.accept();
                new VideoFeedHandler(socket).start();
            }
            server.close();
        } catch (Exception e) {
            Log.e(TAG, "doInBackground: ", e);
        }
        return null;
    }

    public void setLatestJpegFrameData(byte[] latestJpegFrameData) {
        this.latestJpegFrameData = latestJpegFrameData;
    }

    private class VideoFeedHandler extends Thread {
        private Socket socket;

        public VideoFeedHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                outputStream.write(
                        ("HTTP/1.0 200 OK\r\n"+
                                "Connection: close\r\n"+
                                "Content-Type: multipart/x-mixed-replace; boundary=--frame\r\n" +
                                "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n"+
                                "Expires: -1\r\n"+
                                "Pragma: no-cache\r\n\r\n").getBytes());
                outputStream.flush();
                while (!isCancelled()) {
                    if (latestJpegFrameData != null) {
                        outputStream.write(("--frame\r\n"+
                                "Content-Type: image/jpeg\r\n").getBytes());
                        outputStream.write(("Content-Length:"+latestJpegFrameData.length+"\r\n\r\n").getBytes());
                        outputStream.write(latestJpegFrameData);
                        outputStream.write("\r\n".getBytes());
                        outputStream.flush();
                        Thread.sleep(34);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "VideoFeedHandler.run: ", e);
            }finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    socket.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing socket resources: ", e);
                }
            }
        }
    }
}
