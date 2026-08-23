package com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core

import net.minecraft.world.level.chunk.LevelChunk
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JavaInstance

class LuaLevelChunk(val chunk: LevelChunk): LuaUserdata(chunk) {
    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "javaClass", "class" -> JavaInstance(chunk)
            "pos" -> LuaChunkPos(chunk.pos)
            "height" -> valueOf(chunk.height)
            "minY" -> valueOf(chunk.minY)
            "maxY" -> valueOf(chunk.maxY)
            "maxSectionY" -> valueOf(chunk.maxSectionY)
            else -> super.get(key)
        }
    }


    override fun eq(other: LuaValue?): LuaValue {
        val user = other?.touserdata()
        if (other is LuaLevelChunk) {
            if (other.chunk == chunk) {
                return TRUE
            }
        }
        if (user is LevelChunk) {
            if (user == chunk) {
                return TRUE
            }
            return TRUE
        }
        return FALSE
    }

    override fun typename(): String = "chunkpos"
}