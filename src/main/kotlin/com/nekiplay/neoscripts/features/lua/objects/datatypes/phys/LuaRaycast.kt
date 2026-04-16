package com.nekiplay.neoscripts.features.lua.objects.datatypes.phys

import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaVector3d
import kotlinx.coroutines.withTimeout
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

class LuaRaycast(val hitResult: HitResult): LuaUserdata(hitResult) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // --- Fields (Getters) ---
            "type" -> {
                if (hitResult.type == HitResult.Type.ENTITY) {
                    valueOf("entity")
                }
                else if (hitResult.type == HitResult.Type.BLOCK) {
                    valueOf("block")
                } else {
                    valueOf("miss")
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
            "blockpos" -> {
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