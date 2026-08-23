package com.nekiplay.neoscripts.common.features.lua.objects.misc

import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaItemStack
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction

class Items : LuaValue() {
    override fun typename(): String = "items"
    override fun tojstring(): String = "ItemsObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getAll", "getItems", "getItemStacks" -> GetItems()
            "getFromId" -> GetFromId()
            "getFromIdentifier" -> GetFromIdentifier()
            else -> super.get(key)
        }
    }

    class GetFromIdentifier : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isstring()) {
                val optional = BuiltInRegistries.ITEM.get(Identifier.parse(args.arg(1).tojstring()))
                if (optional.isPresent) {
                    return LuaItemStack(
                        optional.get().value().defaultInstance
                    )
                }
            }
            return NIL
        }
    }

    class GetFromId : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isint()) {
                val optional = BuiltInRegistries.ITEM.get(args.arg(1).toint())
                if (optional.isPresent) {
                    return LuaItemStack(
                        optional.get().value().defaultInstance
                    )
                }
            }
            return NIL
        }
    }

    class GetItems : OneArgFunction() {
        override fun call(jsonString: LuaValue): LuaValue {
            val itemsTable = tableOf()
            var index = 1
            for (item in BuiltInRegistries.ITEM) {
                itemsTable.set(index++,
                    LuaItemStack(item.defaultInstance)
                )
            }
            return itemsTable
        }
    }
}