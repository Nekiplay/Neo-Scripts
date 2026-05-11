package com.nekiplay.neoscripts.mixins.minecraft;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface ClientInputAccessor {
    @Accessor("moveVector")
    Vec2 getMoveVector();   // геттер

    @Accessor("moveVector")
    void setMoveVector(Vec2 moveVector);  // сеттер
}
