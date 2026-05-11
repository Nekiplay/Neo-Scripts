package com.nekiplay.neoscripts.events.main

abstract class DoubleCancellableEvent(pre : Boolean) : DoubleEvent(pre) {
    var cancelled : Boolean = false
}