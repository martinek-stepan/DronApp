package com.dron.app.vrtulnicek.missions

import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.view.View
import com.dron.app.R
import com.dron.app.example.common.Utils
import com.dron.app.vrtulnicek.App
import com.dron.app.vrtulnicek.FlightDataMgr
import com.dron.app.vrtulnicek.PositionsMgr
import com.dron.app.vrtulnicek.VrtulnicekActivity
import com.dron.app.vrtulnicek.utils.HTMLText
import com.dron.app.vrtulnicek.utils.format
import com.dron.app.vrtulnicek.utils.toast
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.error.DJIError
import dji.common.flightcontroller.DJILocationCoordinate3D
import dji.sdk.missionmanager.DJIHotPointMission
import dji.sdk.missionmanager.DJIMission
import dji.sdk.missionmanager.DJIMissionManager
import kotlinx.android.synthetic.main.follow_me_mission_running.view.*
import kotlinx.android.synthetic.main.hotpoint_mission1.view.*
import kotlinx.android.synthetic.main.hotpoint_mission2.view.*
import kotlinx.coroutines.experimental.CommonPool

/**
 * Created by qwerty on 23. 4. 2017.
 */


object HotPointMissionMgr: MissionMgr() {
    var hotpoint:DJILocationCoordinate3D? = null
    private var state: MissionState = MissionState.NONE

    var missionAltitude:Float = 10f
    var missionDistance:Float = 10f
    var missionSpeed:Double = 50.0

    var mission: DJIHotPointMission? = null

    override fun initialize(activity: VrtulnicekActivity) {
        super.initialize(activity)
    }

    // Listener for mission finnish callback from DJI SDK
    private fun onMissionExecutionFinished(error: DJIError?) {
        Utils.setResultToToast(missionControls.context, "Execution finished: " + if (error == null) "Success!" else error.description)
        setState(MissionState.NONE)
        mainThread(CommonPool) {
            mission = null
            showPrepareLayout()
        }
    }

    // Listener for mission update callback from DJI SDK
    private fun onMissionProgressStatusUpdate(progressStatus: DJIMission.DJIMissionProgressStatus) {
        if (progressStatus !is DJIHotPointMission.DJIHotPointMissionStatus)
            return

        mainThread(CommonPool) {
            val html = HTMLText("<b>Flight state<b>: " + progressStatus.executionState.name).
                    addLine("<b>Hotpoint distance<b>: " + progressStatus.currentDistanceToHotpoint).
                    addLine("<b>Error<b>: " + if (progressStatus.error == null) "No Errors" else progressStatus.error.description)
            missionControls.missionInfo?.text = html.getSpanned()
        }
    }

    fun onButtonClick(v: View)
    {
        if (missionMgr == null || PositionsMgr.dronePosition == null || App.getProductInstance() == null)
        {
            v.toast("${FollowMeMissionMgr.missionMgr} ${PositionsMgr.dronePosition} ${App.getProductInstance()} ")
            return
        }


        setState(MissionState.PREPARING)
        mission = DJIHotPointMission()
        mission!!.longitude = hotpoint!!.longitude
        mission!!.latitude = hotpoint!!.latitude
        mission!!.altitude = missionAltitude.toDouble()
        mission!!.radius = missionDistance.toDouble()
        val angleSpeed = missionSpeed * DJIHotPointMission.maxAngularVelocityForRadius(missionDistance.toDouble()) / 100
        mission!!.angularVelocity = Math.abs(angleSpeed).toFloat()
        mission!!.isClockwise = (angleSpeed<0)
        mission!!.startPoint = DJIHotPointMission.DJIHotPointStartPoint.Nearest
        mission!!.heading = DJIHotPointMission.DJIHotPointHeading.TowardsHotPoint

        missionMgr?.prepareMission(mission, null, { error ->
            if (error != null) {
                setState(MissionState.NONE)
            } else {
                setState(MissionState.STARTING)
                missionMgr?.startMissionExecution({ error ->
                    if (error == null)
                        setState(MissionState.IN_PROGRESS)
                    else if (missionMgr?.currentExecutingMission != null)
                        missionMgr?.stopMissionExecution { setState(MissionState.NONE) }
                    else
                        setState(MissionState.NONE)

                    Utils.setResultToToast(v.context, "Start: " + if (error == null) "Success" else error.description)
                })
            }
            Utils.setResultToToast(v.context, "Preparation: " + if (error == null) "Success" else error.description)
        })
    }

    private fun dronePositionListener(loc: DJILocationCoordinate3D, h: Double) {
        updateAltitude(loc.altitude)
        if (hotpoint!=null) {
            val dist = FlightDataMgr.distanceBetween(loc.latitude.toFloat(), hotpoint!!.latitude.toFloat(),loc.longitude.toFloat(), hotpoint!!.longitude.toFloat(),0f,0f)
            updateDistance(dist)
        }
        updateSpeedInfo(missionSpeed)

    }



