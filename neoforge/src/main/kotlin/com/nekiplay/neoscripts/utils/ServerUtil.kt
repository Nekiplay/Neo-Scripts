package com.nekiplay.neoscripts.utils

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.events.PacketEvent
import com.nekiplay.neoscripts.events.PlayerTickEvent
import com.nekiplay.neoscripts.events.main.Callback
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object ServerUtil {

    var slot = 0
        private set
    var yaw = 0F
        private set
    var pitch = 0F
        private set

    val silentPackets = ArrayList<Packet<*>>()
    private val delayedPackets = ArrayList<DelayedPacket>()

    @Callback
    fun onTick(event : PlayerTickEvent) {
        ArrayList(delayedPackets).forEach {
            it.ticks--
            if (it.ticks <= 0) {
                sendPacket(it.packet)
                delayedPackets.remove(it)
            }
        }
    }

    @Callback(-10)
    fun onPacketSend(event : PacketEvent.Send) {

        if (event.cancelled) return

        if (event.packet is ServerboundSetCarriedItemPacket) {
            if ((event.packet as ServerboundSetCarriedItemPacket).slot == slot) event.cancelled = true
            slot = (event.packet as ServerboundSetCarriedItemPacket).slot
        } else if (event.packet is ServerboundMovePlayerPacket) {
            yaw = (event.packet as ServerboundMovePlayerPacket).getYRot(yaw)
            pitch = (event.packet as ServerboundMovePlayerPacket).getXRot(pitch)
        }

    }

    @Callback(-10)
    fun onPacketReceive(event : PacketEvent.Receive) {

        if (event.cancelled) return

        if (event.packet is ClientboundSetHeldSlotPacket) {
            slot = (event.packet as ClientboundSetHeldSlotPacket).slot
        } else if (event.packet is ClientboundPlayerPositionPacket) {
            val change = (event.packet as ClientboundPlayerPositionPacket).change

            yaw = change.yRot
            pitch = change.xRot
        }

    }

    fun sendPacket(packet : Packet<*>) {
        mc.player?.connection?.connection?.send(packet)
    }

    @Deprecated(message = "Ignores absolutely all callbacks. Use EventBus.setIgnore(value : Boolean, type : Class<Event>, instance : Any?)")
    fun sendPacketSilently(packet : Packet<*>) {
        silentPackets.add(packet)
        mc.player?.connection?.connection?.send(packet)
    }

    fun sendPacket(packet : Packet<*>, delay : Int) {
        if (delay <= 0) sendPacket(packet)
        else delayedPackets.add(DelayedPacket(packet, delay))
    }

    private data class DelayedPacket(val packet : Packet<*>, var ticks : Int)

}