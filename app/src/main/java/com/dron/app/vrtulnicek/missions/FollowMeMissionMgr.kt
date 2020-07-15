package com.dron.app.vrtulnicek.missions

import android.graphics.Color
import android.location.Location
import android.support.constraint.ConstraintLayout
import android.util.Log
import android.view.View
import com.dron.app.R
import com.dron.app.example.common.Utils
import com.dron.app.example.common.Utils.setResultToToast
import com.dron.app.vrtulnicek.App
import com.dron.app.vrtulnicek.PositionsMgr
import com.dron.app.vrtulnicek.PositionsMgr.userPosition
import com.dron.app.vrtulnicek.VrtulnicekActivity
import com.dron.app.vrtulnicek.utils.HTMLText
import com.dron.app.vrtulnicek.utils.toast
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.error.DJIError
import dji.common.flightcontroller.DJILocationCoordinate3D
import dji.sdk.missionmanager.DJIFollowMeMission
import dji.sdk.missionmanager.DJIMission
import dji.sdk.missionmanager.DJIMissionManager
import kotlinx.android.synthetic.main.follow_me_mission.view.*
import kotlinx.android.synthetic.main.follow_me_mission_running.view.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay

/**
 * Created by Kexik on 21.04.2017.
 */
object FollowMeMissionMgr: MissionMgr()
{
    lateinit var lastUserLocation: Location
    private var state : MissionState = MissionState.NONE

    var mission: DJIFollowMeMission? = null

    override fun initialize(activity : VrtulnicekActivity)
    {
        super.initialize(activity)
    }

    // User position listener function
    private fun userPositionListener(loc: Location)
    {
        lastUserLocation = loc
    }

    // Listener for mission finnish callback from DJI SDK
    private fun onMissionExecutionFinished(error : DJIError?) {
        Utils.setResultToToast(missionControls.context, "Execution finished: " + if (error == null) "Success!" else error.description)
        setState(MissionState.NONE)
        mainThread(CommonPool) {

            mission = null
            showPrepareLayout()
        }
    }


    // Listener for mission update callback from DJI SDK
    private fun onMissionProgressStatusUpdate(progressStatus : DJIMission.DJIMissionProgressStatus)
    {
        if (progressStatus !is DJIFollowMeMission.DJIFollowMeMissionStatus)
            return

        mainThread(CommonPool) {
            val html = HTMLText("<b>Flight state<b>: " + progressStatus.executionState.name).
                    addLine("<b>Horizontal distance<b>: " + progressStatus.horizontalDistance).
                    addLine("<b>Error<b>: " + if (progressStatus.error == null) "No Errors" else progressStatus.error.description)
            missionControls.missionInfo?.text = html.getSpanned()
        }
    }

    // Start mission button onclick listener. Checks if prequesites are OK and try to prepare and then start mission
    fun onButtonClick(v : View)
    {
        if (missionMgr == null)
        {
            missionMgr = App.getProductInstance()?.missionManager
        }
        if (missionMgr == null || PositionsMgr.dronePosition == null || PositionsMgr.userPosition == null)
        {
            var text = "Mgr ${missionMgr == null} drone pos  ${PositionsMgr.dronePosition == null} User pos ${PositionsMgr.userPosition == null}"

            v.toast("Fail! "+ text)
            return
        }
        lastUserLocation = PositionsMgr.userPosition!!
        setState(MissionState.PREPARING)
        mission = DJIFollowMeMission(userPosition!!.latitude, userPosition!!.longitude)
        mission?.heading = if (missionControls.radioFixed?.isChecked ?: false) DJIFollowMeMission.DJIFollowMeHeading.TowardFollowPosition else DJIFollowMeMission.DJIFollowMeHeading.ControlledByRemoteController
        missionMgr?.prepareMission(mission,null,{error ->
            if (error != null)
            {
                setState(MissionState.NONE)
            }
            else
            {
                setState(MissionState.STARTING)
                missionMgr?.startMissionExecution({ error ->
                    if (error == null)
                        setState(MissionState.IN_PROGRESS)
                    else if (missionMgr?.currentExecutingMission != null)
                        missionMgr?.stopMissionExecution{ setState(MissionState.NONE) }
                    else
                        setState(MissionState.NONE)

                    setResultToToast(v.context, "Start: " + if (error == null) "Success" else error.description)
                })
            }
            Utils.setResultToToast(v.context, "Preparation: " +if (error == null) "Success" else error.description)
        })
    }

