package com.nekiplay.hypixelcry.mixins.entity;

import com.nekiplay.hypixelcry.utils.Rotations;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.nekiplay.hypixelcry.HypixelCry.mc;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {
    // Rotations
}