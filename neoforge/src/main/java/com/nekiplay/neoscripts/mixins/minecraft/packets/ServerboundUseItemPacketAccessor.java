package com.nekiplay.neoscripts.mixins.minecraft.packets;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundUseItemPacket.class)
public interface ServerboundUseItemPacketAccessor {
    @Accessor("yRot")
    float getyRot();

    @Accessor("yRot")
    void setyRot(float yRot);

    @Accessor("xRot")
    float getxRot();

    @Accessor("xRot")
    void setxRot(float xRot);
}
