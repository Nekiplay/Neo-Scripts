package com.nekiplay.neoscripts.mixins;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("destroyDelay")
    void setDestroyDelay(int delay);

    @Accessor("destroyDelay")
    int getDestroyDelay();

    @Invoker("ensureHasSentCarriedItem")
    void syncSelectedSlot();

    @Invoker("startPrediction")
    void sendSequencedPacket(ClientLevel world, PredictiveAction packetCreator);
}