    override fun activate(mgr: DJIMissionManager?) {
        super.activate(mgr)
        missionMgr?.setMissionExecutionFinishedCallback(HotPointMissionMgr::onMissionExecutionFinished)
        missionMgr?.setMissionProgressStatusCallback(HotPointMissionMgr::onMissionProgressStatusUpdate)


        PositionsMgr.addOnDroneLocationChangeListener(HotPointMissionMgr::dronePositionListener)

        showPrepareLayout()
        setState(MissionState.NONE)


    }

    override fun deactivate() {
        super.deactivate()
        cancelMission()
        missionMgr?.setMissionExecutionFinishedCallback(null)
        missionMgr?.missionProgressStatusCallback = null

        //PositionsMgr.removeOnUserLocationChangeListener(HotPointMissionMgr::userPositionListener)
        PositionsMgr.removeOnDronLocationChangeListener(HotPointMissionMgr::dronePositionListener)

        MissionsMgr.initUI()
        setState(MissionState.NONE)
    }

    override fun setState(state: MissionState) {
        mainThread(CommonPool) {
            this.state = state
            when (state) {
                MissionState.NONE -> {
                  //  missionControls.radioGimballContoll?.isEnabled = true
                }
                MissionState.PREPARING, MissionState.STARTING -> {
                 //   missionControls.radioGimballContoll?.isEnabled = false
                    missionControls.backToMissions?.isEnabled = false
                }
                MissionState.IN_PROGRESS -> {
                    showMissionLayout()
                }

                else -> {}
            }
        }
    }

    fun updateAltitude(altitude: Float) {
        val color = if (altitudeOK(altitude)) Color.GREEN else Color.RED
        missionControls.altitudeInfoPrep?.text = "Altitude: $altitude"
        missionControls.altitudeInfoPrep?.setTextColor(color)
        missionControls.altitudeInfo?.text = "Altitude: $altitude"
        missionControls.altitudeInfo?.setTextColor(color)
        missionAltitude = altitude
        if (state == MissionState.NONE)
            checkAndReady()

    }
    private fun  updateDistance(distance: Float) {
        val color = if (distanceOK(distance)) Color.GREEN else Color.RED
        missionControls.distanceInfoPrep?.text = "Distance: $distance"
        missionControls.distanceInfoPrep?.setTextColor(color)
        missionDistance = distance
        if (state == MissionState.NONE)
            checkAndReady()
    }



    private fun checkAndReady() {
        missionControls.hotPointStart?.isEnabled =
                (distanceOK(missionDistance)&& altitudeOK(missionAltitude))
    }

    private fun distanceOK(distance : Float) = (distance > DJIHotPointMission.DJI_HOTPOINT_MIN_RADIUS && distance < DJIHotPointMission.DJI_HOTPOINT_MAX_RADIUS)
    private fun altitudeOK(altitude : Float) = (altitude > 5 && altitude <120)

    private fun  updateSpeedInfo(value: Double) {
        missionSpeed = value
        // value je -100 az 100
        if (!distanceOK(missionDistance) || App.getProductInstance() == null) {
            missionControls.speedInfoPrep?.text = "Speed: N/A, Turn time: N/A"
        } else {
            val angleSpeed = missionSpeed * DJIHotPointMission.maxAngularVelocityForRadius(missionDistance.toDouble())  / 100
            val circle = missionDistance * Math.PI * 2 // obvod kruhu
            val normalSpeed = circle / 360 * angleSpeed  // v m/s
            val timeToCircle = Math.abs(360 / angleSpeed)
            missionControls.speedInfoPrep?.text = "Speed:  ${normalSpeed.format(3)} m/s, Turn time:  ${timeToCircle.format(3)} s "
        }
    }

    override fun showPrepareLayout() {
        missionControls.removeAllViews()
        val layout = inflater.inflate(R.layout.hotpoint_mission1, null) as ConstraintLayout
        layout.backToMissions.setOnClickListener { deactivate() }
        layout.nextPage?.setOnClickListener{showPrepareLayout2()}
        missionControls.addView(layout)
    }
    fun showPrepareLayout2() {

        missionControls.removeAllViews()
        hotpoint = PositionsMgr.dronePosition
        val layout2 = inflater.inflate(R.layout.hotpoint_mission2, null) as ConstraintLayout
        layout2.backToPrepare1.setOnClickListener { showPrepareLayout() }
        layout2.hotPointStart?.setOnClickListener(HotPointMissionMgr::onButtonClick)
        layout2.speedBar?.setProgress(50.0)
        missionControls.addView(layout2)
        updateSpeedInfo(50.0)
        layout2.speedBar?.setOnSeekBarChangeListener { _, d ->  updateSpeedInfo(d)}

    }



    override fun showMissionLayout() {
        missionControls.removeAllViews()
        val layout = inflater.inflate(R.layout.follow_me_mission_running, null) as ConstraintLayout
        layout.backToPrepare.setOnClickListener { cancelMission(); showPrepareLayout() }
        missionControls.addView(layout)
        updateAltitude(PositionsMgr.dronePosition?.altitude ?: 0F)
    }
}
