package com.nekiplay.neoscripts.common.mixins.minecraft;

import com.mojang.math.Transformation;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setBillboardConstraints")
    void nsSetBillboardConstraints(Display.BillboardConstraints mode);

    @Invoker("getBillboardConstraints")
    Display.BillboardConstraints nsGetBillboardConstraints();

    @Invoker("setViewRange")
    void nsSetViewRange(float range);

    @Invoker("getViewRange")
    float nsGetViewRange();

    @Invoker("setShadowRadius")
    void nsSetShadowRadius(float radius);

    @Invoker("getShadowRadius")
    float nsGetShadowRadius();

    @Invoker("setShadowStrength")
    void nsSetShadowStrength(float strength);

    @Invoker("getShadowStrength")
    float nsGetShadowStrength();

    @Invoker("setBrightnessOverride")
    void nsSetBrightnessOverride(@Nullable Brightness brightness);

    @Invoker("getPackedBrightnessOverride")
    int nsGetPackedBrightnessOverride();

    @Invoker("setTransformation")
    void nsSetTransformation(Transformation transformation);
}

