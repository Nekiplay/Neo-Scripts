package com.nekiplay.hypixelcry.mixins;

import com.nekiplay.hypixelcry.events.world.ClientChunkLoadEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkManagerMixin {
    @Inject(
            method = "replaceWithPacketData",
            at = @At("TAIL")
    )
    private void onChunkLoad(int x, int z, FriendlyByteBuf buf, Map<Heightmap.Types, long[]> heightmaps, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer, CallbackInfoReturnable<LevelChunk> cir) {
        ChunkAccess chunk = cir.getReturnValue();
        if (chunk != null) {
            ClientLevel world = Minecraft.getInstance().level;
            if (world != null) {
                ClientChunkLoadEvent.EVENT.invoker().onChunkLoad(world, chunk);
            }
        }
    }
}