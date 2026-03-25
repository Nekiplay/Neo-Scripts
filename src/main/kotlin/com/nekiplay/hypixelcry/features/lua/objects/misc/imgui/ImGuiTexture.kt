package com.nekiplay.hypixelcry.features.lua.objects.misc.imgui

import com.mojang.blaze3d.opengl.GlTextureView
import com.mojang.blaze3d.platform.NativeImage
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.core.SimpleLuaWrapper
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua

import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class ImGuiTexture(L: Lua?, val texture: AtomicInteger = AtomicInteger(0)) : SimpleLuaWrapper(L) {
    private var _identifier: Identifier? = null

    override fun getFieldValue(l: Lua, key: String): Any? {
        return when (key) {
            "loadImage" -> JFunction { loadImage(it) }
            "release" -> JFunction { release(it) }
            "getId" -> JFunction { getId(it) }
            else -> null
        }
    }

    private fun getId(l: Lua): Int {
        l.push(texture.get().toDouble())
        return 1
    }

    private fun loadImage(l: Lua): Int {
        if (l.isString(1)) {
            val path = l.toString(1) ?: ""
            val textureId = loadTexture(path)
            texture.set(textureId)
            l.push(true)
        } else {
            l.push(false)
        }
        return 1
    }

    private fun release(l: Lua): Int {
        releaseTexture()
        l.pushNil()
        return 1
    }

    private fun loadTexture(path: String): Int {
        try {
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

                val textureName = "texture_${file.nameWithoutExtension}_${System.currentTimeMillis()}"
                _identifier = Identifier.fromNamespaceAndPath("hypixelcry", textureName)
                val indf = _identifier
                if (indf != null) {
                    val texture = DynamicTexture(Supplier { textureName }, nativeImage)

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