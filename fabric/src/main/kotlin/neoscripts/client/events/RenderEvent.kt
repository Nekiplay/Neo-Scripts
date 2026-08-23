package com.nekiplay.neoscripts.client.events

import com.nekiplay.neoscripts.client.events.main.Event
import org.joml.Matrix4fStack

class RenderEvent(val matrix : Matrix4fStack, val mouseX : Double, val mouseY : Double) : Event()