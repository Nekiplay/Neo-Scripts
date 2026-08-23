package com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text

import com.nekiplay.neoscripts.client.sugar.getFormattedString
import com.nekiplay.neoscripts.client.sugar.getJsonString
import net.minecraft.network.chat.Component
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

class LuaComponent(val component: Component) : LuaUserdata(component) {
    init {
        setmetatable(MT)
    }

    companion object {
        private val MT = LuaTable().apply {
            val idx = LuaTable()
            set("__index", idx)
            set("__tostring", object : OneArgFunction() {
                override fun call(self: LuaValue): LuaValue =
                    valueOf((self as LuaComponent).component.string)
            })

            idx.set("getString", object : OneArgFunction() {
                override fun call(self: LuaValue): LuaValue =
                    valueOf((self as LuaComponent).component.string)
            })

            idx.set("getFormattedString", object : OneArgFunction() {
                override fun call(self: LuaValue): LuaValue =
                    valueOf((self as LuaComponent).component.getFormattedString())
            })

            idx.set("getJsonString", object : OneArgFunction() {
                override fun call(self: LuaValue): LuaValue =
                    valueOf((self as LuaComponent).component.getJsonString())
            })
        }
    }
}