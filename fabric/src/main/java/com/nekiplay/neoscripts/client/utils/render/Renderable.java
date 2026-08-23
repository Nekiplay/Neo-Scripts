package com.nekiplay.neoscripts.client.utils.render;

import com.nekiplay.neoscripts.client.utils.render.primitive.PrimitiveCollector;

public interface Renderable {
    void extractRendering(PrimitiveCollector collector);
}