package com.nekiplay.hypixelcry.features.lua.objects.datatypes.text

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import com.nekiplay.hypixelcry.sugar.getFormattedString
import com.nekiplay.hypixelcry.sugar.getJsonString
import net.minecraft.network.chat.Component
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaComponent(L: Lua, val component: Component) : SimpleLuaWrapper(L) {

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

    /**
     * Переопределяем push, чтобы добавить поддержку метаметода __tostring,
     * как это было в оригинальном companion object MT.
     */
    override fun push(): LuaValue {
        val luaValue = super.push() // Создает объект, таблицу и вешает __index/__newindex

        // Получаем метатаблицу созданного объекта, чтобы добавить __tostring
        if (L.getMetatable(-1) != 0) {
            L.push(JFunction { l ->
                l.push(component.getString())
                1
            })
            L.setField(-2, "__tostring")
            L.pop(1) // Убираем метатаблицу из стека
        }

        return luaValue
    }
}