package com.nekiplay.hypixelcry.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.nekiplay.hypixelcry.HypixelCry;
import com.nekiplay.hypixelcry.annotations.Init;
import com.nekiplay.hypixelcry.features.esp.pathfinder.PathFinderWorker;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class PathFinderCommand {
    private static final String CUSTOM_PATH_ID = "Custom";
    private static final float[] DEFAULT_COLOR = new float[]{1, 0, 0, 0.5f}; // Red color, 0.5 alpha

    @Init
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register(PathFinderCommand::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher,
                                         CommandRegistryAccess commandRegistryAccess) {
        dispatcher.register(
                ClientCommandManager.literal("path")
                        .then(ClientCommandManager.literal("set")
                                .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                                        .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                                                .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                                                        .executes(context -> {
                                                            BlockPos targetPos = getBlockPosFromContext(context);
                                                            PathFinderWorker.addOrUpdatePath(CUSTOM_PATH_ID, targetPos, DEFAULT_COLOR, CUSTOM_PATH_ID, true, true);
                                                            sendFeedback(context, HypixelCry.PREFIX + "§aSet path to position: " + targetPos.toShortString());
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                        .then(ClientCommandManager.literal("clear")
                                .executes(context -> {
                                    if (PathFinderWorker.hasPath(CUSTOM_PATH_ID)) {
                                        PathFinderWorker.removePath(CUSTOM_PATH_ID);
                                        sendFeedback(context, HypixelCry.PREFIX + "§aCleared custom path");
                                    } else {
                                        sendFeedback(context, HypixelCry.PREFIX + "§cNo custom path to clear");
                                    }
                                    return 1;
                                })
                        )
        );
    }

    private static BlockPos getBlockPosFromContext(CommandContext<FabricClientCommandSource> context) {
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        return new BlockPos(x, y, z);
    }

    private static void sendFeedback(CommandContext<FabricClientCommandSource> context, String message) {
        context.getSource().sendFeedback(Text.literal(message));
    }
}