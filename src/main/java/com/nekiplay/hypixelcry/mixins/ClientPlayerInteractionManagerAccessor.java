package com.nekiplay.hypixelcry.mixins;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Invoker("syncSelectedSlot")
    void syncSelectedSlot();

    @Invoker("sendSequencedPacket")
    void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator);
}
