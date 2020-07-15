package com.dron.app.vrtulnicek

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Switch
import com.dron.app.R
import kotlinx.android.synthetic.main.activity_vrtulnicek.*

/**
 * Sigleton managed preferences of app in xml located in
 * /data/data/com.dron.app/shared_prefs/com.dron.app.vrtulnicek.preferences.xml
 * Only boolean preferences are implemented
 */
object SettingsMgr {


    // class representing one option in settings
    class BooleanPreference(var key: String, var value: Boolean, var default: Boolean, var switchID: Int, var onChange: (Boolean) -> Unit)

    val booleanPreferences: ArrayList<BooleanPreference> = arrayListOf(
            BooleanPreference("crop_video", true, true, R.id.settings_crop_video, { b -> activity.imageView.cropped = b; activity.imageView.adjustAspectRatio() }),
            BooleanPreference("super_brightness", true, false, R.id.settings_super_brightness, { b ->
                val windowParams = activity.window.attributes
                windowParams.screenBrightness = if (b) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity.window.attributes = windowParams
            }),
            BooleanPreference("altitude_graph",true,false,R.id.settings_altitude_graph,{b ->
                if (b) activity.graphAlt.visibility = View.VISIBLE
                else activity.graphAlt.visibility = View.GONE
            }),
            BooleanPreference("user_track",true,false,R.id.settings_user_track,MapMgr::showUserPath),
            BooleanPreference("drone_track",true,false,R.id.settings_drone_track,MapMgr::showDronePath)
    )

    lateinit var activity: VrtulnicekActivity
    lateinit var root_layout: LinearLayout
    lateinit var dronePath: Switch
    lateinit var userPath: Switch

    private lateinit var sharedPreferences: SharedPreferences
    // name of xml file
    private val PREF_FILE_KEY = "com.dron.app.vrtulnicek.preferences"


    fun initialize(_activity: VrtulnicekActivity) {
        activity = _activity
        root_layout = activity.settings_popup!!

        sharedPreferences = activity.getSharedPreferences(PREF_FILE_KEY, Context.MODE_PRIVATE)

        booleanPreferences.forEach { initBoolean(it) }
    }


    //function is called when singleton is started
    private fun initBoolean(pref: BooleanPreference) {
        with(pref) {
            val switch = root_layout.findViewById(switchID) as Switch
            value = loadBoolean(key, default)
            // if value loaded from file is not equal as default option, onchange is called to set things right in app
            if (value != default) {
                onChange(value)
            }

            switch.isChecked = value

            switch.setOnCheckedChangeListener { _, isChecked ->
                value = isChecked
                saveBoolean(key, value)
                onChange(value)
            }

            when(pref.switchID)
            {
                R.id.settings_user_track ->  userPath = switch
                R.id.settings_drone_track -> dronePath = switch
            }
        }
    }

    private fun saveBoolean(key: String, value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    private fun loadBoolean(key: String, default: Boolean): Boolean {
        return sharedPreferences.getBoolean(key, default)
    }


}