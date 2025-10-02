package com.seanw.webcam

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutionException

data class CameraUiState(
    val ipAddress: String = "",
    val isFabExpanded: Boolean = false,
    val isServerRunning: Boolean = false,
    val isCameraPermissionGranted: Boolean = false,
)

class CameraViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var mjpegServer: MjpegServer? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var previewView: PreviewView? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    init {
        viewModelScope.launch {
            updateIpAddress()
        }
    }

    private suspend fun updateIpAddress() {
        val ip = NetworkHelper.getIPAddress(true)
        val ipText = "IP address: $ip\nPort: 8080 (Not finish yet~)"
        _uiState.value = _uiState.value.copy(ipAddress = ipText)
    }

    fun setPreviewView(previewView: PreviewView) {
        this.previewView = previewView
    }

    fun toggleFabExpansion() {
        _uiState.value =
            _uiState.value.copy(
                isFabExpanded = !_uiState.value.isFabExpanded,
            )
    }

    fun setCameraPermissionGranted(granted: Boolean) {
        _uiState.value = _uiState.value.copy(isCameraPermissionGranted = granted)
    }

    fun startCamera(context: Context) {
        if (!_uiState.value.isCameraPermissionGranted) return

        viewModelScope.launch {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView?.surfaceProvider)

                val imageAnalysis =
                    ImageAnalysis
                        .Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(android.util.Size(720, 1280))
                        .build()

                imageAnalysis.setAnalyzer(
                    ContextCompat.getMainExecutor(context),
                ) { imageProxy ->
                    processImage(imageProxy)
                }

                cameraProvider?.unbindAll()
                camera =
                    cameraProvider?.bindToLifecycle(
                        context as androidx.lifecycle.LifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis,
                    )
            } catch (e: ExecutionException) {
                Log.e("CameraViewModel", "Camera initialization failed", e)
            } catch (e: InterruptedException) {
                Log.e("CameraViewModel", "Camera initialization interrupted", e)
            }
        }
    }

    private fun processImage(image: ImageProxy) {
        if (!_uiState.value.isServerRunning) {
            image.close()
            return
        }

        try {
            val rgbaBuffer = image.planes[0].buffer
            val rgbaSize = rgbaBuffer.remaining()
            val rgbaData = ByteArray(rgbaSize)

            rgbaBuffer.position(0)
            rgbaBuffer.get(rgbaData, 0, rgbaSize)

            val bitmap =
                Bitmap.createBitmap(
                    image.width,
                    image.height,
                    Bitmap.Config.ARGB_8888,
                )
            bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(rgbaData))

            val jpegStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, jpegStream)
            val jpegData = jpegStream.toByteArray()

            mjpegServer?.setLatestJpegFrameData(jpegData)

            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e("CameraViewModel", "Error processing image", e)
        } finally {
            image.close()
        }
    }

    fun switchCamera(context: Context) {
        cameraSelector =
            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

        startCamera(context)
    }

    fun startServer() {
        if (_uiState.value.isServerRunning) return

        mjpegServer = MjpegServer()
        mjpegServer?.start()
        _uiState.value = _uiState.value.copy(isServerRunning = true)
        Log.d("CameraViewModel", "MJPEG server started")
    }

    fun stopServer() {
        if (!_uiState.value.isServerRunning) return

        mjpegServer?.stop()
        mjpegServer = null
        _uiState.value = _uiState.value.copy(isServerRunning = false)
        Log.d("CameraViewModel", "MJPEG server stopped")
    }

    fun toggleServer() {
        if (_uiState.value.isServerRunning) {
            stopServer()
        } else {
            startServer()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopServer()
        cameraProvider?.unbindAll()
    }
}
