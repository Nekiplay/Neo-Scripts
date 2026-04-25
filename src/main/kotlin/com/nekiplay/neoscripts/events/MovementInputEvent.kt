package com.nekiplay.neoscripts.events

import com.nekiplay.neoscripts.events.main.Event
import com.nekiplay.neoscripts.utils.DirectionalInput

class MovementInputEvent(
    override var directionalInput: DirectionalInput,
    val jump: Boolean,
    val shift: Boolean
) : Event()
