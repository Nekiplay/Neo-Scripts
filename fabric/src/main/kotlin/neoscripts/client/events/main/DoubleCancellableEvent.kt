package com.nekiplay.neoscripts.client.events.main

abstract class DoubleCancellableEvent(pre : Boolean) : DoubleEvent(pre) {
    var cancelled : Boolean = false
}