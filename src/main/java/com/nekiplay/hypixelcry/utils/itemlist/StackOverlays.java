package com.nekiplay.hypixelcry.utils.itemlist;

import com.mojang.logging.LogUtils;
import com.nekiplay.hypixelcry.utils.NEURepoManager;
import com.nekiplay.hypixelcry.utils.Utils;
import io.github.moulberry.repo.NEURepoFile;
import io.github.moulberry.repo.data.NEUItem;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.nio.file.Files;

public class StackOverlays {
    private static final Logger LOGGER = LogUtils.getLogger();
    /** Data Version for 1.21.5 */
    private static final int DATA_VERSION = 4325;
    private static final String OVERLAY_DIRECTORY = "itemsOverlay/" + DATA_VERSION;

    /**
     * Applies the necessary overlay for the {@code stack} if applicable.
     */
    protected static void applyOverlay(NEUItem neuItem, ItemStack stack) {
        try {
            NEURepoFile file = NEURepoManager.file(OVERLAY_DIRECTORY + "/" + neuItem.getSkyblockItemId() + ".snbt");

            //The returned file is null if it does not exist
            if (file != null) {
                //Read the overlay file and parse an ItemStack from it
                String overlayData = Files.readString(file.getFsPath());
                ItemStack overlayStack = ItemStack.CODEC.parse(Utils.getRegistryWrapperLookup().createSerializationContext(NbtOps.INSTANCE), TagParser.parseCompoundFully(overlayData))
                        .setPartial(ItemStack.EMPTY)
                        .resultOrPartial(error -> logParseError(neuItem, error))
                        .get();

                if (!overlayStack.isEmpty()) {
                    //Apply the component changes from the overlay stack
                    DataComponentPatch changes = overlayStack.getComponentsPatch();
                    stack.applyComponentsAndValidate(changes);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[Skyblocker Stack Overlays] Failed to apply stack overlay! Item: {}", neuItem.getSkyblockItemId(), e);
        }
    }

    private static void logParseError(NEUItem neuItem, String message) {
        LOGGER.error("[Skyblocker Stack Overlays] Failed to parse item \"{}\". Error: {}", neuItem.getSkyblockItemId(), message);
    }
}