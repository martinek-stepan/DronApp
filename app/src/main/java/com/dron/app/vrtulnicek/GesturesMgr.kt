package com.dron.app.vrtulnicek

import android.graphics.Point
import android.util.Log
import android.view.MotionEvent
import dji.common.gimbal.DJIGimbalRotateDirection
import dji.common.gimbal.DJIGimbalSpeedRotation
import kotlinx.android.synthetic.main.activity_vrtulnicek.*
import java.util.*

/**
 * Gestures for rotationg camera when user touch drag videofeed. Doesnt work on Phantom 3.
 */
object GesturesMgr {

    lateinit var activity : VrtulnicekActivity

    var gestureStartX: Float = 0f
    var gestureStartY: Float = 0f
    var maxSpeedPx: Float = 300f

    var camera_speed_timer = Timer()

    fun initialize(_activity: VrtulnicekActivity) {
        activity = _activity
        initGestureControl()

    }

    private fun initGestureControl() {
        //one third of shorter dimension of screen is maximum gesture length
        val displaydim = Point()
        activity.windowManager.defaultDisplay.getSize(displaydim)
        maxSpeedPx = minOf(displaydim.x.toFloat(), displaydim.y.toFloat()) / 3

        //event setup
        activity.imageView.setOnTouchListener { _, event ->
            if (!MapSwapper.inMinimapMode) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    gestureStartX = event.x; gestureStartY = event.y
                }
                MotionEvent.ACTION_UP -> setCameraMoveSpeed(0f, 0f)
                MotionEvent.ACTION_MOVE -> setCameraMoveSpeed(event.x - gestureStartX, event.y - gestureStartY)
                else -> return@setOnTouchListener false
            }
            return@setOnTouchListener true // v pripade DOWN UP MOVE
        }
    }

    fun setCameraMoveSpeed(xPX: Float, yPX: Float) {
        // output is in range -1 1
        val x = maxOf(minOf((xPX / maxSpeedPx), 1f), -1f)
        val y = maxOf(minOf((yPX / maxSpeedPx), 1f), -1f)
        // constant
        val C = 100F

        val pitch = DJIGimbalSpeedRotation(-y * C, DJIGimbalRotateDirection.Clockwise)
        val roll = DJIGimbalSpeedRotation(0F, DJIGimbalRotateDirection.Clockwise)
        val yaw = DJIGimbalSpeedRotation(x * C, DJIGimbalRotateDirection.Clockwise)

        val new_timer_task = GimbalRotateTimerTask(pitch,roll,yaw)

        camera_speed_timer.cancel()
        camera_speed_timer.purge()
        if (x!=0f && y!=0f) {
            camera_speed_timer = Timer()
            camera_speed_timer.schedule(new_timer_task, 0, 50)
            // 50 je tak akorat aby se to necukalo na matrici
        }

        //Log.d("camera_gesture", "Set camera move speed $x:$y")
    }
}