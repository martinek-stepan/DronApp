package com.dron.app.vrtulnicek.missions

import android.content.Context
import android.graphics.Color
import android.location.Location
import android.support.constraint.ConstraintLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.dron.app.R
import com.dron.app.vrtulnicek.MapMgr
import com.dron.app.vrtulnicek.MapMgr.map
import com.dron.app.vrtulnicek.MapSwapper
import com.dron.app.vrtulnicek.PositionsMgr
import com.dron.app.vrtulnicek.VrtulnicekActivity
import com.dron.app.vrtulnicek.utils.HTMLText
import com.dron.app.vrtulnicek.utils.toast
import com.google.android.gms.maps.model.*
import com.yoavst.kotlin.`KotlinPackage$Tasks$65a6a183`.mainThread
import dji.common.error.DJIError
import dji.sdk.missionmanager.DJIMission
import dji.sdk.missionmanager.DJIMissionManager
import dji.sdk.missionmanager.DJIWaypoint
import dji.sdk.missionmanager.DJIWaypointMission
import kotlinx.android.synthetic.main.waypoint_action_edit.view.*
import kotlinx.android.synthetic.main.waypoint_edit.view.*
import kotlinx.android.synthetic.main.waypoint_list_row.view.*
import kotlinx.android.synthetic.main.waypoints_mission_in_progress.view.*
import kotlinx.android.synthetic.main.waypoints_mission_prepare.view.*
import kotlinx.android.synthetic.main.waypoints_mission_prepared.view.*
import kotlinx.android.synthetic.main.waypoints_mission_uploading.view.*
import kotlinx.coroutines.experimental.CommonPool

/**
 * Created by Kexik on 23.04.2017.
 */
object WaypointMissionMgr : MissionMgr()
{

    private var state : MissionState = MissionState.NONE
    var mission: DJIWaypointMission? = null

    var waypoints = ArrayList<Waypoint>()
    var polyline: Polyline? = null

    override fun initialize(activity : VrtulnicekActivity)
    {
        super.initialize(activity)
    }

    override fun activate(mgr : DJIMissionManager?)
    {
        super.activate(mgr)
        MapSwapper.swapMapAndVideo(false, true)
        MapMgr.addOnMapLongClickListener(WaypointMissionMgr::onMapClick)
        missionMgr?.setMissionExecutionFinishedCallback(WaypointMissionMgr::onMissionExecutionFinished)
        missionMgr?.setMissionProgressStatusCallback(WaypointMissionMgr::onMissionProgressStatusUpdate)
        setState(MissionState.NONE)
    }

    override fun deactivate()
    {
        super.deactivate()
        MapMgr.removeOnMapLongClickListener(WaypointMissionMgr::onMapClick)
        missionMgr?.setMissionExecutionFinishedCallback(null)
        missionMgr?.missionProgressStatusCallback = null

        polyline?.remove()
        polyline = null
        waypoints.forEach { it.marker.remove() }
        waypoints.clear()
        (missionControls.waypointList?.adapter as WaypointsAdapter?)?.notifyDataSetChanged()
        mission = null

        MissionsMgr.initUI()
    }

    // Listener for mission finnish callback from DJI SDK
    private fun onMissionExecutionFinished(error : DJIError?) {
        mainThread(CommonPool) {
            missionControls.toast("Execution finished: " + if (error == null) "Success!" else error.description)
            setState(MissionState.NONE)
            mission = null
        }
    }

    // Listener for mission update callback from DJI SDK
    private fun onMissionProgressStatusUpdate(progressStatus : DJIMission.DJIMissionProgressStatus)
    {
        if (progressStatus !is DJIWaypointMission.DJIWaypointMissionStatus)
            return

        mainThread(CommonPool) {
            val html = HTMLText("<b>Flight state<b>: " + progressStatus.executionState.name).
                    addLine("<b>Is waypoint reached<b>: " + progressStatus.isWaypointReached).
                    addLine("<b>Target waypoint<b>: " + progressStatus.targetWaypointIndex).
                    addLine("<b>Error<b>: " + if (progressStatus.error == null) "No Errors" else progressStatus.error.description)
            missionControls.missionInfo?.text = html.getSpanned()
        }
    }

