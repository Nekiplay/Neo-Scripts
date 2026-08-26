package com.nekiplay.neoscripts.common.mixins;

import com.nekiplay.neoscripts.client.features.lua.objects.misc.DynamicContentResourcePack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Добавляет источник рантайм-пака с текстурами/моделями динамических
 * предметов (Lua content API) в репозиторий паков.
 */
@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    @Shadow
    @Final
    private Set<RepositorySource> sources;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void neoscripts$addDynamicContentSource(RepositorySource[] originalSources, CallbackInfo ci) {
        sources.add(DynamicContentResourcePack.INSTANCE.getRepositorySource());
    }
}
