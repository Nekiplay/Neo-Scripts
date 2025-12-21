package com.nekiplay.hypixelcry.mixins;

import com.nekiplay.hypixelcry.events.world.CreateParticleEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class LevelMixin {
    @Inject(method = "doAddParticle", at = @At("HEAD"), cancellable = true)
    public void doAddParticle(ParticleOptions particleOptions, boolean bl, boolean bl2, double x, double y, double z, double g, double h, double i, CallbackInfo ci) {
        CreateParticleEvent.EVENT.invoker().create(new CreateParticleEvent(particleOptions.getType(), new Vec3(x, y, z)));
    }
}
