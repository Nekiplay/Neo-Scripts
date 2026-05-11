package com.nekiplay.neoscripts.utils

import com.nekiplay.neoscripts.Main.mc
import kotlin.math.pow
import kotlin.math.roundToInt

object MathUtil {

    fun wrapDegrees(degrees : Float) : Float {
        var i = degrees % 360
        if (i >= 180F) i -= 360F
        if (i < -180F) i += 360F
        return i
    }

    fun getGCD() = ((mc.options.sensitivity().get() * 0.6 + 0.2).pow(3.0)) * 1.2

    fun applyGCD(current: Float, previous: Float): Float {
        val gcd = getGCD()

        val delta = current - previous
        val mousePixels = (delta / gcd).roundToInt()

        return (previous + mousePixels * gcd).toFloat()
    }

    fun limitAngle(current : Float, intended : Float, factor : Float) : Float {
        var change = wrapDegrees(intended - current)
        if (change > factor) change = factor
        if (change < -factor) change = -factor
        return current + change
    }

}