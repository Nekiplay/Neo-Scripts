package com.nekiplay.neoscripts.client.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public class SendMovementPacketsEvent {
    public static final Event<Pre> PRE = EventFactory.createArrayBacked(Pre.class,
            (listeners) -> (yaw, pitch) -> {
                for (Pre listener : listeners) {
                    listener.onSendMovementPacketsPre(yaw, pitch);
                }
            }
    );

    public static final Event<Post> POST = EventFactory.createArrayBacked(Post.class,
            (listeners) -> () -> {
                for (Post listener : listeners) {
                    listener.onSendMovementPacketsPost();
                }
            }
    );

    @FunctionalInterface
    public interface Pre {
        void onSendMovementPacketsPre(float yaw, float pitch);
    }

    @FunctionalInterface
    public interface Post {
        void onSendMovementPacketsPost();
    }
}