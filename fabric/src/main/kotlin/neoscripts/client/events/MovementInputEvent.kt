package com.nekiplay.neoscripts.client.events

import com.nekiplay.neoscripts.client.events.main.Event
import com.nekiplay.neoscripts.client.utils.DirectionalInput

class MovementInputEvent(
    var directionalInput: DirectionalInput,
    val jump: Boolean,
    val shift: Boolean
) : Event()
