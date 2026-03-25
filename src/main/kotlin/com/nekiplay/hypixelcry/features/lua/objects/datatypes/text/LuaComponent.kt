package com.nekiplay.hypixelcry.features.lua.objects.datatypes.text

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import com.nekiplay.hypixelcry.sugar.getFormattedString
import com.nekiplay.hypixelcry.sugar.getJsonString
import net.minecraft.network.chat.Component
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaComponent(L: Lua?, val component: Component) : SimpleLuaWrapper(L) {

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "getString" -> JFunction { lInner ->
                lInner.push(component.getString())
                1
            }
            "getFormattedString" -> JFunction { lInner ->
                lInner.push(component.getFormattedString())
                1
            }
            "getJsonString" -> JFunction { lInner ->
                lInner.push(component.getJsonString())
                1
            }
            else -> null
        }
    }

    override fun push() {
        super.push()

        val lua = L ?: return
        if (lua.getMetatable(-1) != 0) {
            lua.push(JFunction { l ->
                l.push(component.getString())
                1
            })
            lua.setField(-2, "__tostring")
            lua.pop(1)
        }
    }

    override fun pushValue(): LuaValue {
        push()
        return L!!.get()
    }
}