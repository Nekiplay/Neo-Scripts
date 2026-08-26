package com.nekiplay.neoscripts.common.features.lua.objects.misc

import com.nekiplay.neoscripts.client.sugar.isBlock
import com.nekiplay.neoscripts.client.sugar.toBlock
import com.nekiplay.neoscripts.client.sugar.toContentSettings
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaContentSettings
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaItemStack
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction

/**
 * Библиотека динамической регистрации предметов и блоков.
 * Подключается через require("content") и доступна в общих
 * автостарт-скриптах (neoscripts/autostart).
 *
 * API:
 *  content.createSettings({ name=..., texture=..., maxStackSize=..., ... })
 *  content.registerItem("ns:id" [, settings])          -> LuaItemStack
 *  content.registerBlock("ns:id" [, settings])         -> LuaBlockState
 *  content.registerBlockItem("ns:id", blockState [, settings]) -> LuaItemStack
 *  content.getItemTexture("ns:id")                     -> string | nil
 */
class ContentLib : LuaValue() {
    override fun typename(): String = "content_lib"
    override fun tojstring(): String = "ContentLib"
    override fun isnil(): Boolean = false
    override fun type(): Int = TUSERDATA

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "createSettings", "contentSettings", "itemSettings", "blockSettings" -> CreateContentSettings()
            "registerItem" -> RegisterItem()
            "registerBlock" -> RegisterBlock()
            "registerBlockItem" -> RegisterBlockItem()
            "getItemTexture" -> GetItemTexture()
            else -> super.get(key)
        }
    }

    /**
     * content.createSettings({...}) — создает LuaContentSettings.
     */
    class CreateContentSettings : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs =
            if (args.narg() >= 1 && args.arg(1).istable()) {
                LuaContentSettings.fromTable(args.arg(1))
            } else {
                LuaContentSettings()
            }
    }

    /**
     * content.registerItem("ns:my_item" [, settings])
     */
    class RegisterItem : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            val settings = if (args.narg() >= 2) args.arg(2).toContentSettings() else null
            val item = DynamicContent.registerItem(args.arg1().tojstring(), settings) ?: return NIL
            return LuaItemStack(ItemStack(item))
        }
    }

    /**
     * content.registerBlock("ns:my_block" [, settings])
     */
    class RegisterBlock : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            val settings = if (args.narg() >= 2) args.arg(2).toContentSettings() else null
            val block = DynamicContent.registerBlock(args.arg1().tojstring(), settings) ?: return NIL
            return LuaBlockState(block.defaultBlockState())
        }
    }

    /**
     * content.registerBlockItem("ns:my_block", blockState [, settings])
     */
    class RegisterBlockItem : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL

            // Аргументы могут идти как (id, state [, settings]) так и (id, settings, state)
            var stateArg: LuaValue? = null
            var settingsArg: LuaValue? = null
            for (i in 2..args.narg()) {
                val arg = args.arg(i)
                if (arg.isBlock() && stateArg == null) stateArg = arg
                else if (settingsArg == null && arg.toContentSettings() != null) settingsArg = arg
            }
            val state = stateArg?.toBlock() ?: return NIL
            val settings = settingsArg?.toContentSettings()

            val item = DynamicContent.registerBlockItem(args.arg1().tojstring(), state.block, settings) ?: return NIL
            return LuaItemStack(ItemStack(item))
        }
    }

    /**
     * content.getItemTexture("ns:id") — Identifier текстуры строкой,
     * файл загружается при первом вызове (путь из settings.texture).
     */
    class GetItemTexture : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            if (!arg.isstring()) return NIL
            val identifier = DynamicContent.getDynamicTexture(arg.tojstring()) ?: return NIL
            return valueOf(identifier.toString())
        }
    }
}
