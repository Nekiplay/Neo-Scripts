package com.nekiplay.neoscripts.events.main

import com.nekiplay.neoscripts.utils.DirectionalInput

abstract class Event {
    @JvmField
    open var directionalInput: DirectionalInput = TODO("initialize me")
}