package com.example.edgeviewer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: GLRenderer
    private lateinit var cameraManager: CameraManager

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader

    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null

    // Native function: input = grayscale bytes (Y plane); output = ARGB8888 bytes (w * h * 4)
    external fun processImageJNI(bytes: ByteArray, width: Int, height: Int): ByteArray

    companion object {
        init {
            System.loadLibrary("opencv_java4")
            System.loadLibrary("edgeviewer")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Views ---
        textureView = findViewById(R.id.cameraTexture)
        textureView.surfaceTextureListener = textureListener

        glView = findViewById(R.id.glView)
        renderer = GLRenderer()

        // OpenGL ES 2.0
        glView.setEGLContextClientVersion(2)
        glView.setRenderer(renderer)
        glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        // Camera manager
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        // Ask for camera permission if needed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            startCameraThread()
            // Do NOT open camera until TextureView is ready
            if (textureView.isAvailable) {
                openCamera()
            }
        }
    }

    // TextureView callback
    private val textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startCameraThread()
                openCamera()
            }
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            closeCamera()
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    // Permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (textureView.isAvailable) {
                startCameraThread()
                openCamera()
            } // else: openCamera will be called from textureListener when ready
        }
    }

    // ---------- Camera lifecycle ----------

    override fun onResume() {
        super.onResume()
        if (cameraThread == null) {
            startCameraThread()
        }
        if (cameraDevice == null &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED &&
            textureView.isAvailable
        ) {
            openCamera()
        }
    }

    override fun onPause() {
        closeCamera()
        stopCameraThread()
        super.onPause()
    }

    private fun startCameraThread() {
        if (cameraThread != null) return
        cameraThread = HandlerThread("CameraBackground").also {
            it.start()
            cameraHandler = Handler(it.looper)
        }
    }

    private fun stopCameraThread() {
        cameraThread?.quitSafely()
        cameraThread = null
        cameraHandler = null
    }

    private fun openCamera() {
        val cameraId = cameraManager.cameraIdList[0]

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) return

        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {

                override fun onOpened(cd: CameraDevice) {
                    cameraDevice = cd
                    startPreview()
                }

                override fun onDisconnected(cd: CameraDevice) {
                    cd.close()
                    cameraDevice = null
                }

                override fun onError(cd: CameraDevice, error: Int) {
                    cd.close()
                    cameraDevice = null
                }
            },
            cameraHandler
        )
    }

    private fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null
        } catch (_: Exception) {
        }

        try {
            cameraDevice?.close()
            cameraDevice = null
        } catch (_: Exception) {
        }

        try {
            if (this::imageReader.isInitialized) {
                imageReader.close()
            }
        } catch (_: Exception) {
        }
    }

    // ---------- Preview + OpenCV + OpenGL ----------

    private fun startPreview() {
        val device = cameraDevice ?: return

        val previewSize = Size(640, 480)

        // ImageReader for processing frames in native code
        imageReader = ImageReader.newInstance(
            previewSize.width,
            previewSize.height,
            ImageFormat.YUV_420_888,
            2
        )

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            try {
                // Copy everything we need BEFORE closing the image
                val w = image.width
                val h = image.height

                // Use just the Y (luma) plane as grayscale input
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                // We now have our own copy of the pixels, safe to close image
                image.close()

                // Native processing with OpenCV, returns ARGB8888 bytes
                val processed = processImageJNI(bytes, w, h)
                val frameCopy = processed.clone()

                runOnUiThread {
                    renderer.updateFrame(frameCopy, w, h)
                    glView.requestRender()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Make sure image is closed if something failed before we closed it
                try { image.close() } catch (_: Exception) {}
            }
        }, cameraHandler)

        // --- Surfaces for Camera2 ---
        val texture = textureView.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(texture)        // live preview on TextureView
        val imageSurface = imageReader.surface       // frames for processing

        val surfaces = listOf(previewSurface, imageSurface)

        try {
            device.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return
                        captureSession = session

                        try {
                            val request = device.createCaptureRequest(
                                CameraDevice.TEMPLATE_PREVIEW
                            ).apply {
                                addTarget(previewSurface)
                                addTarget(imageSurface)
                                set(
                                    CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                )
                            }

                            session.setRepeatingRequest(
                                request.build(),
                                null,
                                cameraHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        } catch (e: IllegalStateException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        // Optional: log or toast
                    }
                },
                cameraHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }
}
