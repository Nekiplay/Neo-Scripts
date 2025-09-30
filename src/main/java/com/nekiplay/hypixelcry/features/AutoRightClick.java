package com.nekiplay.hypixelcry.features;

import com.nekiplay.hypixelcry.HypixelCry;
import com.nekiplay.hypixelcry.annotations.Init;
import com.nekiplay.hypixelcry.config.enums.AutoRightClickBlocks;
import com.nekiplay.hypixelcry.config.enums.AutoRightClickOpenFeatures;
import com.nekiplay.hypixelcry.events.world.BlockUpdateEvent;
import com.nekiplay.hypixelcry.utils.Location;
import com.nekiplay.hypixelcry.utils.RaycastUtils;
import com.nekiplay.hypixelcry.utils.scheduler.Scheduler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import com.nekiplay.hypixelcry.events.SkyblockEvents;

import java.util.*;

import static com.nekiplay.hypixelcry.HypixelCry.mc;
import static com.nekiplay.hypixelcry.utils.PlayerUtils.getEyePosition;
import static com.nekiplay.hypixelcry.utils.PlayerUtils.getLookEndPos;

public class AutoRightClick {
    private static final Map<BlockPos, Integer> openedChests = new LinkedHashMap<>();
    private static int tickCounter = 0;
    private static final int CHEST_COOLDOWN = 20 * 5;
    private static final int MAX_REMOVALS_PER_TICK = 20;

    @Init
    public static void init() {
        Scheduler.INSTANCE.scheduleCyclic(AutoRightClick::onTick, 1);
        BlockUpdateEvent.EVENT.register(AutoRightClick::onBlockUpdate);
        SkyblockEvents.LOCATION_CHANGE.register(AutoRightClick::handleLocationChange);
    }

    private static boolean shouldSkipTick() {
        return mc.world == null ||
                mc.player == null || !HypixelCry.config.macros.autoRightClick.enabled;
    }

    private static void handleLocationChange(Location location) {
        openedChests.clear();
    }

    private static ActionResult onBlockUpdate(BlockUpdateEvent event) {
        List<Block> selectedBlocks = getAllowedBlocks();
        if (openedChests.containsKey(event.getBlockPos()) && selectedBlocks.contains(event.getNew().getBlock())) {
            return ActionResult.FAIL;
        }
        else {
            return ActionResult.PASS;
        }
    }

    private static void onTick() {
        if (shouldSkipTick()) return;
        if (++tickCounter % MAX_REMOVALS_PER_TICK == 0) {
            cleanUpOldChests();
            if (openedChests.isEmpty()) {
                tickCounter = 0;
            }
        }
        if (mc.currentScreen == null) handleChestOpening();
    }

    private static void cleanUpOldChests() {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = openedChests.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            if (entry.getValue() >= CHEST_COOLDOWN) {
                iterator.remove();
            } else {
                entry.setValue(entry.getValue() + 1);
            }
        }
    }

    private static List<Block> getAllowedBlocks() {
        List<Block> selectedBlocks = new ArrayList<>();
        List<AutoRightClickBlocks> blocks = HypixelCry.config.macros.autoRightClick.blocks;
        if (blocks.contains(AutoRightClickBlocks.Chest)) {
            selectedBlocks.add(Blocks.CHEST);
            selectedBlocks.add(Blocks.TRAPPED_CHEST);
        }
        if (blocks.contains(AutoRightClickBlocks.Lever)) {
            selectedBlocks.add(Blocks.LEVER);
        }
        if (blocks.contains(AutoRightClickBlocks.Skull)) {
            selectedBlocks.add(Blocks.PLAYER_HEAD);
            selectedBlocks.add(Blocks.PLAYER_WALL_HEAD);

            selectedBlocks.add(Blocks.WITHER_SKELETON_SKULL);
            selectedBlocks.add(Blocks.WITHER_SKELETON_WALL_SKULL);

            selectedBlocks.add(Blocks.SKELETON_SKULL);
            selectedBlocks.add(Blocks.SKELETON_WALL_SKULL);

            selectedBlocks.add(Blocks.CREEPER_HEAD);
            selectedBlocks.add(Blocks.CREEPER_WALL_HEAD);

            selectedBlocks.add(Blocks.ZOMBIE_HEAD);
            selectedBlocks.add(Blocks.ZOMBIE_WALL_HEAD);
        }
        return selectedBlocks;
    }

    private static void handleChestOpening() {
        boolean ghostHand = HypixelCry.config.macros.autoRightClick.features.contains(AutoRightClickOpenFeatures.GhostHand);

        List<Block> selectedBlocks = getAllowedBlocks();

        if (selectedBlocks.isEmpty()) {
            return;
        }
        if (ghostHand) {
            HitResult mouseOver = RaycastUtils.rayTraceToBlocks(
                    getEyePosition(),
                    getLookEndPos(HypixelCry.config.macros.autoRightClick.range),
                    selectedBlocks
            );
            if (mouseOver instanceof BlockHitResult blockHitResult) {
                tryOpenChest(blockHitResult);
            }
        } else if (isLookingAt(selectedBlocks) && mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            tryOpenChest(blockHitResult);
        }
    }


    private static boolean isLookingAt(List<Block> blocks) {
        boolean allowCheck = mc.crosshairTarget != null &&
                mc.crosshairTarget.getType() == HitResult.Type.BLOCK;
        if (allowCheck && mc.crosshairTarget instanceof BlockHitResult blockHitResult) {
            assert mc.world != null;
            return blocks.contains(mc.world.getBlockState(blockHitResult.getBlockPos()).getBlock());
        }
        return false;
    }

    private static void tryOpenChest(BlockHitResult mouseOver) {
        if (mouseOver == null || mouseOver.getType() != HitResult.Type.BLOCK || openedChests.containsKey(mouseOver.getBlockPos())) {
            return;
        }
        simulateHumanClick(mouseOver);

    }

    private static void simulateHumanClick(BlockHitResult mop) {
        if (mop == null || mop.getBlockPos() == null) return;

        assert mc.interactionManager != null;
        ActionResult success = mc.interactionManager.interactBlock(
                mc.player,
                Hand.MAIN_HAND,
                mop
        );
        assert mc.player != null;
        mc.player.swingHand(Hand.MAIN_HAND);
        BlockPos pos = mop.getBlockPos();
        if (HypixelCry.config.macros.autoRightClick.features.contains(AutoRightClickOpenFeatures.Air)) {
            BlockState state = Blocks.AIR.getDefaultState();

            assert mc.world != null;
            mc.world.setBlockState(pos, state);

            mc.worldRenderer.updateBlock(mc.world, pos, mc.world.getBlockState(pos), state, 0);
            mc.world.updateNeighbors(pos, Blocks.AIR);
        }
        openedChests.put(mop.getBlockPos(), 0);
    }
}