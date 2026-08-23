package com.nekiplay.neoscripts.common.mixins.minecraft;

import com.nekiplay.neoscripts.client.events.PacketEvent;
import com.nekiplay.neoscripts.client.events.main.EventBus;
import com.nekiplay.neoscripts.client.utils.ServerUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.nekiplay.neoscripts.ClientMain.mc;

@Mixin(Connection.class)
public abstract class ClientConnectionMixin {

    @Shadow
    public abstract void send(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, boolean bl);

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void onSend(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        PacketEvent.Send event = new PacketEvent.Send(packet, false);
        if (ServerUtil.INSTANCE.getSilentPackets().contains(packet)) {
            ServerUtil.INSTANCE.getSilentPackets().remove(packet);
        } else if (EventBus.INSTANCE.sendCancellable(event)) {
            ci.cancel();
        } else if (event.getModified()) {
            ci.cancel();
            this.send(event.getPacket(), listener, true);
        }

    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onChannelRead(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ClientboundSetEntityDataPacket entityDataPacket) {
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(entityDataPacket.id());
            if (entity instanceof FakePlayer) {
                ci.cancel();
                return;
            }
        }

        if (EventBus.INSTANCE.sendCancellable(new PacketEvent.Receive(packet, false))) ci.cancel();
    }
}
