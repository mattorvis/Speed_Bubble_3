package com.example.speed_bubble_3

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import android.widget.Button
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_OVERLAY_PERMISSION = 101

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private var bubbleService: BubbleService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder = iBinder as BubbleService.LocalBinder
            bubbleService = binder.getService()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bubbleService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = "The Bubble of Dreams"


        checkOverlayPermission()

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission()
        } else {
            // Permission already granted, start your service
        }

        val toggleSpeedUnitButton = findViewById<Button>(R.id.toggle_speed_unit_button)
        toggleSpeedUnitButton.setOnClickListener {
            bubbleService?.toggleSpeedUnit()
        }

        bindBubbleService()

        val btnStopBubble = findViewById<Button>(R.id.btn_stop_bubble)
        btnStopBubble.setOnClickListener {
            stopBubble()
        }

        val btnRestartBubble = findViewById<Button>(R.id.btn_restart_bubble)
        btnRestartBubble.setOnClickListener {
            restartBubble()
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start your service
            } else {
                // Permission denied, show a message or disable the feature
            }
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        } else {
            startBubbleService()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                startBubbleService()
            } else {
                Toast.makeText(this, "Overlay permission not granted ya ding dong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startBubbleService() {
        val intent = Intent(this, BubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun bindBubbleService() {
        Intent(this, BubbleService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unbindBubbleService() {
        unbindService(serviceConnection)
        bubbleService = null
    }

    private fun stopBubble() {
        bubbleService?.let {
            // Stop the BubbleService
            val intent = Intent(this, BubbleService::class.java)
            stopService(intent)
            unbindBubbleService()
        }
    }

    private fun restartBubble() {
        if (bubbleService == null) {
            startBubbleService()
        } else {
            bubbleService?.restartBubble()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindBubbleService()
    }
}
