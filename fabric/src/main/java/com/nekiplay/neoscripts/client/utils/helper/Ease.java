package com.nekiplay.neoscripts.client.utils.helper;

import java.util.function.Function;

public enum Ease {
    //        EASE_OUT_BACK(x -> 1 + (1 + 2 * (x - 1)) * (x - 1) * (x - 1)),
    EASE_OUT_SINE(x -> (float) Math.sin((x * Math.PI) / 2)),
    EASE_IN_OUT_SINE(x -> (float) (-(Math.cos(x * Math.PI) - 1) / 2)),
    EASE_OUT_QUAD(x -> 1 - (1 - x) * (1 - x)),
    EASE_OUT_CUBIC(x -> 1 - (1 - x) * (1 - x) * (1 - x)),
    EASE_OUT_CIRC(x -> (float) Math.sqrt(1 - (x - 1) * (x - 1)));
//        EASE_OUT_MIN_JERK(x -> (float) (6 * Math.pow(x, 5) - 15 * Math.pow(x, 4) + 10 * Math.pow(x, 3)));

    private final Function<Float, Float> easingFunction;

    Ease(Function<Float, Float> easingFunction) {
        this.easingFunction = easingFunction;
    }

    public float invoke(float x) {
        return easingFunction.apply(x);
    }
}