package com.example.speed_bubble_3

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import kotlin.math.abs

import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat.checkSelfPermission
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager

import android.os.Binder

private const val NOTIFICATION_CHANNEL_ID = "BubbleServiceChannel"

class BubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: View
    private lateinit var bubbleText: TextView
    private lateinit var layoutParams: WindowManager.LayoutParams

    private lateinit var locationManager: LocationManager
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val speedInMetersPerSecond = location.speed
            val speed = if (displayKmh) {
                speedInMetersPerSecond * 3.6 // Convert to km/h
            } else {
                speedInMetersPerSecond * 2.23694 // Convert to mph
            }
            val speedUnit = if (displayKmh) "km/h" else "mph"
            bubbleText.text = "%.2f %s".format(speed, speedUnit)
        }
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private var displayKmh: Boolean = true // Set the default unit to km/h

    inner class LocalBinder : Binder() {
        fun getService(): BubbleService = this@BubbleService
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bubble Service Channel"
            val descriptionText = "Channel for Bubble Service"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return LocalBinder()
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Bubble Overlay")
            .setContentText("Bubble overlay service is running and crying")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true) // Add this line to make the notification ongoing
            .build()



        startForeground(1, notification)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)
        bubbleText = bubbleView.findViewById(R.id.bubble_text)

        // Initialize the LocationManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // Check for location permission
        if (checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, locationListener)
        } else {
            // Request permission from user
            // NOTE: You should handle this situation in your main activity
        }

        layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 1000
        layoutParams.y = 100

        windowManager.addView(bubbleView, layoutParams)

        setUpBubbleDragListener(layoutParams)
        //startUpdatingNumber()

        createBubble()
    }

    private fun setUpBubbleDragListener(layoutParams: WindowManager.LayoutParams) {
        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            private var lastAction: Int = 0
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private val CLICK_THRESHOLD = 150
            private var isClick: Boolean = true

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastAction = event.action
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            // Bubble was clicked, handle the click event if necessary
                            handleBubbleClick()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(bubbleView, layoutParams)
                        lastAction = event.action
                        isClick = isClick && abs(event.rawX - initialTouchX) < CLICK_THRESHOLD && abs(event.rawY - initialTouchY) < CLICK_THRESHOLD
                        return true
                    }
                }
                return false
            }
        })
    }

    /*private fun startUpdatingNumber() {
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            var count = 0

            override fun run() {
                count++
                bubbleText.text = count.toString()
                handler.postDelayed(this, 1000) // Update every 1000 milliseconds (1 second)
            }
        }
        handler.post(runnable)
    }*/

    fun restartBubble() {
        windowManager.removeView(bubbleView)
        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)
        bubbleText = bubbleView.findViewById(R.id.bubble_text)

        val layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        } else {
            WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
        }

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 1000
        layoutParams.y = 100

        windowManager.addView(bubbleView, layoutParams)
        setUpBubbleDragListener(layoutParams)
    }

    fun createBubble() {
        if (!::bubbleView.isInitialized) {
            bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)
            bubbleText = bubbleView.findViewById(R.id.bubble_text)

            val layoutParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
            } else {
                WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
            }

            layoutParams.gravity = Gravity.TOP or Gravity.START
            layoutParams.x = 1000
            layoutParams.y = 100

            windowManager.addView(bubbleView, layoutParams)
            setUpBubbleDragListener(layoutParams)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(bubbleView)
        locationManager.removeUpdates(locationListener) // Remove location updates
    }

    private fun handleBubbleClick() {
        Toast.makeText(this, "Stop touching my lil Bubble", Toast.LENGTH_SHORT).show()
    }

    fun toggleSpeedUnit() {
        displayKmh = !displayKmh
    }
}

