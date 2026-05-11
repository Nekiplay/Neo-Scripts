package com.nekiplay.neoscripts.utils

import net.minecraft.client.player.ClientInput
import net.minecraft.world.entity.player.Input

data class DirectionalInput(
    val forwards: Boolean,
    val backwards: Boolean,
    val left: Boolean,
    val right: Boolean,
) {
    constructor(input: Input) : this(
        input.forward,
        input.backward,
        input.left,
        input.right
    )

    constructor(movementForward: Float, movementSideways: Float) : this(
        forwards = movementForward > 0.0,
        backwards = movementForward < 0.0,
        left = movementSideways > 0.0,
        right = movementSideways < 0.0
    )

    fun getForwardValue(): Int = when {
        forwards -> 1
        backwards -> -1
        else -> 0
    }

    fun getSidewaysValue(): Int = when {
        right -> 1
        left -> -1
        else -> 0
    }

    fun invert(): DirectionalInput = DirectionalInput(
        forwards = backwards,
        backwards = forwards,
        left = right,
        right = left
    )

    companion object {
        @JvmField
        val NONE = DirectionalInput(forwards = false, backwards = false, left = false, right = false)

        @JvmField
        val FORWARDS = DirectionalInput(forwards = true, backwards = false, left = false, right = false)

        @JvmField
        val BACKWARDS = DirectionalInput(forwards = false, backwards = true, left = false, right = false)

        @JvmField
        val LEFT = DirectionalInput(forwards = false, backwards = false, left = true, right = false)

        @JvmField
        val RIGHT = DirectionalInput(forwards = false, backwards = false, left = false, right = true)

        @JvmField
        val FORWARDS_LEFT = DirectionalInput(forwards = true, backwards = false, left = true, right = false)

        @JvmField
        val FORWARDS_RIGHT = DirectionalInput(forwards = true, backwards = false, left = false, right = true)

        @JvmField
        val BACKWARDS_LEFT = DirectionalInput(forwards = false, backwards = true, left = true, right = false)

        @JvmField
        val BACKWARDS_RIGHT = DirectionalInput(forwards = false, backwards = true, left = false, right = true)
    }
}