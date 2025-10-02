package com.seanw.webcam

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class MjpegServer {
    private val TAG = "MjpegServer"

    @Volatile
    private var latestJpegFrameData: ByteArray? = null
    private var serverJob: Job? = null
    private var portNumber: Int
    private var framePerSecond: Int

    constructor() {
        portNumber = 8080
        framePerSecond = 24
    }

    constructor(portNumber: Int) {
        this.portNumber = portNumber
        this.framePerSecond = 24
    }

    constructor(portNumber: Int, framePerSecond: Int) {
        this.portNumber = portNumber
        this.framePerSecond = framePerSecond
    }

    fun start() {
        serverJob =
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val server = ServerSocket(portNumber)
                    Log.d(TAG, "MJPEG server started on port $portNumber")

                    while (isActive) {
                        try {
                            val socket = server.accept()
                            // Handle each client in a separate coroutine
                            launch {
                                handleClient(socket)
                            }
                        } catch (e: Exception) {
                            if (isActive) {
                                Log.e(TAG, "Error accepting connection", e)
                            }
                        }
                    }
                    server.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Server error", e)
                }
            }
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        Log.d(TAG, "MJPEG server stopped")
    }

    fun setLatestJpegFrameData(latestJpegFrameData: ByteArray?) {
        this.latestJpegFrameData = latestJpegFrameData
    }

    private suspend fun handleClient(socket: Socket) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null

        try {
            inputStream = socket.getInputStream()
            outputStream = socket.getOutputStream()

            // Send HTTP headers
            outputStream.write(
                (
                    "HTTP/1.0 200 OK\r\n" +
                        "Connection: close\r\n" +
                        "Content-Type: multipart/x-mixed-replace; boundary=--frame\r\n" +
                        "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                        "Expires: -1\r\n\r\n"
                ).toByteArray(),
            )
            outputStream.flush()

            val frameInterval = if (framePerSecond > 0) (1000 / framePerSecond) else 42
            var lastFrameTime = System.currentTimeMillis()

            while (serverJob?.isActive == true) {
                val currentFrameData = latestJpegFrameData
                if (currentFrameData != null) {
                    val currentTime = System.currentTimeMillis()
                    val elapsedTime = currentTime - lastFrameTime

                    // Skip frame if we're running behind
                    if (elapsedTime < frameInterval) {
                        delay(maxOf(1, frameInterval - elapsedTime))
                    }

                    outputStream.write(
                        (
                            "--frame\r\n" +
                                "Content-Type: image/jpeg\r\n"
                        ).toByteArray(),
                    )
                    outputStream.write(("Content-Length:${currentFrameData.size}\r\n\r\n").toByteArray())
                    outputStream.write(currentFrameData)
                    outputStream.write("\r\n".toByteArray())
                    outputStream.flush()

                    lastFrameTime = System.currentTimeMillis()
                } else {
                    delay(10) // Small delay when no frame data available
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
        } finally {
            try {
                outputStream?.close()
                inputStream?.close()
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing socket resources", e)
            }
        }
    }
}
