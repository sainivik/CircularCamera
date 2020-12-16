package com.sainivik.circularcamera.activity

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Point
import android.hardware.Camera
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import com.sainivik.circularcamera.R
import com.sainivik.circularcamera.camera.CameraHelper
import com.sainivik.circularcamera.camera.CameraListener
import com.sainivik.circularcamera.widget.RoundBorderView
import com.sainivik.circularcamera.widget.RoundTextureView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class CameraActivity : AppCompatActivity(), OnGlobalLayoutListener, CameraListener,
    OnSeekBarChangeListener {
    private var cameraHelper: CameraHelper? = null
    private var textureView: RoundTextureView? = null
    private var roundBorderView: RoundBorderView? = null
    var camera: Camera? = null
    var data: ByteArray? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        initView()
        setListener()
    }

    private fun setListener() {
        val seekBar = findViewById<View>(R.id.radiusSeekBar) as SeekBar
        seekBar.setOnSeekBarChangeListener(this@CameraActivity)
        seekBar.post { seekBar.progress = seekBar.max }
        val btn =
            findViewById<View>(R.id.button) as Button
        btn.setOnClickListener { v: View? ->
            try {
                capture()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun initView() {
        textureView = findViewById(R.id.texture_preview)
        textureView!!.getViewTreeObserver().addOnGlobalLayoutListener(this)
    }

    fun initCamera() {
        cameraHelper = CameraHelper.Builder()
            .cameraListener(this)
            .specificCameraId(CAMERA_ID)
            .previewOn(textureView)
            .previewViewSize(
                Point(
                    textureView!!.layoutParams.width,
                    textureView!!.layoutParams.height
                )
            )
            .rotation(windowManager.defaultDisplay.rotation)
            .build()
        cameraHelper!!.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        initCamera()
    }

    override fun onGlobalLayout() {
        textureView!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
        val layoutParams = textureView!!.layoutParams
        val sideLength =
            Math.min(textureView!!.width, textureView!!.height) * 3 / 4
        layoutParams.width = sideLength
        layoutParams.height = sideLength
        textureView!!.layoutParams = layoutParams
        textureView!!.turnRound()
        initCamera()
    }

    override fun onPause() {
        if (cameraHelper != null) {
            cameraHelper!!.stop()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (cameraHelper != null) {
            cameraHelper!!.start()
        }
    }

    private var previewSize: Camera.Size? = null
    override fun onCameraOpened(
        camera: Camera?,
        cameraId: Int,
        displayOrientation: Int,
        isMirror: Boolean
    ) {
        previewSize = camera!!.parameters.previewSize
        Log.i(
            TAG,
            "onCameraOpened:  previewSize = " + previewSize!!.width + "x" + previewSize!!.height
        )
        runOnUiThread(object : Runnable {
            override fun run() {
                run {
                    val layoutParams = textureView!!.layoutParams
                    if (displayOrientation % 180 == 0) {
                        layoutParams.height =
                            layoutParams.width * previewSize!!.height / previewSize!!.width
                    } else {
                        layoutParams.height =
                            layoutParams.width * previewSize!!.width / previewSize!!.height
                    }
                    textureView!!.layoutParams = layoutParams
                }
                roundBorderView = RoundBorderView(this@CameraActivity)
                (textureView!!.parent as FrameLayout).addView(
                    roundBorderView,
                    textureView!!.layoutParams
                )
            }
        })
    }

    override fun onPreview(
        nv21: ByteArray?,
        camera: Camera?
    ) {
        data = nv21
        this.camera = camera
    }

    override fun onCameraClosed() {
        Log.i(TAG, "onCameraClosed: ")
    }

    override fun onCameraError(e: Exception?) {
        e!!.printStackTrace()
    }

    override fun onCameraConfigurationChanged(
        cameraID: Int,
        displayOrientation: Int
    ) {
    }

    override fun onDestroy() {
        if (cameraHelper != null) {
            cameraHelper!!.release()
        }
        super.onDestroy()
    }

    override fun onProgressChanged(
        seekBar: SeekBar,
        progress: Int,
        fromUser: Boolean
    ) {
        textureView!!.radius = progress * Math.min(
            textureView!!.width,
            textureView!!.height
        ) / 2 / seekBar.max
        textureView!!.turnRound()
        roundBorderView!!.setRadius(
            progress * Math.min(
                roundBorderView!!.width,
                roundBorderView!!.height
            ) / 2 / seekBar.max
        )
        roundBorderView!!.turnRound()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}
    override fun onStopTrackingTouch(seekBar: SeekBar) {}

    @Throws(IOException::class)
    private fun capture(): Boolean {
        val bitmap: Bitmap = CameraHelper.preview!!.bitmap!!
        val file_path =
            Environment.getExternalStorageDirectory().absolutePath +
                    "/WhideImages"
        val dir = File(file_path)
        if (!dir.exists()) dir.mkdirs()
        val file =
            File(dir, "whide" + Calendar.getInstance().timeInMillis + ".jpg")
        val fOut = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 99, fOut)
        fOut.flush()
        fOut.close()
        (findViewById<View>(R.id.ivImage) as ImageView).setImageBitmap(
            bitmap
        )
        return true
    }

    companion object {
        private const val TAG = "CameraActivity"

        // private SeekBar radiusSeekBar;
        private const val CAMERA_ID = Camera.CameraInfo.CAMERA_FACING_FRONT
    }
}