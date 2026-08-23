package com.nekiplay.neoscripts.client.events.main

abstract class CancellableEvent : Event() {
    var cancelled : Boolean = false
}