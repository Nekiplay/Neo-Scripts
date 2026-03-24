package com.nekiplay.hypixelcry.features.lua.objects.misc

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import com.nekiplay.hypixelcry.utils.itemlist.ItemRepository
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua

class Creator(val L: Lua) {
    fun register() {
        L.push(JFunction { createBox(it) })
        L.setGlobal("createBox")

        L.push(JFunction { createBox(it) })
        L.setGlobal("createAABB")

        L.push(JFunction { createDirection(it) })
        L.setGlobal("createDirection")

        L.push(JFunction { createStackFromId(it) })
        L.setGlobal("createItemStackFromId")
    }

    private fun createBox(l: Lua): Int {
        if (l.isNumber(1) && l.isNumber(2) && l.isNumber(3) &&
            l.isNumber(4) && l.isNumber(5) && l.isNumber(6)) {

            val box = AABB(
                l.toNumber(1), l.toNumber(2), l.toNumber(3),
                l.toNumber(4), l.toNumber(5), l.toNumber(6)
            )

            // Оборачиваем в ваш LuaBox
            l.push(LuaBox(l, box).push())
        } else {
            l.pushNil()
        }
        return 1
    }

    private fun createDirection(l: Lua): Int {
        if (l.isString(1)) {
            val dirStr = l.toString(1)?.uppercase()
            try {
                if (dirStr != null) {
                    val dir = Direction.valueOf(dirStr)
                    l.push(LuaDirection(l, dir).push())
                    return 1
                }
            } catch (e: Exception) {}
        }
        l.pushNil()
        return 1
    }

    private fun createStackFromId(l: Lua): Int {
        if (!l.isString(1)) {
            l.pushNil()
            return 1
        }

        val idString = l.toString(1) ?: ""
        val stack = ItemRepository.getItemStack(idString)

        if (stack != null) {
            l.push(LuaItemStack(l, stack).push())
        } else {
            l.pushNil()
        }
        return 1
    }
}