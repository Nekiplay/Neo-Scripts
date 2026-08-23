package com.nekiplay.neoscripts.client.events

import com.nekiplay.neoscripts.client.events.main.CancellableEvent
import net.minecraft.network.protocol.Packet

class PacketEvent {

    class Send(var packet : Packet<*>, var modified : Boolean = false) : CancellableEvent()
    class Receive(var packet : Packet<*>, var modified : Boolean = false) : CancellableEvent()

}