package com.nekiplay.neoscripts.features.modules

import com.nekiplay.neoscripts.Main.mc


open class ClientModule {
    val player by lazy { mc.player }  // Инициализируется при первом обращении
    val world by lazy { mc.level }
    val screen by lazy { mc.screen }
    val interaction by lazy { mc.gameMode }

    open fun init() {

    }

    open fun get_name(): String {
        return "";
    }
}