package com.nekiplay.neoscripts.utils.render;

import com.nekiplay.neoscripts.utils.render.primitive.PrimitiveCollector;

public interface Renderable {
    void extractRendering(PrimitiveCollector collector);
}