    override fun showPrepareLayout()
    {
        missionControls.removeAllViews()
        val layout = inflater.inflate(R.layout.waypoints_mission_prepare, null) as LinearLayout
        layout.backToMissions.setOnClickListener{deactivate()}
        layout.waypointList.adapter = WaypointsAdapter(layout.waypointList.context)
        layout.waypointList.setOnItemClickListener { _, _, position, _ -> showWaypointLayout(position) }
        layout.prepare.setOnClickListener(WaypointMissionMgr::onPrepareClick)
        layout.addWaypoint.setOnClickListener { v ->
            if (PositionsMgr.dronePosition == null)
                v.toast("We need drone position to add waypoint!")
            else
                addWaypoint(PositionsMgr.dronePosition!!.longitude, PositionsMgr.dronePosition!!.latitude, PositionsMgr.dronePosition!!.altitude)
        }
        layout.removeAllWaypoints.setOnClickListener { waypoints.forEach { it.marker.remove() }; waypoints.clear(); (missionControls.waypointList?.adapter as WaypointsAdapter?)?.notifyDataSetChanged() }
        layout.prepare.isEnabled = waypoints.size > 1
        missionControls.addView(layout)
        updateMap()
    }

    fun onPrepareClick(v: View)
    {
        mission = DJIWaypointMission()
        mission!!.needExitMissionOnRCSignalLost = true
        if (PositionsMgr.userPosition != null)
        {
            mission!!.pointOfInterestLatitude = PositionsMgr.userPosition!!.latitude
            mission!!.pointOfInterestLongitude = PositionsMgr.userPosition!!.longitude
        }
        mission!!.autoFlightSpeed = 0F
        mission!!.maxFlightSpeed = 14F
        mission!!.finishedAction = getFinnishedAction()
        mission!!.flightPathMode = getFlightPathMode()
        mission!!.headingMode = getHeadingMode()
        mission!!.repeatNum = 0
        val wp = ArrayList<DJIWaypoint>()
        waypoints.forEach {
            val waypoint = it.dji
            it.actions.forEach { waypoint.addAction(it) }
            wp.add(waypoint)
        }
        mission!!.addWaypoints(wp)

        setState(MissionState.PREPARING)
        missionMgr?.prepareMission(mission, {
            _, pct -> mainThread(CommonPool) {
                missionControls.uploadingProgress?.progress = (pct*100F).toInt()
            }
        },{
            error -> mainThread(CommonPool) {
            if (error != null)
            {
                missionControls.toast("Preparing error: ${error.description}")
                setState(MissionState.NONE)
            }
            else
            {
                missionControls.toast("Preparing OK!")
                setState(MissionState.PREPARED)
            }
        }})
    }

    // Gets proper enum acorrding to spinner selection
    private fun getHeadingMode() : DJIWaypointMission.DJIWaypointMissionHeadingMode
    {
        when(missionControls.headingMode?.selectedItemPosition)
        {
            0 -> return DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto
            1 -> return DJIWaypointMission.DJIWaypointMissionHeadingMode.TowardPointOfInterest
            2 -> return DJIWaypointMission.DJIWaypointMissionHeadingMode.UsingInitialDirection
            3 -> return DJIWaypointMission.DJIWaypointMissionHeadingMode.UsingWaypointHeading
        }

        return DJIWaypointMission.DJIWaypointMissionHeadingMode.Auto
    }

    // Gets proper enum acorrding to spinner selection
    private fun getFlightPathMode() : DJIWaypointMission.DJIWaypointMissionFlightPathMode
    {
        when(missionControls.flightMode?.selectedItemPosition)
        {
            0 -> return DJIWaypointMission.DJIWaypointMissionFlightPathMode.Normal
            1 -> return DJIWaypointMission.DJIWaypointMissionFlightPathMode.Curved
        }

        return DJIWaypointMission.DJIWaypointMissionFlightPathMode.Normal
    }

    // Gets proper enum acorrding to spinner selection
    private fun  getFinnishedAction() : DJIWaypointMission.DJIWaypointMissionFinishedAction
    {
        when(missionControls.finnishAction?.selectedItemPosition)
        {
            0 -> return DJIWaypointMission.DJIWaypointMissionFinishedAction.NoAction
            1 -> return DJIWaypointMission.DJIWaypointMissionFinishedAction.AutoLand
            2 -> return DJIWaypointMission.DJIWaypointMissionFinishedAction.ContinueUntilEnd
            3 -> return DJIWaypointMission.DJIWaypointMissionFinishedAction.GoFirstWaypoint
            4 -> return DJIWaypointMission.DJIWaypointMissionFinishedAction.GoHome
        }

        return DJIWaypointMission.DJIWaypointMissionFinishedAction.NoAction
    }

    // Change layout to show uploading progress
    fun showUploadingLayout()
    {
        missionControls.removeAllViews()
        val layout = inflater.inflate(R.layout.waypoints_mission_uploading, null) as ConstraintLayout
        missionControls.addView(layout)
    }

