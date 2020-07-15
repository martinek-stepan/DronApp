package com.dron.app.vrtulnicek.utils

/**
 * Created by smartinek on 20.4.2017.
 */
// Currently unused (used in ActiveTrack Mission)
data class Point(var x: Float,var y: Float)
{
    fun distance(point: Point): Float = Math.abs(x - point.x) + Math.abs(y - point.y)
}