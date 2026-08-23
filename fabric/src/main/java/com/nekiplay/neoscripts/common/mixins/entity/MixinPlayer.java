package com.nekiplay.neoscripts.common.mixins.entity;

import com.nekiplay.neoscripts.client.events.TravelEvent;
import com.nekiplay.neoscripts.client.events.main.EventBus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.nekiplay.neoscripts.ClientMain.mc;

@Mixin(Player.class)
public abstract class MixinPlayer  {
    @Unique
    private final Player me = (Player) ((Object) this);


    @Inject(method = "travel", at = @At("HEAD"))
    private void travelHook(Vec3 vec3, CallbackInfo ci) {
        if (me == mc.player) EventBus.INSTANCE.send(new TravelEvent(true));
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void travelPostHook(Vec3 vec3, CallbackInfo ci) {
        if (me == mc.player) EventBus.INSTANCE.send(new TravelEvent(false));
    }

}
