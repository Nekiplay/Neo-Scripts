package com.nekiplay.neoscripts.utils.animation

object AnimationUtil {

    fun lerp(oldValue : Double, newValue : Double, interpolationValue : Double) = (oldValue + (newValue - oldValue) * interpolationValue)
    fun lerp(oldValue : Float, newValue : Float, interpolationValue : Double) = ((oldValue + (newValue - oldValue) * interpolationValue).toFloat())
    fun lerp(oldValue : Float, newValue : Float, interpolationValue : Float) = (oldValue + (newValue - oldValue) * interpolationValue)

}