package com.nekiplay.hypixelcry.utils.render;

import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector;

public interface Renderable {
    void extractRendering(PrimitiveCollector collector);
}