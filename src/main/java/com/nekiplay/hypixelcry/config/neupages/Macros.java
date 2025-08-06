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

    @Accordion
    @ConfigOption(name = "Ghost Blocks", desc = "Set looking blocks to air")
    @Expose
    public Ghost_Blocks ghostBlocks = new Ghost_Blocks();

    public static class Ghost_Blocks {
        @ConfigOption(name = "Key binding", desc = "Activation key bind")
        @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
        @Expose
        public int ghostBlocksKeyBind = -1;

        @ConfigOption(name = "Range", desc = "Raycast range")
        @ConfigEditorSlider(minValue = 2, maxValue = 32, minStep = 1)
        @Expose
        public int range = 8;
    }

    @Category(name = "Dungeons", desc = "Macros in dungeons")
    @Expose
    public Dungeons dungeons = new Dungeons();

    public static class Dungeons {
        @Accordion
        @ConfigOption(name = "Auto chest close", desc = "Auto close dungeon chests")
        @Expose
        public Auto_Close_Chests autoCloseChests = new Auto_Close_Chests();

        public static class Auto_Close_Chests {
            @ConfigOption(name = "Auto close chests", desc = "Auto close dungeon chests?")
            @ConfigEditorBoolean()
            @Expose
            public boolean enable = true;
        }
    }

    @Category(name = "Items", desc = "")
    @Expose
    public Items items = new Items();

    public static class Items {
        @Accordion
        @ConfigOption(name = "Aspect Of The Teleports", desc = "")
        @Expose
        public Aspect_Of_The_Teleports aspectOfTheTeleports = new Aspect_Of_The_Teleports();

        public static class Aspect_Of_The_Teleports {
            @ConfigOption(name = "Key binding", desc = "Activation key bind")
            @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
            @Expose
            public int keybind = -1;

            @ConfigOption(name = "Delay", desc = "Repeat delay")
            @ConfigEditorSlider(minValue = 0, maxValue = 10, minStep = 1)
            @Expose
            public int delay = 2;
        }

        @Accordion
        @ConfigOption(name = "Healing Wands", desc = "")
        @Expose
        public Healing_Wands healingWands = new Healing_Wands();

        public static class Healing_Wands {
            @ConfigOption(name = "Key binding", desc = "Activation key bind")
            @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
            @Expose
            public int keybind = -1;
        }

        @Accordion
        @ConfigOption(name = "Wither Cloak", desc = "")
        @Expose
        public Wither_Cloak witherCloak = new Wither_Cloak();

        public static class Wither_Cloak {
            @ConfigOption(name = "Key binding", desc = "Activation key bind")
            @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
            @Expose
            public int keybind = -1;
        }

        @Accordion
        @ConfigOption(name = "Zombie Sword", desc = "")
        @Expose
        public Zombie_Sword zombieSword = new Zombie_Sword();

        public static class Zombie_Sword {
            @ConfigOption(name = "Key binding", desc = "Activation key bind")
            @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
            @Expose
            public int keybind = -1;
        }

        @Accordion
        @ConfigOption(name = "Rogue Sword", desc = "")
        @Expose
        public Rogue_Sword rogueSword = new Rogue_Sword();

        public static class Rogue_Sword {
            @ConfigOption(name = "Key binding", desc = "Activation key bind")
            @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
            @Expose
            public int keybind = -1;
        }
    }
}
