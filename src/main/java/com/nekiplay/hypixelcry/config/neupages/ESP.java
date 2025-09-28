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
