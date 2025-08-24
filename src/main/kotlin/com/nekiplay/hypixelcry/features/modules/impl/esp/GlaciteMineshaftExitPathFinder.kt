package com.nekiplay.hypixelcry.features.modules.impl.esp

import com.nekiplay.hypixelcry.events.SkyblockEvents
import com.nekiplay.hypixelcry.events.SkyblockEvents.SkyblockLocationChange
import com.nekiplay.hypixelcry.features.esp.mining.dwarvenmines.DarkMonolithESP
import com.nekiplay.hypixelcry.features.esp.pathfinder.PathFinderRenderer
import com.nekiplay.hypixelcry.features.modules.ClientModule
import com.nekiplay.hypixelcry.utils.Location
import com.nekiplay.hypixelcry.utils.SpecialColor.toSpecialColorFloatArray
import com.nekiplay.hypixelcry.utils.scheduler.Scheduler
import net.minecraft.util.math.BlockPos

object GlaciteMineshaftExitPathFinder: ClientModule() {
    var enterance: BlockPos? = null;
    var tick = 0;

    override fun init() {
        Scheduler.INSTANCE.scheduleCyclic(Runnable {
            if (tick == 1) {
                enterance = player?.blockPos;
                if (config.esp.glaciteMineshafts.frozenCourpes.enabledExitPathFinder.get()) {
                    PathFinderRenderer.addOrUpdatePath(
                        "Exit", enterance,
                        "0:127:0:255:255".toSpecialColorFloatArray(), "Exit"
                    )
                }
                tick = 0
            }
        }, 1)
        SkyblockEvents.LOCATION_CHANGE.register(SkyblockLocationChange { location: Location? ->
            if (location == Location.GLACITE_MINESHAFT) {
                enterance = player?.blockPos;
                tick++;
            }
            else {
                enterance = null;
                tick = 0
                if (PathFinderRenderer.hasPath("Exit")) {
                    PathFinderRenderer.removePath("Exit")
                }
            }
        })
    }

}