package com.nekiplay.hypixelcry.config.neupages;

import com.google.gson.annotations.Expose;
import com.nekiplay.hypixelcry.config.enums.AutoRightClickBlocks;
import com.nekiplay.hypixelcry.config.enums.AutoRightClickOpenFeatures;
import io.github.notenoughupdates.moulconfig.annotations.*;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class Macros {
    @Accordion
    @ConfigOption(name = "Auto RightClick", desc = "Auto click to selected blocks")
    @Expose
    public AutoRightClick autoRightClick = new AutoRightClick();

    public static class AutoRightClick {
        @ConfigOption(name = "Enabled", desc = "Enable Auto RightClick?")
        @ConfigEditorBoolean()
        @Expose
        public boolean enabled = false;

        @ConfigOption(name = "Blocks", desc = "Click for blocks")
        @ConfigEditorDraggableList(requireNonEmpty = false)
        @Expose
        public List<AutoRightClickBlocks> blocks = new ArrayList<AutoRightClickBlocks>() {{
            add(AutoRightClickBlocks.Chest);
            add(AutoRightClickBlocks.Lever);
        }};

        @ConfigOption(name = "Features", desc = "Additional features")
        @ConfigEditorDraggableList(requireNonEmpty = false)
        @Expose
        public List<AutoRightClickOpenFeatures> features = new ArrayList<AutoRightClickOpenFeatures>() {{
            add(AutoRightClickOpenFeatures.Air);
            add(AutoRightClickOpenFeatures.GhostHand);
        }};

        @ConfigOption(name = "Range", desc = "GhostHand Raycast range")
        @ConfigEditorSlider(minValue = 2, maxValue = 5.5f, minStep = 0.25f)
        @Expose
        public float range = 4.5f;
    }
}
