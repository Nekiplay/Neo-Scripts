package com.nekiplay.neoscripts.mixins.entity;

import com.nekiplay.neoscripts.events.ExtractRenderStateEvent;
import com.nekiplay.neoscripts.events.main.EventBus;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Unique
    private T lastEntity;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("HEAD"))
    public void extractRenderStateHook(T livingEntity, S livingEntityRenderState, float f, CallbackInfo ci) {
        lastEntity = livingEntity;
        EventBus.INSTANCE.send(new ExtractRenderStateEvent(true, livingEntity, livingEntityRenderState, f));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    public void extractRenderStatePostHook(T livingEntity, S livingEntityRenderState, float f, CallbackInfo ci) {
        EventBus.INSTANCE.send(new ExtractRenderStateEvent(false, livingEntity, livingEntityRenderState, f));
    }

}