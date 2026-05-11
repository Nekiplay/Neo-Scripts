package com.nekiplay.neoscripts.utils.render;

import com.nekiplay.neoscripts.utils.render.primitive.PrimitiveCollector;

public class WorldRenderExtractionCallback extends net.neoforged.bus.api.Event {
    public PrimitiveCollector collector;

    public WorldRenderExtractionCallback(PrimitiveCollector collector) {
        this.collector = collector;
    }
}