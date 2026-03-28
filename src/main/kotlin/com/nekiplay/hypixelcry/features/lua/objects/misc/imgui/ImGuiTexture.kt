package com.nekiplay.hypixelcry.features.lua.objects.misc.imgui

import com.mojang.blaze3d.opengl.GlTextureView
import com.mojang.blaze3d.platform.NativeImage
import com.nekiplay.hypixelcry.HypixelCry.mc
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier


class ImGuiTexture(val texture: AtomicInteger) : LuaUserdata(AtomicInteger(0)) {
    private var _identifier: Identifier? = null

    override fun get(key: LuaValue): LuaValue {
        return when (val field = key.tojstring()) {
            "loadImage" -> LoadImage()
            "release" -> Release()
            "getId" -> GetID()
            else -> super.get(key)
        } as LuaValue
    }

    private inner class GetID : OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            return valueOf(texture.get())
        }
    }

    private inner class LoadImage : OneArgFunction() {
        override fun call(path: LuaValue): LuaValue {
            if (path.isstring()) {
                val textureId = loadTexture(path.tojstring())
                texture.set(textureId)
                return TRUE
            }
            return FALSE
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
                return 0
            }

            if (!file.canRead()) {
                println("Cannot read file: $path")
                return 0
            }

            FileInputStream(file).use { inputStream ->
                val nativeImage = NativeImage.read(inputStream)

                // Generate unique identifier
                val textureName = "texture_${file.nameWithoutExtension}_${System.currentTimeMillis()}"
                _identifier = Identifier.fromNamespaceAndPath("hypixelcry", textureName)
                val indf = _identifier
                if (indf != null) {
                    // Create texture
                    val texture = DynamicTexture(Supplier { textureName }, nativeImage)

                    // Register texture
                    mc.textureManager.register(indf, texture)

                    val texture2 = mc.textureManager.getTexture(indf).getTextureView() as GlTextureView
                    return texture2.texture().glId()
                }
                return 0
            }
        } catch (e: Exception) {
            println("Failed to load texture from $path: ${e.message}")
            e.printStackTrace()
            return 0
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
            texture.set(0)
        } catch (e: Exception) {
            println("Error releasing texture: ${e.message}")
        }
    }
}