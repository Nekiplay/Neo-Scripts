package com.nekiplay.neoscripts.mixins.packets;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundMovePlayerPacket.class)
public interface ServerboundMovePlayerPacketAccessor {
    @Mutable
    @Accessor("y")
    void neoscripts$setX(double x);
    @Mutable
    @Accessor("y")
    void neoscripts$setY(double y);
    @Mutable
    @Accessor("y")
    void neoscripts$setZ(double z);
    @Mutable
    @Accessor("onGround")
    void neoscripts$setOnGround(boolean onGround);
    @Mutable
    @Accessor("yRot")
    void neoscripts$setYaw(float yRot);
    @Mutable
    @Accessor("xRot")
    void neoscripts$setPitch(float xRot);
}
