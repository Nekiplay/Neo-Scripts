package com.nekiplay.hypixelcry.config.neupages;

import com.google.gson.annotations.Expose;
import com.nekiplay.hypixelcry.config.enums.ESPFeatures;
import com.nekiplay.hypixelcry.config.enums.PathFinderPriority;
import io.github.notenoughupdates.moulconfig.annotations.*;
import io.github.notenoughupdates.moulconfig.observer.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ESP {

    @Accordion
    @ConfigOption(
            name = "Chest ESP",
            desc = ""
    )
    @Expose
    public Chest_ESP chestEsp = new Chest_ESP();

    public static class Chest_ESP {
        @ConfigOption(
                name = "Enable",
                desc = "Enable Chest ESP?"
        )
        @ConfigEditorBoolean
        @Expose
        public boolean enabled = false;

        @ConfigOption(name = "Range", desc = "Max range, 0 = unlimited")
        @ConfigEditorSlider(minValue = 0, maxValue = 50, minStep = 1)
        @Expose
        public int maxRange = 0;

        @ConfigOption(name = "Color", desc = "ESP color")
        @ConfigEditorColour
        @Expose
        public String colour = "0:255:255:0:13";

        @ConfigOption(name = "Features", desc = "Render features")
        @Expose
        @ConfigEditorDraggableList(requireNonEmpty = false)
        public List<ESPFeatures> features = new ArrayList<>(Collections.singletonList(ESPFeatures.Box));
    }

    @Category(name = "Dwarven Mines", desc = "Render features in dwarven mines")
    @Expose
    public Dwarven_Mines dwarvenMines = new Dwarven_Mines();

    public static class Dwarven_Mines {
        @Accordion
        @ConfigOption(
                name = "Dark Monolith",
                desc = ""
        )
        @Expose
        public Dark_Monolith darkMonolith = new Dark_Monolith();

        public static class Dark_Monolith {
            @ConfigOption(
                    name = "Enable",
                    desc = "Enable Dark Monolith ESP?"
            )
            @ConfigEditorBoolean
            @Expose
            public boolean enabled = false;

            @ConfigOption(name = "Color", desc = "ESP color")
            @ConfigEditorColour
            @Expose
            public String colour = "0:255:131:0:255";

            @ConfigOption(name = "Features", desc = "Render features")
            @Expose
            @ConfigEditorDraggableList(requireNonEmpty = false)
            public List<ESPFeatures> features = new ArrayList<>(Collections.singletonList(ESPFeatures.Box));
        }
    }

    @Category(name = "Glacite Mineshafts", desc = "Render features in glacite mineshafts")
    @Expose
    public Glacite_Mineshafts glaciteMineshafts = new Glacite_Mineshafts();

    public static class Glacite_Mineshafts {
        @Accordion
        @ConfigOption(
                name = "Frozen Courpes ESP",
                desc = ""
        )
        @Expose
        public Frozen_Courpes frozenCourpes = new Frozen_Courpes();

        public static class Frozen_Courpes {
            @ConfigOption(
                    name = "Enable",
                    desc = "Enable Frozen Courpes ESP?"
            )
            @ConfigEditorBoolean
            @Expose
            public boolean enabled = true;

            @ConfigOption(name = "Features", desc = "Render features")
            @Expose
            @ConfigEditorDraggableList(requireNonEmpty = false)
            public List<ESPFeatures> features = new ArrayList<>(Collections.singletonList(ESPFeatures.Box));

            @ConfigOption(
                    name = "Enable PathFinder",
                    desc = "Enable Frozen Courpes PathFinder?"
            )
            @ConfigEditorBoolean
            @Expose
            public Property<Boolean> enabledPathFinder = Property.of(true);
        }
    }

    @Accordion
    @ConfigOption(
            name = "PathFinder",
            desc = ""
    )
    @Expose
    public PathFinderESP pathFinderESP = new PathFinderESP();

    public static class PathFinderESP {
        @ConfigOption(
                name = "Enable",
                desc = "Enable render PathFinder?"
        )
        @ConfigEditorBoolean
        @Expose
        public boolean enabled = true;

        @ConfigOption(
                name = "Enable",
                desc = "Enable render sub points?"
        )
        @ConfigEditorBoolean
        @Expose
        public boolean enableSubPoints = true;
    }
}
