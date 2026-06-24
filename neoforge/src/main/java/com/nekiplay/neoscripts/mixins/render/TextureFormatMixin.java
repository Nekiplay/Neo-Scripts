package com.nekiplay.neoscripts.mixins.render;

import com.mojang.blaze3d.textures.TextureFormat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Arrays;

@Mixin(TextureFormat.class)
public class TextureFormatMixin {

    @Shadow
    @Final
    @Mutable
    private static TextureFormat[] $VALUES;

    // Создаем инвокер для вызова конструктора TextureFormat.
    // Сигнатура принимает: String name, int ordinal, int pixelSize
    @Invoker("<init>")
    private static TextureFormat skyblocker$create(String name, int ordinal, int pixelSize) {
        throw new AssertionError();
    }

    // Внедряемся в конец статической инициализации перечисления TextureFormat
    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void skyblocker$addCustomEnum(CallbackInfo ci) {
        ArrayList<TextureFormat> values = new ArrayList<>(Arrays.asList($VALUES));

        // SKYBLOCKER$RGBA32F имеет размер 16 байт (4 канала float по 4 байта каждый)
        TextureFormat rgba32f = skyblocker$create("NEOSCRIPTS_RGBA32F", values.size(), 16);

        values.add(rgba32f);
        $VALUES = values.toArray(new TextureFormat[0]);
    }
}