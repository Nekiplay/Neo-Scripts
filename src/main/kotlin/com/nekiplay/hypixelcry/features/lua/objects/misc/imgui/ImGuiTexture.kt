package com.nekiplay.hypixelcry.features.lua.objects.misc.imgui

import com.mojang.blaze3d.platform.NativeImage
import com.nekiplay.hypixelcry.features.lua.objects.render.TwoRenderObject.Companion.textureCache
import com.nekiplay.hypixelcry.features.lua.objects.render.TwoRenderObject.Companion.textureCounter
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier
import kotlin.collections.set


class ImGuiTexture : LuaUserdata(null) {
    private val _texture = AtomicInteger(-1)
    private var _identifier: ResourceLocation? = null

    public fun getTexture(): Int {
        return _texture.get()
    }

    private val loadImageInstance = LoadImage()

    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "loadImage" -> loadImageInstance
            else -> super.get(key)
        } as LuaValue
    }

    private inner class LoadImage : OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            if (path.isstring()) {
                val textureId = loadTexture(path.tojstring())
                _texture.set(textureId)
                return LuaValue.valueOf(1)
            }
            return LuaValue.valueOf(0)
        }
    }

    private inner class Release : ZeroArgFunction() {
        override fun call(): LuaValue {
            releaseTexture()
            return NIL
        }
    }

    private fun loadTexture(path: String): Int {
        try {
            // Release any previously loaded texture
            releaseTexture()

            val file = File(path)
            if (!file.exists() || !file.isFile) {
                println("File does not exist or is not a file: $path")
                return -1
            }

            if (!file.canRead()) {
                println("Cannot read file: $path")
                return -1
            }

            FileInputStream(file).use { inputStream ->
                val nativeImage = NativeImage.read(inputStream)

                // Generate unique identifier
                val textureName = "texture_${file.nameWithoutExtension}_${System.currentTimeMillis()}"
                _identifier = ResourceLocation.fromNamespaceAndPath("hypixelcry", textureName)

                // Create texture
                val texture = DynamicTexture(Supplier { textureName }, nativeImage)

                // Register texture
                mc.textureManager.register(_identifier, texture)

                // Get OpenGL texture ID
                val glTextureId = texture.texture.usage()
                _texture.set(glTextureId)

                return glTextureId
            }
        } catch (e: Exception) {
            println("Failed to load texture from $path: ${e.message}")
            e.printStackTrace()
            return -1
        }
    }

    private fun releaseTexture() {
        try {
            _identifier?.let { identifier ->
                if (mc.textureManager.getTexture(identifier) != null) {
                    mc.textureManager.release(identifier)
                }
            }
            _identifier = null
            _texture.set(-1)
        } catch (e: Exception) {
            println("Error releasing texture: ${e.message}")
        }
    }
}