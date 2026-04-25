package com.nekiplay.neoscripts.events

import com.nekiplay.neoscripts.events.main.CancellableEvent
import net.minecraft.network.protocol.Packet

class PacketEvent {

    class Send(var packet : Packet<*>, var modified : Boolean = false) : CancellableEvent()
    class Receive(var packet : Packet<*>, var modified : Boolean = false) : CancellableEvent()

}