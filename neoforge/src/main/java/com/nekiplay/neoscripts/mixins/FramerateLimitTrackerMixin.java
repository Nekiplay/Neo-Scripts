package com.nekiplay.neoscripts.mixins;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FramerateLimitTracker.class)
public class FramerateLimitTrackerMixin {

    @Shadow
    private int framerateLimit;

    @Shadow
    private Minecraft minecraft;

    @Shadow
    private long latestInputTime;

    /**
     * @author nekiplay
     * @reason Полностью отключаем троттлинг FPS при AFK и свёрнутом окне
     */
    @Overwrite
    public int getFramerateLimit() {
        return this.framerateLimit;
    }

    /**
     * @author nekiplay
     * @reason Всегда возвращаем NONE, игнорируя AFK и свёрнутое окно
     */
    @Overwrite
    public FramerateLimitTracker.FramerateThrottleReason getThrottleReason() {
        // Проверяем только OUT_OF_LEVEL_MENU (меню паузы/главное меню)
        if (this.minecraft.level == null || this.minecraft.screen != null) {
            return FramerateLimitTracker.FramerateThrottleReason.OUT_OF_LEVEL_MENU;
        }
        return FramerateLimitTracker.FramerateThrottleReason.NONE;
    }

    /**
     * @author nekiplay
     * @reason Отключаем флаг "тяжёлого" троттлинга
     */
    @Overwrite
    public boolean isHeavilyThrottled() {
        return false;
    }
}