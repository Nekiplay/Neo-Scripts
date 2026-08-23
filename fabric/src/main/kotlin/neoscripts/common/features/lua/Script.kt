package com.nekiplay.neoscripts.common.features.lua

import org.luaj.vm2.lib.jse.JsePlatform

open class Script(val scriptName: String, val manager: LuaManager) {
    var scriptGlobals = JsePlatform.standardGlobals()

    open fun cleanup() {
        scriptGlobals = null
    }
}