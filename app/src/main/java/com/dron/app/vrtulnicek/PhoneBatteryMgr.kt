package com.dron.app.vrtulnicek

import android.content.BroadcastReceiver
import android.content.Context
import com.dron.app.vrtulnicek.views.IconInfoView
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter


/**
 * Singleton managing battery reading from Android device
 * this is done by reginstering BroadcastReceiver and
 * filtering Intent.ACTION_BATTERY_CHANGED
 */
object PhoneBatteryMgr {

    lateinit var batteryIcon : IconInfoView
    lateinit var ctx : Context

    val batteryInfoReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            batteryIcon.percentage = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1)
        }
    }

    fun initialize(_ctx : Context, _batteryIcon : IconInfoView)
    {
        batteryIcon = _batteryIcon
        ctx=_ctx
        ctx.registerReceiver(this.batteryInfoReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    fun deinitialize()
    {
        ctx.unregisterReceiver(this.batteryInfoReceiver)
    }

}