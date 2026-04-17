package com.nekiplay.neoscripts.utils;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;

import static com.nekiplay.neoscripts.Main.mc;

public class BlockUtils {
    public static boolean breaking;
    private static boolean breakingThisTick;


    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(BlockUtils::onTickPre);
        ClientTickEvents.END_CLIENT_TICK.register(BlockUtils::onTickPost);
    }
    private static void onTickPre(Minecraft minecraft) {
        breakingThisTick = false;
    }
    private static void onTickPost(Minecraft minecraft) {
        if (!breakingThisTick && breaking) {
            breaking = false;
            if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        }
    }

    /**
     * Needs to be used in {@link TickEvent.Pre}
     */
    public static boolean breakBlock(BlockPos blockPos, Direction direction, boolean swing) {
        if (mc.level.getBlockState(blockPos).isAir()) return false;

        // Creating new instance of block pos because minecraft assigns the parameter to a field, and we don't want it to change when it has been stored in a field somewhere
        BlockPos pos = blockPos instanceof BlockPos.MutableBlockPos ? new BlockPos(blockPos) : blockPos;

        if (mc.gameMode.isDestroying())
            mc.gameMode.continueDestroyBlock(pos, direction);
        else mc.gameMode.startDestroyBlock(pos, direction);

        if (swing) mc.player.swing(InteractionHand.MAIN_HAND);
        else mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

        breaking = true;
        breakingThisTick = true;

        return true;
    }
}
