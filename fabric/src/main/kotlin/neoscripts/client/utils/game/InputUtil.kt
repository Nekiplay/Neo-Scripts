package com.nekiplay.neoscripts.client.utils.game

import com.nekiplay.neoscripts.ClientMain.mc
import com.nekiplay.neoscripts.client.events.PlayerTickEvent
import com.nekiplay.neoscripts.client.events.main.Callback
import com.nekiplay.neoscripts.client.events.main.EventBus
import com.nekiplay.neoscripts.common.mixins.minecraft.MouseHandlerAccessor
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW
import java.lang.reflect.Modifier
import kotlin.math.cos
import kotlin.math.sin

object InputUtil {

    private val keyMap = HashMap<Int, String>()
    val stringMap = HashMap<String, Int>()

    private val forwardKey by lazy { Key(mc.options.keyUp) }
    private val backwardKey by lazy { Key(mc.options.keyDown) }
    private val leftKey by lazy { Key(mc.options.keyLeft) }
    private val rightKey by lazy { Key(mc.options.keyRight) }
    private val spaceKey by lazy { Key(mc.options.keyJump) }
    private val sprintKey by lazy { Key(mc.options.keySprint) }

    init {
        for (field in GLFW::class.java.fields) {
            if (Modifier.isStatic(field.modifiers) && (field.name.startsWith("GLFW_KEY_") || field.name.startsWith("GLFW_MOUSE_BUTTON_"))) {
                try {
                    val code = field.getInt(null)
                    var name = field.name.substring(
                        if (field.name.startsWith("GLFW_KEY_")) 9 else 5
                    ).replace("_", " ").replace("BUTTON ", "").lowercase()
                    keyMap[code] = name
                    stringMap[name] = code
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun getStringKey(key : Int) : String {
        if (key < 0) return "N/A"
        val glfw = GLFW.glfwGetKeyName(key, 0)

        val str =
            if (glfw != null) glfw
            else keyMap.getOrDefault(key, "N/A")

        return str
    }

    fun getKey(name : String) : Int {
        return stringMap[name] ?: GLFW.GLFW_KEY_UNKNOWN
    }

    fun getKeyOrNull(name : String) : Int? {
        return stringMap[name]
    }

    fun isPressed(key : Int, handle : Long) : Boolean {
        if (key == GLFW.GLFW_KEY_UNKNOWN) return false
        if (key <= GLFW.GLFW_MOUSE_BUTTON_LAST) return GLFW.glfwGetMouseButton(handle, key) == GLFW.GLFW_TRUE
        return GLFW.glfwGetKey(handle, key) == GLFW.GLFW_TRUE
    }

    fun isPressed(key : Int) : Boolean {
        return isPressed(key, mc.window.handle())
    }

    /**
     * Захват мыши без закрытия активного экрана: только GLFW-режим курсора и внутренний флаг.
     */
    fun grabMouse() : Boolean {
        val handler = mc.mouseHandler ?: return false
        val accessor = handler as? MouseHandlerAccessor ?: return false
        if (accessor.mouseGrabbed) return true

        val window = mc.window.handle()
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)

        accessor.mouseGrabbed = true
        // Пропускаем первый event движения, чтобы не было рывка камеры
        handler.setIgnoreFirstMove()
        return true
    }

    /**
     * Отпускание мыши: возвращает обычный курсор, не открывая экранов.
     */
    fun releaseMouse() : Boolean {
        val handler = mc.mouseHandler ?: return false
        val accessor = handler as? MouseHandlerAccessor ?: return false
        if (!accessor.mouseGrabbed) return true

        val window = mc.window.handle()
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)

        accessor.mouseGrabbed = false
        return true
    }

    fun calculateCorrectedKeys(
        pressedW : Boolean,
        pressedS : Boolean,
        pressedA : Boolean,
        pressedD : Boolean,
        viewYaw : Float,
        targetYaw : Float
    ): MoveInput {
        val forward = (if (pressedW) 1.0 else 0.0) - (if (pressedS) 1.0 else 0.0)
        val strafe = (if (pressedA) 1.0 else 0.0) - (if (pressedD) 1.0 else 0.0)

        if (forward == 0.0 && strafe == 0.0) return MoveInput()

        val diffYaw = Math.toRadians((targetYaw - viewYaw).toDouble())

        val cos = cos(diffYaw)
        val sin = sin(diffYaw)

        val fixedForward = forward * cos - strafe * sin
        val fixedStrafe = strafe * cos + forward * sin

        val threshold = 0.3

        return MoveInput(
            forward = fixedForward > threshold,
            back = fixedForward < -threshold,
            left = fixedStrafe > threshold,
            right = fixedStrafe < -threshold
        )
    }

    fun calculateImpulse(positive : Boolean, negative : Boolean): Float {
        return if (positive == negative) 0.0f
        else {
            if (positive) 1.0f else -1.0f
        }
    }

    fun setForward(value : Boolean, ticks : Int = 0, priority : Int = 0) {
        forwardKey.set(value, ticks, priority)
    }

    fun setBackward(value : Boolean, ticks : Int = 0, priority : Int = 0) {
        backwardKey.set(value, ticks, priority)
    }

    fun setLeft(value : Boolean, ticks : Int = 0, priority : Int = 0) {
        leftKey.set(value, ticks, priority)
    }

    fun setRight(value : Boolean, ticks : Int = 0, priority : Int = 0) {
        rightKey.set(value, ticks, priority)
    }

    fun setSpace(value : Boolean, ticks : Int = 0, priority : Int = 0) {
        spaceKey.set(value, ticks, priority)
    }

    fun setSprint(value : Boolean, ticks : Int = 0, priority : Int = 0) {
        sprintKey.set(value, ticks, priority)
    }

    fun setAllMovement(value : Boolean, ticks : Int = 0, priority : Int = 0) {
        forwardKey.set(value, ticks, priority)
        backwardKey.set(value, ticks, priority)
        leftKey.set(value, ticks, priority)
        rightKey.set(value, ticks, priority)
        spaceKey.set(value, ticks, priority)
        sprintKey.set(value, ticks, priority)
    }

    class Key(val key : KeyMapping) {

        var value = false
            private set
        var priority = -1
            private set
        var ticks = -1
            private set

        private var set = false
        private var lastValue = false

        init {
            EventBus.register(this)
        }

        fun set(value : Boolean, ticks : Int = 0, priority : Int = 0) {
            if (priority >= this.priority) {

                this.lastValue = key.isDown
                this.value = value
                this.ticks = ticks

                this.set = true
                this.priority = priority
            }
        }

        @Callback(-10)
        fun onTick(event : PlayerTickEvent) {

            if (set) {
                key.isDown = value
                mc.player?.input?.tick()
            }

            if (ticks >= 0) {
                when {
                    ticks > 0 -> ticks--
                    set -> set = false
                    else -> {
                        value = false
                        priority = -1
                        ticks = -1
                        set = false
                        key.isDown = isPressed(key.key.value)
                        mc.player?.input?.tick()
                    }
                }
            }
        }
    }

    data class MoveInput(
        var forward : Boolean = false,
        var back : Boolean = false,
        var left : Boolean = false,
        var right : Boolean = false
    )

}