package com.nekiplay.neoscripts.common.features.lua.objects.misc

import com.nekiplay.neoscripts.ClientMain.mc
import com.nekiplay.neoscripts.ServerMain
import com.nekiplay.neoscripts.client.sugar.isBlock
import com.nekiplay.neoscripts.client.sugar.isContentSettings
import com.nekiplay.neoscripts.client.sugar.toBlock
import com.nekiplay.neoscripts.client.sugar.toContentSettings
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaContentSettings
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaTransform
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaMutableBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction

/**
 * Тип сущности, полученный из creator.createEntity("minecraft:pig").
 * Передаётся в world.spawnEntity(...)
 */
class LuaEntityType(val entityType: EntityType<*>) : LuaUserdata(entityType) {
    override fun typename(): String = "entitytype"
}

class Creator : LuaValue() {
    override fun typename(): String = "creator"
    override fun tojstring(): String = "CreatorObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "createAABB", "createBox" -> CreateBox()
            "createDirection" -> CreateDirection()
            "createBlockPos" -> CreateBlockPos()
            "createMutableBlockPos" -> CreateMutableBlockPos()
            "createBlockState" -> CreateBlockState()
            "createEntity", "createEntityType" -> CreateEntity()
            "createSettings", "contentSettings", "itemSettings", "blockSettings" -> CreateContentSettings()
            "registerItem" -> RegisterItem()
            "registerBlock" -> RegisterBlock()
            "registerBlockItem" -> RegisterBlockItem()
            "createVector3", "createVector3d" -> CreateVector3()
            "createTransform", "createTransformation" -> CreateTransform()
            else -> super.get(key)
        }
    }

    /**
     * creator.createTransform([tx, ty, tz], [sx, sy, sz], [rx, ry, rz])
     * Смещение, масштаб и поворот (в градусах) для display-сущностей.
     * Все параметры опциональны: без аргументов — единичная трансформация.
     */
    class CreateTransform : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val translation = if (args.narg() >= 3 && args.arg(1).isnumber() && args.arg(2).isnumber() && args.arg(3).isnumber()) {
                Vector3f(args.arg(1).tofloat(), args.arg(2).tofloat(), args.arg(3).tofloat())
            } else {
                Vector3f(0f, 0f, 0f)
            }

            val scale = if (args.narg() >= 6 && args.arg(4).isnumber() && args.arg(5).isnumber() && args.arg(6).isnumber()) {
                Vector3f(args.arg(4).tofloat(), args.arg(5).tofloat(), args.arg(6).tofloat())
            } else {
                Vector3f(1f, 1f, 1f)
            }

            val rotation = if (args.narg() >= 9 && args.arg(7).isnumber() && args.arg(8).isnumber() && args.arg(9).isnumber()) {
                Vector3f(args.arg(7).tofloat(), args.arg(8).tofloat(), args.arg(9).tofloat())
            } else {
                Vector3f(0f, 0f, 0f)
            }

            return LuaTransform(translation, scale, rotation)
        }
    }

    class CreateVector3 : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isnumber() && args.arg(2).isnumber() && args.arg(3).isnumber()) {
                return LuaVector3d(
                    Vec3(args.arg(1).todouble(), args.arg(2).todouble(), args.arg(3).todouble())
                )
            }
            return NIL
        }
    }

    class CreateBlockState : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isint()) {
                return LuaBlockState(
                    Block.stateById(args.arg(1).toint())
                )
            }
            return NIL
        }
    }

    /**
     * Создает экземпляр сущности по идентификатору (например "minecraft:sheep")
     * и возвращает её как LuaEntity (как new Entity в Java).
     * Сущность НЕ в мире: настройте поля и заспавньте через world.spawnEntity(entity, ...).
     * Мир нужен только потому, что EntityType.create(Level, ...) в ваниле требует Level;
     * берётся клиентский уровень, а если его нет — серверный.
     */
    class CreateEntity : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            if (!arg.isstring()) return NIL

            return try {
                val id = Identifier.parse(arg.tojstring())
                val holder = BuiltInRegistries.ENTITY_TYPE.get(id)
                val level = mc.level ?: ServerMain.SERVER?.getLevel(Level.OVERWORLD)
                if (holder.isPresent && level != null) {
                    val entity = holder.get().value().create(level, EntitySpawnReason.COMMAND)
                    if (entity != null) LuaEntity(entity) else NIL
                } else {
                    NIL
                }
            } catch (e: Exception) {
                NIL
            }
        }
    }

    /**
     * creator.createSettings({ name = "Мой предмет", texture = "neoscripts:textures/item/my.png",
     *                           maxStackSize = 16, fireResistant = true, rarity = "epic" })
     * Создает LuaContentSettings для registerItem/registerBlock/registerBlockItem.
     * Все поля опциональны. Настройку можно менять и после создания: settings.name = "..."
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
     * creator.registerItem("neoscripts:my_item" [, settings])
     * Динамически регистрирует предмет в BuiltInRegistries.ITEM.
     * Работает из скриптов автозагрузки (реестр временно размораживается).
     * settings — результат creator.createSettings({...}) (опционально):
     * задает имя, текстуру, размер стака, редкость и т.д.
     * Возвращает LuaItemStack нового предмета или nil при ошибке.
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
     * creator.registerBlock("neoscripts:my_block" [, settings])
     * Динамически регистрирует блок в BuiltInRegistries.BLOCK.
     * Возвращает LuaBlockState (состояние по умолчанию) или nil при ошибке.
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
     * creator.registerBlockItem("neoscripts:my_block", blockState [, settings])
     * Регистрирует предмет-блок (BlockItem) для блока. Второй аргумент —
     * результат creator.registerBlock (LuaBlockState).
     * Возвращает LuaItemStack или nil.
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
                else if (settingsArg == null && (arg.isContentSettings() || arg.istable())) settingsArg = arg
            }
            val state = stateArg?.toBlock() ?: return NIL
            val settings = settingsArg?.toContentSettings()

            val item = DynamicContent.registerBlockItem(args.arg1().tojstring(), state.block, settings) ?: return NIL
            return LuaItemStack(ItemStack(item))
        }
    }

    class CreateBlockPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isint() && args.arg(2).isint() && args.arg(3).isint()) {
                return LuaBlockPos(
                    BlockPos(args.arg(1).toint(), args.arg(2).toint(), args.arg(3).toint())
                )
            }
            return NIL
        }
    }

    class CreateMutableBlockPos : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            return if (args.arg(1).isint() && args.arg(2).isint() && args.arg(3).isint()) {
                LuaMutableBlockPos(
                    BlockPos.MutableBlockPos(args.arg(1).toint(), args.arg(2).toint(), args.arg(3).toint())
                )
            } else {
                LuaMutableBlockPos(
                    BlockPos.MutableBlockPos()
                )
            }
        }
    }

    class CreateDirection : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isstring()) {
                return LuaDirection(
                    Direction.valueOf(args.arg1().tojstring().uppercase())
                )
            }
            return NIL
        }
    }

    class CreateBox : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.arg(1).isnumber() && args.arg(2).isnumber() && args.arg(3).isnumber() && args.arg(4).isnumber() && args.arg(5).isnumber() && args.arg(6).isnumber()) {
                return LuaBox(
                    AABB(
                        args.arg(1).todouble(),
                        args.arg(2).todouble(),
                        args.arg(3).todouble(),
                        args.arg(4).todouble(),
                        args.arg(5).todouble(),
                        args.arg(6).todouble()
                    )
                )
            }
            return NIL
        }
    }
}