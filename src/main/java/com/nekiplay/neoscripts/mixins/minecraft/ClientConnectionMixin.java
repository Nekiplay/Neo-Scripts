package com.nekiplay.neoscripts.mixins.minecraft;

import com.nekiplay.neoscripts.events.network.PacketEvent;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ClientConnectionMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.BEFORE), cancellable = true)
    private void onHandlePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        InteractionResult result = PacketEvent.RECEIVE.invoker().update(new PacketEvent(packet, (Connection) (Object) this));
        if (result == InteractionResult.FAIL) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", cancellable = true)
    private void onSendPacketHead(Packet<?> packet, ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        InteractionResult result = PacketEvent.SEND.invoker().update(new PacketEvent(packet, (Connection) (Object) this));
        if (result == InteractionResult.FAIL) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("TAIL"))
    private void onSendPacketTail(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, CallbackInfo ci) {
        PacketEvent.SENT.invoker().update(new PacketEvent(packet, (Connection) (Object) this));
    }
}
