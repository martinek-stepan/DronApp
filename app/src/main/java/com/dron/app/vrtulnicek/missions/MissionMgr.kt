package com.dron.app.vrtulnicek.missions

import android.view.LayoutInflater
import android.widget.LinearLayout
import com.dron.app.vrtulnicek.VrtulnicekActivity
import dji.sdk.missionmanager.DJIMissionManager
import kotlinx.android.synthetic.main.activity_vrtulnicek.*

/**
 * Created by smartinek on 20.4.2017.
 */
abstract class MissionMgr {
    var missionMgr: DJIMissionManager? = null
    lateinit var inflater: LayoutInflater
    lateinit var missionControls: LinearLayout

    // Initialize mission (saves inflater and missionContorols View)
    open fun initialize(activity : VrtulnicekActivity)
    {
        inflater = activity.layoutInflater
        missionControls = activity.missionsContol
    }
    var active = false

    // Saves DJIMissionManager and set mission as active
    open fun activate(mgr: DJIMissionManager?)
    {
        missionMgr = mgr
        active = true
    }

    // Cancel and cleans up after mission
    open fun deactivate() {
        cancelMission()
        MissionsMgr.activeMission = null
        missionMgr = null
        active = false
    }

    // Send mission stop if any mission is currently executed
    fun cancelMission()
    {
        if (missionMgr?.currentExecutingMission != null)
            missionMgr?.stopMissionExecution {}
    }

    // Abstract function for prepare layout show when missing is activated
    abstract fun showPrepareLayout()
    // Abstract function for mission layout when missin is in progress
    abstract fun showMissionLayout()
    // Abstract function for changing mission state
    abstract fun setState(state: MissionState)
}

enum class MissionState
{
    NONE,
    PREPARING,
    PREPARED,
    STARTING,
    IN_PROGRESS
}
