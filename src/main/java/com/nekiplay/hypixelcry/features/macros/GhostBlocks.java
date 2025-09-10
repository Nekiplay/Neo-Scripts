package com.nekiplay.hypixelcry.features.macros;

import com.nekiplay.hypixelcry.HypixelCry;
import com.nekiplay.hypixelcry.annotations.Init;
import com.nekiplay.hypixelcry.events.KeyEvent;
import com.nekiplay.hypixelcry.events.MouseButtonEvent;
import com.nekiplay.hypixelcry.mixins.MinecraftClientAccessor;
import com.nekiplay.hypixelcry.utils.misc.input.KeyAction;
import net.minecraft.block.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import static com.nekiplay.hypixelcry.HypixelCry.mc;

public class GhostBlocks {
    @Init
    public static void init() {
        KeyEvent.EVENT.register(GhostBlocks::keyEvent);
        MouseButtonEvent.EVENT.register(GhostBlocks::mouseEvent);
    }

    private static ActionResult mouseEvent(MouseButtonEvent mouseButtonEvent) {
        if (mouseButtonEvent.getAction() == KeyAction.Press && mouseButtonEvent.getButton() ==  HypixelCry.config.macros.ghostBlocks.ghostBlocksKeyBind) {
            handleGhostBlocks();
        }
        return ActionResult.PASS;
    }

    private static ActionResult keyEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyAction.Press && keyEvent.getKey() == HypixelCry.config.macros.ghostBlocks.ghostBlocksKeyBind) {
            handleGhostBlocks();
        }
        return ActionResult.PASS;
    }

    private static void handleGhostBlocks() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;

        HitResult mouseOver = mc.player.raycast(HypixelCry.config.macros.ghostBlocks.range, 1, false);
        if (mouseOver instanceof BlockHitResult blockHitResult) {
            BlockPos pos = blockHitResult.getBlockPos();
            if (pos != null) {
                Block block = mc.world.getBlockState(pos).getBlock();

                if (block instanceof ChestBlock || block instanceof SkullBlock || block instanceof LeverBlock) {
                    ((MinecraftClientAccessor)mc).doItemUse();
                } else {
                    BlockState state = Blocks.AIR.getDefaultState();
                    mc.world.setBlockState(pos, state);

                    mc.worldRenderer.updateBlock(mc.world, pos, mc.world.getBlockState(pos), state, 0);
                    mc.world.updateNeighbors(pos, Blocks.AIR);

                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }
}
