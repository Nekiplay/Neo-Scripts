package com.nekiplay.neoscripts.events.main

abstract class CancellableEvent : Event() {
    var cancelled : Boolean = false
}