    override fun showMissionLayout()
    {
        missionControls.removeAllViews()
        val layout = inflater.inflate(R.layout.waypoints_mission_in_progress, null) as ConstraintLayout
        layout.backFromInProgress.setOnClickListener{ cancelMission(); setState(MissionState.NONE)}
        layout.automaticSpeedSB.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar : SeekBar?, progress : Int, fromUser : Boolean) {}

            override fun onStartTrackingTouch(seekBar : SeekBar?) {}

            override fun onStopTrackingTouch(seekBar : SeekBar)
            {
                missionControls.automaticSpeedTV.text = "Automatic speed: ${seekBar.progress} m/s"
                DJIWaypointMission.setAutoFlightSpeed(seekBar.progress.toFloat(), { error -> mainThread(CommonPool) {
                    if (error != null)
                    {
                        missionControls.toast("Failed set speed: ${error.description}")
                    }
                    missionControls.toast("Atomatic speed set successfully!")
                }})
            }
        })
        missionControls.addView(layout)
    }

    // Show layout for editing specific waypoint
    fun showWaypointLayout(pos: Int)
    {
        missionControls.removeAllViews()
        val layout = inflater.inflate(R.layout.waypoint_edit, null) as LinearLayout
        layout.backFromWaypoint.setOnClickListener{ showPrepareLayout() }
        layout.waypointInfo.text = "ID: $pos Lat: ${waypoints[pos].dji.latitude} Long: ${waypoints[pos].dji.longitude}"
        layout.waypointAltTV.text = "Altitude: ${waypoints[pos].dji.altitude}"
        layout.waypointAltitude.progress = waypoints[pos].dji.altitude.toInt()
        layout.waypointAltitude.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar : SeekBar, progress : Int, fromUser : Boolean){
                layout.waypointAltTV.text = "Altitude: ${seekBar.progress.toFloat()}"}
            override fun onStartTrackingTouch(seekBar : SeekBar?){}

            override fun onStopTrackingTouch(seekBar : SeekBar)
            {
                waypoints[pos].dji.altitude = maxOf(10F,seekBar.progress.toFloat())
                layout.waypointAltTV.text = "Altitude: ${waypoints[pos].dji.altitude}"
                if (seekBar.progress < 10)
                    seekBar.progress = 10
            }
        })
        layout.addAction.setOnClickListener {
            waypoints[pos].actions.add(DJIWaypoint.DJIWaypointAction(DJIWaypoint.DJIWaypointActionType.GimbalPitch, 0))
            showActionLayout(pos, waypoints[pos].actions.size-1)
        }
        layout.removeWaypoint.setOnClickListener {
            removeWaypoint(pos)
            showPrepareLayout()
        }

        layout.actionsList?.adapter = WaypointsAdapter(layout.context, pos, true)
        layout.actionsList?.setOnItemClickListener { parent, view, position, id -> showActionLayout(pos,position) }
        missionControls.addView(layout)
    }

    // Helper for removing waypoint
    private fun removeWaypoint(pos: Int)
    {
        waypoints[pos].marker.remove()
        waypoints.removeAt(pos)
        waypoints.forEachIndexed { index, waypoint -> waypoint.marker.title = index.toString() }
    }

    // Show layout for editing specific action
    private fun showActionLayout(pos : Int, actionPos: Int)
    {
        missionControls.removeAllViews()
        val layout = inflater.inflate(R.layout.waypoint_action_edit, null) as LinearLayout
        layout.backFromAction.setOnClickListener{ showWaypointLayout(pos) }
        layout.actionType.setSelection(waypoints[pos].actions[actionPos].mActionType.ordinal)
        updateActionValueBoundaries(layout.actionValue, waypoints[pos].actions[actionPos])
        layout.actionValue.progress = getProgressValueForActionType(waypoints[pos].actions[actionPos].mActionType,waypoints[pos].actions[actionPos].mActionParam)
        layout.actionInfo.text = "Pos: $actionPos ${getInfoForActionType(waypoints[pos].actions[actionPos].mActionType)}"
        layout.actionValueTV.text = "${waypoints[pos].actions[actionPos].mActionParam}"
        layout.actionValue.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar : SeekBar?, progress : Int, fromUser : Boolean){
                waypoints[pos].actions[actionPos].mActionParam = getValueForActionType(waypoints[pos].actions[actionPos].mActionType)
                missionControls.actionValueTV.text = ""+getValueForActionType(waypoints[pos].actions[actionPos].mActionType)
            }
            override fun onStartTrackingTouch(seekBar : SeekBar?){}

            override fun onStopTrackingTouch(seekBar : SeekBar)
            {
            }
        })
        layout.actionType.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                waypoints[pos].actions[actionPos].mActionType = DJIWaypoint.DJIWaypointActionType.values()[position]
                layout.actionInfo.text = getInfoForActionType(waypoints[pos].actions[actionPos].mActionType)
                updateActionValueBoundaries(missionControls.actionValue,waypoints[pos].actions[actionPos])
            }
        }
        layout.removeAction.setOnClickListener {
            waypoints[pos].actions.removeAt(actionPos)
            showWaypointLayout(pos)
        }
        missionControls.addView(layout)
    }

    // Update action progressbar boundaries
    private fun updateActionValueBoundaries(actionValue: SeekBar?,action: DJIWaypoint.DJIWaypointAction) {
        if (actionValue == null)
            return

        val progress = actionValue

        when(action.mActionType)
        {
            DJIWaypoint.DJIWaypointActionType.StopRecord,DJIWaypoint.DJIWaypointActionType.StartRecord,DJIWaypoint.DJIWaypointActionType.StartTakePhoto -> {
                missionControls.actionValue?.visibility = View.GONE
                missionControls.actionValueTV?.visibility = View.GONE
                return
            }
            else -> {
                missionControls.actionValue?.visibility = View.VISIBLE
                missionControls.actionValueTV?.visibility = View.VISIBLE
            }
        }

        when(action.mActionType)
        {
            DJIWaypoint.DJIWaypointActionType.Stay -> progress.max = 32767
            DJIWaypoint.DJIWaypointActionType.RotateAircraft -> progress.max = 360
            DJIWaypoint.DJIWaypointActionType.GimbalPitch -> progress.max = 90
            else -> {}
        }
    }

    // Retrieve (and recalculate) value from action progressbar according to action type
    private fun getValueForActionType(type: DJIWaypoint.DJIWaypointActionType): Int {
        when(type) {
            DJIWaypoint.DJIWaypointActionType.Stay -> return missionControls.actionValue?.progress ?: 0
            DJIWaypoint.DJIWaypointActionType.StopRecord, DJIWaypoint.DJIWaypointActionType.StartRecord, DJIWaypoint.DJIWaypointActionType.StartTakePhoto -> return 0

            DJIWaypoint.DJIWaypointActionType.RotateAircraft -> return (missionControls.actionValue?.progress ?: 180) - 180
            DJIWaypoint.DJIWaypointActionType.GimbalPitch -> return (missionControls.actionValue?.progress ?: 90) - 90
        }
    }

    // Calculate value to be set on progress bar (since progress bar support only unsigned numbers) according to action type
    private fun getProgressValueForActionType(type: DJIWaypoint.DJIWaypointActionType, value: Int): Int {
        when(type) {
            DJIWaypoint.DJIWaypointActionType.Stay -> return value
            DJIWaypoint.DJIWaypointActionType.RotateAircraft -> return value + 180
            DJIWaypoint.DJIWaypointActionType.GimbalPitch -> return value + 90
            else -> return 0
        }
    }

    // Get action type tooltip
    private fun getInfoForActionType(type: DJIWaypoint.DJIWaypointActionType): String {
        when(type)
        {
            DJIWaypoint.DJIWaypointActionType.Stay -> return "Drone will stay on place for specified number of miliseconds (0-32767)"
            DJIWaypoint.DJIWaypointActionType.StartTakePhoto -> return "Drone will take photo"
            DJIWaypoint.DJIWaypointActionType.StartRecord -> return "Drone will start recording video"
            DJIWaypoint.DJIWaypointActionType.StopRecord -> return "Drone will stop recording video"
            DJIWaypoint.DJIWaypointActionType.RotateAircraft -> return "Drone will rotate according to specified angle (-180,180) degrees"
            DJIWaypoint.DJIWaypointActionType.GimbalPitch -> return "Drone will ajdust pitch of gimball according to specified angle (-90,0) degrees"
        }
    }

    private fun showPreparedLayout()
    {
        missionControls.removeAllViews()
        val layout = inflater.inflate(R.layout.waypoints_mission_prepared, null) as ConstraintLayout
        layout.backFromPrepared.setOnClickListener{ cancelMission(); setState(MissionState.NONE)}
        layout.startMission?.isEnabled = true
        layout.startMission.setOnClickListener{ setState(MissionState.STARTING); missionMgr?.startMissionExecution{
            error -> mainThread(CommonPool) {
            if (error != null)
            {
                missionControls.toast("Starting failed: ${error.description}")
                setState(MissionState.NONE)
            }
            else
            {
                missionControls.toast("Starting OK!")
                setState(MissionState.IN_PROGRESS)
            }
        }}}
        missionControls.addView(layout)
    }

    override fun setState(state : MissionState)
    {
        this.state = state
        when (state)
        {
            MissionState.NONE ->
            {
                showPrepareLayout()
            }
            MissionState.PREPARING ->
            {
                showUploadingLayout()
            }
            MissionState.STARTING ->
            {
                missionControls.startMission?.isEnabled = false
            }
            MissionState.IN_PROGRESS ->
            {
                showMissionLayout()
            }
            MissionState.PREPARED -> showPreparedLayout()
        }
    }

    // Check and add new waypoint
    fun addWaypoint(longtitude: Double, lastitude: Double, altitude: Float)
    {
        if (altitude < 10F)
        {
            missionControls.toast("As security precaution altitude have to be at least 10 m for waypoint!")
            return
        }
        if (waypoints.size == DJIWaypointMission.DJI_WAYPOINT_MISSION_MAXIMUM_WAYPOINT_COUNT)
        {
            missionControls.toast("You reached maximum number of waypoints! (${DJIWaypointMission.DJI_WAYPOINT_MISSION_MAXIMUM_WAYPOINT_COUNT}")
            return
        }
        if (MapMgr.map == null)
        {
            missionControls.toast("Error map is missing!")
            return
        }
        if (!waypoints.isEmpty())
        {
            val arr = FloatArray(1)
            Location.distanceBetween(waypoints.last().dji.latitude, waypoints.last().dji.longitude,lastitude,longtitude, arr)
            if (arr[0] < 5F || arr[0] > 500F)
            {
                missionControls.toast("Error: distance between waypoints must be between 5m and 500m!")
                return
            }
        }

        val markerOptions = MarkerOptions()
        markerOptions.position(LatLng(lastitude,longtitude))
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        markerOptions.title("${waypoints.size}")

        waypoints.add(Waypoint(DJIWaypoint(lastitude,longtitude,altitude),MapMgr.map!!.addMarker(markerOptions)))
        showWaypointLayout(waypoints.size-1)
        updateMap()

    }

    // Update map polylines and markers
    private fun updateMap()
    {
        missionControls.prepare?.isEnabled = !waypoints.isEmpty()
        (missionControls.waypointList?.adapter as WaypointsAdapter?)?.notifyDataSetChanged()
        polyline?.remove()
        polyline = null

        if ( map == null || waypoints.size < 2)
        {
            return
        }

        val options = PolylineOptions()

        options.color( Color.parseColor( "#CC0000F0" ) )
        options.width( 5F )
        options.visible( true )

        waypoints.forEach { options.add( it.marker.position ) }

        polyline = MapMgr.map!!.addPolyline(options)

    }

    // OnMapClick listener for adding waypoints by long click on map
    fun onMapClick(point : LatLng)
    {
        if (PositionsMgr.dronePosition == null)
        {
            missionControls.toast("Drone is not flying using default altitude 30 meters")
        }

        addWaypoint(point.longitude, point.latitude, PositionsMgr.dronePosition?.altitude ?: 30F)
    }


    // Adapter for waypoint ListView
    private class WaypointsAdapter(context: Context,var pos: Int = 0,var action:Boolean = false) : BaseAdapter() {
        private val mInflator: LayoutInflater = LayoutInflater.from(context)

        override fun getCount() = if (!action) WaypointMissionMgr.waypoints.size else WaypointMissionMgr.waypoints[pos].actions.size


        override fun getItem(position: Int) = if (!action) WaypointMissionMgr.waypoints[position] else WaypointMissionMgr.waypoints[pos].actions[position]

        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
            val view: View?
            val vh: WaypointHolder
            if (convertView == null) {
                view = this.mInflator.inflate(R.layout.waypoint_list_row, parent, false)
                vh = WaypointHolder(view)
                view.tag = vh
            } else {
                view = convertView
                vh = view.tag as WaypointHolder
            }

            vh.pos.text = "$position:"
            val wayPos = if (action) pos else position
            val waypoint = WaypointMissionMgr.waypoints[wayPos]
            if (!action)
                vh.info.text = "${waypoint.dji.latitude}, ${waypoint.dji.longitude}, ${waypoint.dji.altitude}"
            else
                vh.info.text = "${waypoint.actions[position].mActionType} ${waypoint.actions[position].mActionParam}"
            return view
        }

        private class WaypointHolder(row: View) {
            val pos: TextView = row.position
            val info: TextView = row.info

        }
    }

    // Waypoint holder class
    class Waypoint(var dji: DJIWaypoint, var marker: Marker, var actions: ArrayList<DJIWaypoint.DJIWaypointAction> = ArrayList())
}


