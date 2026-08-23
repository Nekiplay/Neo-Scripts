package com.nekiplay.neoscripts.common.features.lua.objects.misc

import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaMutableBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaBox
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
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
            "createVector3", "createVector3d" -> CreateVector3()
            else -> super.get(key)
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
     * Создает тип сущности из идентификатора (например "minecraft:sheep"),
     * который возвращает entities.getAll().
     */
    class CreateEntity : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            if (!arg.isstring()) return NIL

            return try {
                val id = Identifier.parse(arg.tojstring())
                val holder = BuiltInRegistries.ENTITY_TYPE.get(id)
                if (holder.isPresent) {
                    LuaEntityType(holder.get().value())
                } else {
                    NIL
                }
            } catch (e: Exception) {
                NIL
            }
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