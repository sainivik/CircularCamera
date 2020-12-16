package com.sainivik.circularcamera.camera

import android.hardware.Camera

interface CameraListener {
    /**
     * Execute when opened
     * @param camera camera instance
     * @param cameraId camera ID
     * @param displayOrientation camera preview rotation angle
     * @param isMirror Whether to mirror display
     */
    fun onCameraOpened(
        camera: Camera?,
        cameraId: Int,
        displayOrientation: Int,
        isMirror: Boolean
    )

    /**
     * Preview data callback
     * @param data preview data
     * @param camera camera instance
     */
    fun onPreview(data: ByteArray?, camera: Camera?)
    fun onCameraClosed()

    /**
     * Execute when an exception occurs
     * @param e camera related exception
     */
    fun onCameraError(e: Exception?)

    /**
     * Called when the property changes
     * @param cameraID camera ID
     * @param displayOrientation camera rotation direction
     */
    fun onCameraConfigurationChanged(cameraID: Int, displayOrientation: Int)
}