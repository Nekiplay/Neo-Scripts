package com.nekiplay.hypixelcry.features.modules

import com.nekiplay.hypixelcry.HypixelCry
import com.nekiplay.hypixelcry.pathfinder.utils.mc

open class ClientModule {
    val player by lazy { mc.player }  // Инициализируется при первом обращении
    val world by lazy { mc.world }
    val screen by lazy { mc.currentScreen }
    val interaction by lazy { mc.interactionManager }
    val config by lazy { HypixelCry.config }

    open fun init() {

    }
}