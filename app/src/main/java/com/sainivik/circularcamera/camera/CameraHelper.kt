package com.sainivik.circularcamera.camera

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import java.io.IOException
import java.util.*

class CameraHelper private constructor(builder: Builder) :
    PreviewCallback {
    private var mCamera: Camera? = null
    private var mCameraId = 0
    private var previewViewSize: Point?
    private var previewSize: Camera.Size? = null
    private var specificPreviewSize: Point?
    private var displayOrientation = 0
    private var rotation: Int
    private var additionalRotation: Int
    private var isMirror = false
    private var specificCameraId: Int? = null
    private var cameraListener: CameraListener?
    fun start() {
        synchronized(this) {
            if (mCamera != null) {
                return
            }
            mCameraId = Camera.getNumberOfCameras() - 1
            if (specificCameraId != null && specificCameraId!! <= mCameraId) {
                mCameraId = specificCameraId as Int
            }
            if (mCameraId == -1) {
                if (cameraListener != null) {
                    cameraListener!!.onCameraError(Exception("camera not found"))
                }
                return
            }
            if (mCamera == null) {
                mCamera = Camera.open(mCameraId)
            }
            displayOrientation = getCameraOri(rotation)
            mCamera!!.setDisplayOrientation(displayOrientation)
            try {
                val parameters = mCamera!!.parameters
                parameters.previewFormat = ImageFormat.NV21
                previewSize = parameters.previewSize
                val supportedPreviewSizes =
                    parameters.supportedPreviewSizes
                if (supportedPreviewSizes != null && supportedPreviewSizes.size > 0) {
                    previewSize = getBestSupportedSize(supportedPreviewSizes, previewViewSize)
                }
                parameters.setPreviewSize(previewSize!!.width, previewSize!!.height)
                val supportedFocusModes =
                    parameters.supportedFocusModes
                if (supportedFocusModes != null && supportedFocusModes.size > 0) {
                    if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                    } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
                    } else if (supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
                    }
                }
                mCamera!!.parameters = parameters
                mCamera!!.setPreviewTexture(preview!!.surfaceTexture)
                mCamera!!.setPreviewCallbackWithBuffer(this)
                mCamera!!.addCallbackBuffer(ByteArray(previewSize!!.width * previewSize!!.height * 3 / 2))
                mCamera!!.startPreview()
                if (cameraListener != null) {
                    cameraListener!!.onCameraOpened(
                        mCamera,
                        mCameraId,
                        displayOrientation,
                        isMirror
                    )
                }
            } catch (e: Exception) {
                if (cameraListener != null) {
                    cameraListener!!.onCameraError(e)
                }
            }
        }
    }

    private fun getCameraOri(rotation: Int): Int {
        var degrees = rotation * 90
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
            else -> {
            }
        }
        additionalRotation /= 90
        additionalRotation *= 90
        degrees += additionalRotation
        var result: Int
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(mCameraId, info)
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360
        } else {
            result = (info.orientation - degrees + 360) % 360
        }
        return result
    }

    fun stop() {
        synchronized(this) {
            if (mCamera == null) {
                return
            }
            mCamera!!.setPreviewCallback(null)
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
            if (cameraListener != null) {
                cameraListener!!.onCameraClosed()
            }
        }
    }

    val isStopped: Boolean
        get() {
            synchronized(this) { return mCamera == null }
        }

    fun release() {
        synchronized(this) {
            stop()
            preview = null
            specificCameraId = null
            cameraListener = null
            previewViewSize = null
            specificPreviewSize = null
            previewSize = null
        }
    }

    private fun getBestSupportedSize(
        sizes: List<Camera.Size>,
        previewViewSize: Point?
    ): Camera.Size {
        var sizes: List<Camera.Size>? = sizes
        if (sizes == null || sizes.size == 0) {
            return mCamera!!.parameters.previewSize
        }
        val tempSizes =
            sizes.toTypedArray()
        Arrays.sort(
            tempSizes
        ) { o1, o2 ->
            if (o1.width > o2.width) {
                -1
            } else if (o1.width == o2.width) {
                if (o1.height > o2.height) -1 else 1
            } else {
                1
            }
        }
        sizes = Arrays.asList(*tempSizes)
        var bestSize = sizes[0]
        var previewViewRatio: Float
        previewViewRatio = if (previewViewSize != null) {
            previewViewSize.x.toFloat() / previewViewSize.y.toFloat()
        } else {
            bestSize.width.toFloat() / bestSize.height.toFloat()
        }
        if (previewViewRatio > 1) {
            previewViewRatio = 1 / previewViewRatio
        }
        val isNormalRotate = additionalRotation % 180 == 0
        for (s in sizes) {
            if (specificPreviewSize != null && specificPreviewSize!!.x == s.width && specificPreviewSize!!.y == s.height) {
                return s
            }
            if (isNormalRotate) {
                if (Math.abs(s.height / s.width.toFloat() - previewViewRatio) < Math.abs(
                        bestSize.height / bestSize.width.toFloat() - previewViewRatio
                    )
                ) {
                    bestSize = s
                }
            } else {
                if (Math.abs(s.width / s.height.toFloat() - previewViewRatio) < Math.abs(
                        bestSize.width / bestSize.height.toFloat() - previewViewRatio
                    )
                ) {
                    bestSize = s
                }
            }
        }
        return bestSize
    }

    val supportedPreviewSizes: List<Camera.Size>?
        get() = if (mCamera == null) {
            null
        } else mCamera!!.parameters.supportedPreviewSizes

    val supportedPictureSizes: List<Camera.Size>?
        get() = if (mCamera == null) {
            null
        } else mCamera!!.parameters.supportedPictureSizes

    override fun onPreviewFrame(
        nv21: ByteArray,
        camera: Camera
    ) {
        camera.addCallbackBuffer(nv21)
        if (cameraListener != null) {
            cameraListener!!.onPreview(nv21, camera)
        }
    }

    private val textureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(
            surfaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
//            start();
            if (mCamera != null) {
                try {
                    mCamera!!.setPreviewTexture(surfaceTexture)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        override fun onSurfaceTextureSizeChanged(
            surfaceTexture: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            Log.i(
                TAG,
                "onSurfaceTextureSizeChanged: $width  $height"
            )
        }

        override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
            stop()
            return false
        }

        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
            if (mCamera != null) {
                try {
                    mCamera!!.setPreviewTexture(surfaceTexture)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            preview!!.getBitmap(
                Bitmap.createBitmap(
                    150,
                    150,
                    Bitmap.Config.ARGB_8888
                )
            )
        }
    }

    fun changeDisplayOrientation(rotation: Int) {
        if (mCamera != null) {
            this.rotation = rotation
            displayOrientation = getCameraOri(rotation)
            mCamera!!.setDisplayOrientation(displayOrientation)
            if (cameraListener != null) {
                cameraListener!!.onCameraConfigurationChanged(mCameraId, displayOrientation)
            }
        }
    }



    class Builder {
        var previewDisplayView: TextureView? = null
        var isMirror = false
        var specificCameraId: Int? = null
        var cameraListener: CameraListener? = null
        var previewViewSize: Point? = null
        var rotation = 0
        var previewSize: Point? = null
        var additionalRotation = 0
        fun previewOn(`val`: TextureView?): Builder {
            previewDisplayView = `val`
            return this
        }

        fun isMirror(`val`: Boolean): Builder {
            isMirror = `val`
            return this
        }

        fun previewSize(`val`: Point?): Builder {
            previewSize = `val`
            return this
        }

        fun previewViewSize(`val`: Point?): Builder {
            previewViewSize = `val`
            return this
        }

        fun rotation(`val`: Int): Builder {
            rotation = `val`
            return this
        }

        fun additionalRotation(`val`: Int): Builder {
            additionalRotation = `val`
            return this
        }

        fun specificCameraId(`val`: Int?): Builder {
            specificCameraId = `val`
            return this
        }

        fun cameraListener(`val`: CameraListener?): Builder {
            cameraListener = `val`
            return this
        }

        fun build(): CameraHelper {
            if (previewViewSize == null) {
                Log.e(
                    TAG,
                    "previewViewSize is null, now use default previewSize"
                )
            }
            if (cameraListener == null) {
                Log.e(
                    TAG,
                    "cameraListener is null, callback will not be called"
                )
            }
            if (previewDisplayView == null) {
                throw RuntimeException("you must preview on a textureView or a surfaceView")
            }
            return CameraHelper(this)
        }
    }

    companion object {
        private const val TAG = "CameraHelper"
        var preview: TextureView? = null
    }

    init {
        preview = builder.previewDisplayView
        specificCameraId = builder.specificCameraId
        cameraListener = builder.cameraListener
        rotation = builder.rotation
        additionalRotation = builder.additionalRotation
        previewViewSize = builder.previewViewSize
        specificPreviewSize = builder.previewSize
        isMirror = builder.isMirror
        preview!!.surfaceTextureListener = textureListener
        if (isMirror) {
            preview!!.scaleX = -1f
        }
    }
}