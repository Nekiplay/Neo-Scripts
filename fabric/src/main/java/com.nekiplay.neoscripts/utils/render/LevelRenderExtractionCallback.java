package com.nekiplay.neoscripts.utils.render;

import com.nekiplay.neoscripts.utils.render.primitive.PrimitiveCollector;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface LevelRenderExtractionCallback {
    Event<LevelRenderExtractionCallback> EVENT = EventFactory.createArrayBacked(LevelRenderExtractionCallback.class, callbacks -> collector -> {
        for (LevelRenderExtractionCallback callback : callbacks) {
            callback.onExtract(collector);
        }
    });

    void onExtract(PrimitiveCollector collector);
}