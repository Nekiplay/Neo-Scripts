package com.nekiplay.neoscripts.common.mixins;

import com.mojang.authlib.SignatureState;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Позволяет использовать неподписанные текстуры игроков (кастомные головы из /summon
 * моделей) если их URL указывает на официальный домен textures.minecraft.net.
 * Реально подписанные свойства проходят штатную проверку.
 */
@Mixin(YggdrasilMinecraftSessionService.class)
public abstract class YggdrasilSessionServiceMixin {

    @Inject(method = "getPropertySignatureState", at = @At("HEAD"), cancellable = true)
    private void neoscripts$trustUnsignedMinecraftTextures(Property property, CallbackInfoReturnable<SignatureState> cir) {
        if (property != null && !property.hasSignature()) {
            String value = property.value();
            if (value != null && value.contains("textures.minecraft.net")) {
                cir.setReturnValue(SignatureState.SIGNED);
            }
        }
    }
}