    // Drone position listener for altitude update
    private fun dronePositionListener(loc: DJILocationCoordinate3D, h: Double) = updateAltitude(loc.altitude)

    override fun activate(mgr : DJIMissionManager?)
    {
        super.activate(mgr)
        missionMgr?.setMissionExecutionFinishedCallback(FollowMeMissionMgr::onMissionExecutionFinished)
        missionMgr?.setMissionProgressStatusCallback(FollowMeMissionMgr::onMissionProgressStatusUpdate)

        PositionsMgr.addOnUserLocationChangeListener(FollowMeMissionMgr::userPositionListener)
        PositionsMgr.addOnDroneLocationChangeListener(FollowMeMissionMgr::dronePositionListener)

        showPrepareLayout()
        setState(MissionState.NONE)

        async(CommonPool){
            while(active){
                delay(100)
                if (mission != null) mainThread(CommonPool)
                {
                    DJIFollowMeMission.updateFollowMeCoordinate(lastUserLocation.latitude, lastUserLocation.longitude, {error ->
                        if (error != null) Log.e("followme","Follow me update error: "+error.description)
                    })
                }
            }
        }
    }

    override fun deactivate()
    {
        super.deactivate()
        cancelMission()
        missionMgr?.setMissionExecutionFinishedCallback(null)
        missionMgr?.missionProgressStatusCallback = null

        PositionsMgr.removeOnUserLocationChangeListener(FollowMeMissionMgr::userPositionListener)
        PositionsMgr.removeOnDronLocationChangeListener(FollowMeMissionMgr::dronePositionListener)

        MissionsMgr.initUI()
        setState(MissionState.NONE)
    }

    // Change state and modify layout according to it
    override fun setState(state : MissionState)
    {
        mainThread(CommonPool) {
            this.state = state
            when (state)
            {
                MissionState.NONE ->
                {
                    missionControls.radioGimballContoll?.isEnabled = true
                    missionControls.backToMissions?.isEnabled = true
                }
                MissionState.PREPARING, MissionState.STARTING ->
                {
                    missionControls.radioGimballContoll?.isEnabled = false
                    missionControls.backToMissions?.isEnabled = false
                }
                MissionState.IN_PROGRESS ->
                {
                    showMissionLayout()
                }
                else -> {}
            }
        }
    }

    // Update latitude in TextView and enable/disable start button according to it
    fun updateAltitude(altitude: Float)
    {
        val color = if (altitude > 10) Color.GREEN else Color.RED
        missionControls.altitudeInfoPrep?.text = "Altitude: $altitude"
        missionControls.altitudeInfoPrep?.setTextColor(color)
        missionControls.altitudeInfo?.text = "Altitude: $altitude"
        missionControls.altitudeInfo?.setTextColor(color)
        if (state == MissionState.NONE)
            missionControls.followMeStart?.isEnabled = altitude > 10

    }

    override fun showPrepareLayout()
    {
        missionControls.removeAllViews()
        val layout = inflater.inflate(R.layout.follow_me_mission, null) as ConstraintLayout
        layout.backToMissions.setOnClickListener{deactivate()}
        layout.followMeStart?.setOnClickListener(FollowMeMissionMgr::onButtonClick)
        missionControls.addView(layout)
        updateAltitude(PositionsMgr.dronePosition?.altitude ?: 0F)
    }

    override fun showMissionLayout()
    {
        missionControls.removeAllViews()
        val layout = inflater.inflate(R.layout.follow_me_mission_running, null) as ConstraintLayout
        layout.backToPrepare.setOnClickListener{ cancelMission(); showPrepareLayout()}
        missionControls.addView(layout)
        updateAltitude(PositionsMgr.dronePosition?.altitude ?: 0F)
    }
}