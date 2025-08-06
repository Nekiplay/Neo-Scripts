package com.nekiplay.hypixelcry.features;

import com.nekiplay.hypixelcry.HypixelCry;
import com.nekiplay.hypixelcry.annotations.Init;
import com.nekiplay.hypixelcry.events.SkyblockEvents;
import com.nekiplay.hypixelcry.utils.Area;
import com.nekiplay.hypixelcry.utils.Location;

import net.minecraft.text.Text;

import static com.nekiplay.hypixelcry.HypixelCry.mc;

public class Test {

    @Init
    public static void init() {
        SkyblockEvents.LOCATION_CHANGE.register(Test::locationChange);
        SkyblockEvents.AREA_CHANGE.register(Test::areaChange);
    }

    private static void areaChange(Area area) {
        if (mc.player != null && HypixelCry.config.misc.debug.enabled) {
            String areaf = area.toString();
            if (!areaf.isEmpty()) {
                mc.player.sendMessage(Text.of("New area: " + areaf), false);
            }
        }
    }

    private static void locationChange(Location location) {
        if (mc.player != null && HypixelCry.config.misc.debug.enabled) {
            String locationf = location.toString();
            if (!locationf.isEmpty()) {
                mc.player.sendMessage(Text.of("New location: " + locationf), false);
            }
        }
    }
}
