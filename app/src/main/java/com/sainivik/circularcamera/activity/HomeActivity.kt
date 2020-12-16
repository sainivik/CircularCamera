package com.sainivik.circularcamera.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.sainivik.circularcamera.R
import com.sainivik.circularcamera.databinding.ActivityHomeBinding
import java.util.*


class HomeActivity : AppCompatActivity() {
    private val ACTION_REQUEST_PERMISSIONS = 2435
    lateinit var binding: ActivityHomeBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_home
        )

        setListener()
    }


    private fun setListener() {
        // Set up the listener for take photo button
        binding.cameraCaptureButton.setOnClickListener {
            if (checkAndRequestPermissions(this)) {
                startActivity(Intent(this, CameraActivity::class.java))
            }


        }
    }


    fun checkAndRequestPermissions(context: Activity?): Boolean {
        val ExtstorePermission = ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val cameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        val listPermissionsNeeded: MutableList<String> =
            ArrayList()
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (ExtstorePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded
                .add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                context, listPermissionsNeeded
                    .toTypedArray(), ACTION_REQUEST_PERMISSIONS

            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACTION_REQUEST_PERMISSIONS -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkAndRequestPermissions(this)) {
                        startActivity(Intent(this, CameraActivity::class.java))
                    }

                } else {
                    checkAndRequestPermissions(this)
                }


            }
        }
    }

}