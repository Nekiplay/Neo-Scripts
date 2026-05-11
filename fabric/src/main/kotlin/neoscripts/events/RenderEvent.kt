package com.nekiplay.neoscripts.events

import com.nekiplay.neoscripts.events.main.Event
import org.joml.Matrix4fStack

class RenderEvent(val matrix : Matrix4fStack, val mouseX : Double, val mouseY : Double) : Event()