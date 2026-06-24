package com.nekiplay.neoscripts.utils.render;

import com.nekiplay.neoscripts.utils.render.primitive.PrimitiveCollector;

public class LevelRenderExtractionCallback extends net.neoforged.bus.api.Event {
    public PrimitiveCollector collector;

    public LevelRenderExtractionCallback(PrimitiveCollector collector) {
        this.collector = collector;
    }
}