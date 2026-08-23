package com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys

import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JavaInstance

class LuaRaycast(val hitResult: HitResult): LuaUserdata(hitResult) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // --- Fields (Getters) ---
            "javaClass", "class" -> JavaInstance(hitResult);
            "type" -> {
                when (hitResult.type) {
                    HitResult.Type.ENTITY -> {
                        valueOf("entity")
                    }
                    HitResult.Type.BLOCK -> {
                        valueOf("block")
                    }
                    else -> {
                        valueOf("miss")
                    }
                }
            }
            "data", "entity" -> LuaEntity((hitResult as EntityHitResult).entity)
            "side" -> {
                if (hitResult.type == HitResult.Type.BLOCK) {
                    val result = hitResult as BlockHitResult
                    LuaDirection(result.direction)
                }
                else {
                    NIL
                }
            }
            "blockpos", "blockPos" -> {
                if (hitResult.type == HitResult.Type.BLOCK) {
                    val result = hitResult as BlockHitResult
                    LuaBlockPos(result.blockPos)
                }
                else {
                    NIL
                }
            }
            "pos", "location" -> LuaVector3d(hitResult.location)

            else -> super.get(key)
        }
    }
}