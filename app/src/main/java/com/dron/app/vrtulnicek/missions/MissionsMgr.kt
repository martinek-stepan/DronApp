package com.dron.app.vrtulnicek.missions

import android.support.constraint.ConstraintLayout
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.dron.app.R
import com.dron.app.vrtulnicek.App
import com.dron.app.vrtulnicek.MapSwapper
import com.dron.app.vrtulnicek.VrtulnicekActivity
import dji.sdk.missionmanager.DJIMissionManager
import kotlinx.android.synthetic.main.activity_vrtulnicek.*
import kotlinx.android.synthetic.main.mission_button.view.*

/* Missions manager handles base mission selection UI, activating specified mission and canceling current mission when needed */

object MissionsMgr
{
    internal class Mission(var text: String,var mgr: MissionMgr, var icon: Int = R.mipmap.ic_launcher)

    var missionManager: DJIMissionManager? = null
    lateinit var inflater: LayoutInflater
    lateinit var missionControls: LinearLayout
    var activeMission: MissionMgr? = null

    private val missions = arrayListOf(Mission("Follow me", FollowMeMissionMgr, R.drawable.ic_folow),Mission("POI", HotPointMissionMgr, R.drawable.ic_poi),Mission("Waypoints mission", WaypointMissionMgr,R.drawable.ic_waypoints))

    var panoramaEnabled = false

    // Initialize singleton and all available missions singletons
    fun initialize(activity : VrtulnicekActivity)
    {
        inflater = activity.layoutInflater
        missionControls = activity.missionsContol

        initUI()

        MapSwapper.addFrontStuff(activity.missionsContol)

        // Initialize mission manager and his callbacks
        missionManager = App.getProductInstance()?.missionManager

        //ActiveTrackMissionMgr.initialize(activity)
        //TapFlyMissionMgr.initialize(activity)
        FollowMeMissionMgr.initialize(activity)
        HotPointMissionMgr.initialize(activity)
        WaypointMissionMgr.initialize(activity)

        panoramaEnabled = App.getAircraftInstance()?.camera?.isTimeLapseSupported ?: false
    }

    // Shows UI with specific mission buttons
    fun initUI()
    {
        missionControls.removeAllViews()
        for (mission in missions)
        {
            val button = inflater.inflate(R.layout.mission_button, null) as ConstraintLayout
            button.missionName.text = mission.text
            button.missionImage.setImageResource(mission.icon)
            button.missionButt.setOnClickListener {
                activeMission?.deactivate()
                activeMission = mission.mgr
                mission.mgr.activate(missionManager)
            }
            missionControls.addView(button)
        }
    }

    // Cancle current mission af any
    fun cancelCurrentMission(){
        activeMission?.deactivate()
    }
}