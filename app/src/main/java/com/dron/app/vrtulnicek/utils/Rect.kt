package com.dron.app.vrtulnicek.utils

import java.lang.Math.abs

/**
 * Created by smartinek on 20.4.2017.
 */

// Currently unused (used in ActiveTrack Mission)
data class Rect(var p1: Point,var p2: Point) {

    init {
        this.p1 = Point(minOf(p1.x, p2.x), minOf(p1.y,p2.y))
        this.p2 = Point(maxOf(p1.x, p2.x), maxOf(p1.y,p2.y))
    }

    fun left() = p1.x
    fun top() = p1.y
    fun right() = p2.x
    fun bottom() = p2.y
    fun  width() = abs(p2.x - p1.x)
    fun  height() = abs(p2.y - p1.y)
}