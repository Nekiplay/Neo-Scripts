package com.nekiplay.hypixelcry.events.network;

import com.nekiplay.hypixelcry.events.world.BlockUpdateEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public class PacketEvent {
    public static final Event<PacketEvent.PacketEventReciveCallback> RECEIVE = EventFactory.createArrayBacked(
            PacketEvent.PacketEventReciveCallback.class,
            (listeners) -> (event) -> {
                for (PacketEvent.PacketEventReciveCallback listener : listeners) {
                    ActionResult result = listener.update(event);

                    if(result != ActionResult.PASS) {
                        return result;
                    }
                }

                return ActionResult.PASS;
            }
    );

    public static final Event<PacketEvent.PacketEventSendCallback> SEND = EventFactory.createArrayBacked(
            PacketEvent.PacketEventSendCallback.class,
            (listeners) -> (event) -> {
                for (PacketEvent.PacketEventSendCallback listener : listeners) {
                    ActionResult result = listener.update(event);

                    if(result != ActionResult.PASS) {
                        return result;
                    }
                }

                return ActionResult.PASS;
            }
    );

    public static final Event<PacketEvent.PacketEventSentCallback> SENT = EventFactory.createArrayBacked(
            PacketEvent.PacketEventSentCallback.class,
            (listeners) -> (event) -> {
                for (PacketEvent.PacketEventSentCallback listener : listeners) {
                    ActionResult result = listener.update(event);

                    if(result != ActionResult.PASS) {
                        return result;
                    }
                }

                return ActionResult.PASS;
            }
    );

    private Packet<?> packet;
    private ClientConnection connection;

    public PacketEvent(Packet<?> packet, ClientConnection connection) {
        this.packet = packet;
        this.connection = connection;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public ClientConnection getConnection() {
        return connection;
    }

    public interface PacketEventReciveCallback {
        ActionResult update(PacketEvent event);
    }
    public interface PacketEventSendCallback {
        ActionResult update(PacketEvent event);
    }
    public interface PacketEventSentCallback {
        ActionResult update(PacketEvent event);
    }
}
