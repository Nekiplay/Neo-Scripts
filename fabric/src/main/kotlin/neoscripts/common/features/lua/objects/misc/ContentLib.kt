package com.nekiplay.neoscripts.common.features.lua.objects.misc

import com.nekiplay.neoscripts.client.sugar.isBlock
import com.nekiplay.neoscripts.client.sugar.toBlock
import com.nekiplay.neoscripts.client.sugar.toContentSettings
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaContentSettings
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction

/**
 * Библиотека динамической регистрации предметов и блоков.
 * Подключается через require("content") и доступна в общих
 * автостарт-скриптах (neoscripts/autostart).
 * Документация по свойствам блока: https://docs.fabricmc.net/develop/blocks/first-block
 *
 * API:
 *  content.createSettings({ name=..., texture=..., model=..., maxStackSize=..., sound=..., mapColor=...,
 *                           hardness=..., resistance=..., luminance=..., friction=...,
 *                           noCollision=..., noOcclusion=..., instabreak=..., requiresTool=..., offsetType=..., copyFrom=... })
 *  content.registerItem("ns:id" [, settings])                  -> string
 *  content.registerBlock("ns:id" [, settings])                 -> LuaBlockState  // куб, см. first-block
 *  content.registerSlab("ns:id" [, settings])                  -> LuaBlockState  // SlabBlock, модель slab/slab_top
 *  content.registerStairs("ns:id", baseBlockIdOrState [, settings]) -> LuaBlockState // StairBlock, base = текстура/состояние опоры
 *  content.registerDoor("ns:id" [, settings [, blockSetType]]) -> LuaBlockState // DoorBlock, blockSetType ="oak"/"iron"/"stone"/"copper"/...
 *  content.registerTrapdoor("ns:id" [, settings [, blockSetType]]) -> LuaBlockState
 *  content.registerFence("ns:id" [, settings])                 -> LuaBlockState
 *  content.registerBlockItem("ns:id", blockState [, settings]) -> string
 *  content.registerFood("ns:id" [, settings [, foodTable]])    -> string
 *  content.registerDrink("ns:id" [, settings [, foodTable]])   -> string
 *  content.registerTool/Paxel("ns:id" [, settings [, toolTable]]) -> string
 *  content.getItemTexture("ns:id")                             -> string | nil
 *
 *  settings.model: "minecraft:block/cube_all" | "minecraft:diamond_block" | "tinker_construct:block/cast" |
 *                  "config/neoscripts/models/my_model.json" (путь к JSON-файлу)
 *  settings.sound: "stone","wood","grass","metal","glass" и т.д. (SoundType)
 *  settings.copyFrom: "minecraft:stone" — скопировать свойства блока
 *  foodTable: { nutrition=4, saturation=0.6, alwaysEdible=false }
 *  toolTable: { type="pickaxe|axe|shovel|hoe|sword|paxel", material="diamond|iron|...", damage=1, speed=-2.8 }
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
            "registerSlab" -> RegisterSlab()
            "registerStairs", "registerStair" -> RegisterStairs()
            "registerDoor" -> RegisterDoor()
            "registerTrapdoor", "registerTrapDoor" -> RegisterTrapdoor()
            "registerFence" -> RegisterFence()
            "registerBlockItem" -> RegisterBlockItem()
            "registerFood" -> RegisterFood()
            "registerDrink" -> RegisterDrink()
            "registerTool" -> RegisterTool()
            "registerPaxel" -> RegisterTool()
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
     * ВНИМАНИЕ: выполняется до биндинга компонентов (onInitialize), поэтому
     * возвращает ID предмета СТРОКОЙ, а не LuaItemStack — создать ItemStack
     * на этом этапе невозможно (Holder.components ещё не связаны).
     * После загрузки игры предмет доступен через items.get("ns:id").
     */
    class RegisterItem : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            val settings = if (args.narg() >= 2) args.arg(2).toContentSettings() else null
            val item = DynamicContent.registerItem(args.arg1().tojstring(), settings) ?: return NIL
            return valueOf(BuiltInRegistries.ITEM.getKey(item).toString())
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
     * Возвращает ID предмета строкой (см. registerItem).
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
            return valueOf(BuiltInRegistries.ITEM.getKey(item).toString())
        }
    }

    companion object {
        private fun extractFoodAndSettings(args: Varargs, startIdx: Int): Pair<LuaContentSettings?, org.luaj.vm2.LuaValue?> {
            var settings: LuaContentSettings? = null
            var foodTable: org.luaj.vm2.LuaValue? = null
            for (i in startIdx..args.narg()) {
                val arg = args.arg(i)
                if (arg.toContentSettings() != null && settings == null) settings = arg.toContentSettings()
                else if (arg.istable() && arg.toContentSettings() == null && foodTable == null) foodTable = arg
            }
            return settings to foodTable
        }
    }

    class RegisterFood : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            val (settings, foodTable) = extractFoodAndSettings(args, 2)
            val food = DynamicContent.parseFoodTable(foodTable) ?: DynamicContent.buildFoodProperties(4, 0.6f, false)
            val item = DynamicContent.registerFood(args.arg1().tojstring(), settings, food) ?: return NIL
            return valueOf(BuiltInRegistries.ITEM.getKey(item).toString())
        }
    }

    class RegisterDrink : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            val (settings, foodTable) = extractFoodAndSettings(args, 2)
            val food = DynamicContent.parseFoodTable(foodTable)
            val item = DynamicContent.registerDrink(args.arg1().tojstring(), settings, food) ?: return NIL
            return valueOf(BuiltInRegistries.ITEM.getKey(item).toString())
        }
    }

    class RegisterSlab : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            val settings = if (args.narg() >= 2) args.arg(2).toContentSettings() else null
            val block = DynamicContent.registerSlab(args.arg1().tojstring(), settings) ?: return NIL
            return LuaBlockState(block.defaultBlockState())
        }
    }

    class RegisterStairs : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            val rawId = args.arg1().tojstring()
            var baseId: String? = null
            var settings: LuaContentSettings? = null
            for (i in 2..args.narg()) {
                val arg = args.arg(i)
                when {
                    arg.isBlock() && baseId == null -> {
                        // LuaBlockState -> block id
                        try { baseId = arg.toBlock()?.let { BuiltInRegistries.BLOCK.getKey(it.block).toString() } } catch (_: Exception) {}
                    }
                    arg.isstring() && baseId == null -> baseId = arg.tojstring()
                    arg.toContentSettings() != null && settings == null -> settings = arg.toContentSettings()
                }
            }
            val block = DynamicContent.registerStairs(rawId, baseId, settings) ?: return NIL
            return LuaBlockState(block.defaultBlockState())
        }
    }

    class RegisterDoor : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            var settings: LuaContentSettings? = null
            var setType: String? = null
            for (i in 2..args.narg()) {
                val arg = args.arg(i)
                if (arg.toContentSettings() != null && settings == null) settings = arg.toContentSettings()
                else if (arg.isstring() && setType == null) setType = arg.tojstring()
            }
            val block = DynamicContent.registerDoor(args.arg1().tojstring(), setType, settings) ?: return NIL
            return LuaBlockState(block.defaultBlockState())
        }
    }

    class RegisterTrapdoor : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            var settings: LuaContentSettings? = null
            var setType: String? = null
            for (i in 2..args.narg()) {
                val arg = args.arg(i)
                if (arg.toContentSettings() != null && settings == null) settings = arg.toContentSettings()
                else if (arg.isstring() && setType == null) setType = arg.tojstring()
            }
            val block = DynamicContent.registerTrapdoor(args.arg1().tojstring(), setType, settings) ?: return NIL
            return LuaBlockState(block.defaultBlockState())
        }
    }

    class RegisterFence : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            val settings = if (args.narg() >= 2) args.arg(2).toContentSettings() else null
            val block = DynamicContent.registerFence(args.arg1().tojstring(), settings) ?: return NIL
            return LuaBlockState(block.defaultBlockState())
        }
    }

    class RegisterTool : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (!args.arg(1).isstring()) return NIL
            var settings: LuaContentSettings? = null
            var toolTable: org.luaj.vm2.LuaValue? = null
            for (i in 2..args.narg()) {
                val arg = args.arg(i)
                if (arg.toContentSettings() != null && settings == null) settings = arg.toContentSettings()
                else if (arg.istable() && toolTable == null) toolTable = arg
            }
            val item = DynamicContent.registerTool(args.arg1().tojstring(), settings, toolTable) ?: return NIL
            return valueOf(BuiltInRegistries.ITEM.getKey(item).toString())
        }
    }

    /**
     * content.getItemTexture("ns:id") — Identifier текстуры строкой,
     * как она лежит в рантайм ресурспаке ("ns:textures/item/path.png").
     * nil, если текстура не задана или файл не прочитан.
     */
    class GetItemTexture : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            if (!arg.isstring()) return NIL
            val rawId = arg.tojstring()
            if (DynamicContent.textureData.containsKey(rawId)) {
                try {
                    val id = Identifier.parse(rawId)
                    return valueOf(Identifier.fromNamespaceAndPath(id.namespace, "textures/item/${id.path}.png").toString())
                } catch (_: Exception) {
                }
            }
            return NIL
        }
    }
}
