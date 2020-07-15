package com.dron.app.vrtulnicek

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ToggleButton
import com.dron.app.R
import com.dron.app.example.common.DJISampleApplication
import com.dron.app.vrtulnicek.missions.MissionsMgr
import com.dron.app.vrtulnicek.utils.isClickInsideView
import kotlinx.android.synthetic.main.activity_vrtulnicek.*

typealias App = DJISampleApplication

/* Main content activity */
class VrtulnicekActivity : Activity() {


    var battery_popup : LinearLayout? =null
    var controller_signal_popup : LinearLayout? =null
    var settings_popup : LinearLayout? =null
    var smart_functions_popup : LinearLayout? =null
    var controller_battery_popup : LinearLayout? =null
    var video_signal_popup : LinearLayout? =null
    var drone_signal_popup : LinearLayout? =null


    var selfie_follow : ToggleButton? = null

    /* Initialize UI and Singletons */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_vrtulnicek)

        setScreenParams()
        CallbacksMgr.initilize()
        initToolbar()

        CameraMgr.initialize(this@VrtulnicekActivity)
        GesturesMgr.initialize(this@VrtulnicekActivity)
        PositionsMgr.initialize(this)
        GraphMgr.initialize(this)
        MapMgr.initialize(mapView,this, toolBarLL.layoutParams.height, savedInstanceState)
        MapSwapper.initialize(imageView,mapView,cameraControls,mutableListOf(cameraControls, toolBarLL, flight_data, flightWarnings,graphAlt,missionsContolOuter))

        PhoneBatteryMgr.initialize(this,phone_battery)
        MissionsMgr.initialize(this)
        FlightDataMgr.initialize(this)
        FlightWarning.initialize(this)
    }

    fun initToolbar() {
        rootLayout.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        rootLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        initAfterLayoutComplete()
                    }
                }
        )
    }
    // this is called after inflating whole layout
    private fun initAfterLayoutComplete() {
        ToolbarMgr.initialize(this@VrtulnicekActivity)
        selfie_follow = smart_functions_popup?.findViewById(R.id.selfieFollow) as ToggleButton
        SelfieMgr.initialize(selfie_follow!!)
        SmartFunctionsMgr.initialize(this@VrtulnicekActivity)
        SettingsMgr.initialize(this@VrtulnicekActivity)
    }

    public override fun onResume() {
        super.onResume()
        setScreenParams()
        MapMgr.onResume()
        PositionsMgr.onResume()
    }

    public override fun onDestroy() {
        super.onDestroy()
        MapMgr.onDestroy()
        PhoneBatteryMgr.deinitialize()
    }

    override fun onPause() {
        super.onPause()
        MapMgr.onPause()
        PositionsMgr.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        MapMgr.onLowMemory()
    }

    //catching events when user click outside of popup, popus are hidden after that
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus!=null) {
            if (! isClickInsideView(ev!!.x,ev.y,currentFocus) && currentFocus.getTag(R.id.toolbar_parent) != null) {

                val toolbarParentId = currentFocus.getTag(R.id.toolbar_parent)?.toString()?.toInt()
                if (toolbarParentId!=null) {
                    if (!isClickInsideView(ev.x, ev.y, findViewById(toolbarParentId))) {
                        this.currentFocus?.visibility = View.INVISIBLE
                    } else {
                        this.currentFocus?.visibility = View.INVISIBLE
                        return true
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    // fullscreen and immersive mode (statusbar and soft keys are hidden)
    fun setScreenParams() {
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }


}