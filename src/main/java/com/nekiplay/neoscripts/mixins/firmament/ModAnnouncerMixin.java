package com.nekiplay.neoscripts.mixins.firmament;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.nekiplay.neoscripts.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Coerce;

@Pseudo
@Mixin(targets = "moe.nea.firmament.features.misc.ModAnnouncer")
public class ModAnnouncerMixin {
    @WrapMethod(method = "onServerJoin", require = 0)
    private void onServerJoin(@Coerce Object event, Operation<Void> original) {
        Main.LOGGER.info("Firmament detected, disabling mod announcer.");
    }
}