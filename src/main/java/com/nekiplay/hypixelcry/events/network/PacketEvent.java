package com.nekiplay.hypixelcry.events.network;

import com.nekiplay.hypixelcry.events.world.BlockUpdateEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionResult;

public class PacketEvent {
    public static final Event<PacketEvent.PacketEventReciveCallback> RECEIVE = EventFactory.createArrayBacked(
            PacketEvent.PacketEventReciveCallback.class,
            (listeners) -> (event) -> {
                for (PacketEvent.PacketEventReciveCallback listener : listeners) {
                    InteractionResult result = listener.update(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    public static final Event<PacketEvent.PacketEventSendCallback> SEND = EventFactory.createArrayBacked(
            PacketEvent.PacketEventSendCallback.class,
            (listeners) -> (event) -> {
                for (PacketEvent.PacketEventSendCallback listener : listeners) {
                    InteractionResult result = listener.update(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    public static final Event<PacketEvent.PacketEventSentCallback> SENT = EventFactory.createArrayBacked(
            PacketEvent.PacketEventSentCallback.class,
            (listeners) -> (event) -> {
                for (PacketEvent.PacketEventSentCallback listener : listeners) {
                    InteractionResult result = listener.update(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    private Packet<?> packet;
    private Connection connection;

    public PacketEvent(Packet<?> packet, Connection connection) {
        this.packet = packet;
        this.connection = connection;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    public Connection getConnection() {
        return connection;
    }

    public interface PacketEventReciveCallback {
        InteractionResult update(PacketEvent event);
    }
    public interface PacketEventSendCallback {
        InteractionResult update(PacketEvent event);
    }
    public interface PacketEventSentCallback {
        InteractionResult update(PacketEvent event);
    }
}
