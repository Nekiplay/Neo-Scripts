package com.nekiplay.hypixelcry.features.lua.objects.render

import com.nekiplay.hypixelcry.pathfinder.utils.mc
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.awt.Color
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class TwoRenderObject(private val context: DrawContext?, private val scriptId: String? = null): LuaValue() {
    // Кэш для загруженных текстур с разделением по скриптам
    companion object {
        private val textureCache = ConcurrentHashMap<String, MutableMap<String, Identifier>>()
        private val textureCounter = AtomicInteger(0)

        // Очистка кэша для конкретного скрипта
        fun clearScriptCache(scriptId: String) {
            textureCache[scriptId]?.values?.forEach { identifier ->
                mc.textureManager.destroyTexture(identifier)
            }
            textureCache.remove(scriptId)
        }

        // Полная очистка всех кэшей
        fun clearAllCaches() {
            textureCache.values.forEach { scriptCache ->
                scriptCache.values.forEach { identifier ->
                    mc.textureManager.destroyTexture(identifier)
                }
            }
            textureCache.clear()
        }
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getWindowScale" -> GetWindowScaleFunction()
            "getTextWidth" -> GetTextWidthFunction()
            "renderText" -> RenderTextFunction()
            "renderImage" -> RenderImageFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class GetTextWidthFunction : OneArgFunction() {
        override fun call(text: LuaValue): LuaValue {
            if (text.isstring()) {
                val textRenderer: TextRenderer? = mc.textRenderer
                val width: Int? = textRenderer?.getWidth(text.tojstring())
                if (width != null) {
                    return valueOf(width)
                }
            }
            return NIL
        }
    }

    private inner class GetWindowScaleFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val table = tableOf()
            val width: Int = mc.window.scaledWidth
            val height: Int = mc.window.scaledHeight

            table.set("width", width)
            table.set("height", height)
            return table
        }
    }

    private inner class RenderTextFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val text = if (table.get("text").isstring()) table.get("text").tojstring() else "Empty"

                val x: Int = if (table.get("x").isnumber()) table.get("x").toint() else 0
                val y: Int = if (table.get("y").isnumber()) table.get("y").toint() else 0

                val red = if (table.get("red").isnumber()) table.get("red").toint() else 255
                val green = if (table.get("green").isnumber()) table.get("green").toint() else 255
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 255
                val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 255

                val color = (alpha and 0xFF shl 24) or
                        (red and 0xFF shl 16) or
                        (green and 0xFF shl 8) or
                        (blue and 0xFF)

                val isShadow = if (table.get("shadow").isboolean()) table.get("shadow").toboolean() else true

                val textRenderer: TextRenderer? = mc.textRenderer
                context.drawText(textRenderer, Text.literal(text), x, y, color, isShadow)
            }
            return NIL
        }
    }

    private inner class RenderImageFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val path = if (table.get("path").isstring()) table.get("path").tojstring() else return NIL
                val x: Int = if (table.get("x").isnumber()) table.get("x").toint() else 0
                val y: Int = if (table.get("y").isnumber()) table.get("y").toint() else 0
                val width: Int = if (table.get("width").isnumber()) table.get("width").toint() else 0
                val height: Int = if (table.get("height").isnumber()) table.get("height").toint() else 0

                val u: Int = if (table.get("u").isnumber()) table.get("u").toint() else 0
                val v: Int = if (table.get("v").isnumber()) table.get("v").toint() else 0
                val regionWidth: Int = if (table.get("region_width").isnumber()) table.get("region_width").toint() else width
                val regionHeight: Int = if (table.get("region_height").isnumber()) table.get("region_height").toint() else height

                try {
                    val identifier = loadTexture(path)
                    if (identifier != null) {
                        context.drawTexture(
                            RenderPipelines.GUI_TEXTURED, identifier,
                            x, y,
                            u.toFloat(), v.toFloat(),
                            width, height,
                            regionWidth, regionHeight,
                            regionWidth, regionHeight
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return NIL
        }
    }

    private inner class ClearImageCacheFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            // Очищаем кэш текстур только для текущего скрипта
            scriptId?.let { TwoRenderObject.clearScriptCache(it) }
            return NIL
        }
    }

    /**
     * Загружает текстуру из файла и возвращает её Identifier
     */
    private fun loadTexture(path: String): Identifier? {
        val scriptCacheId = scriptId ?: "global"

        // Проверяем кэш для текущего скрипта
        val scriptCache = textureCache.getOrPut(scriptCacheId) { ConcurrentHashMap() }
        if (scriptCache.containsKey(path)) {
            return scriptCache[path]
        }

        try {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                return null
            }

            FileInputStream(file).use { inputStream ->
                val nativeImage = NativeImage.read(inputStream)

                // Используем правильный конструктор NativeImageBackedTexture
                val textureName = "hypixelcry:texture_${scriptCacheId}_${textureCounter.getAndIncrement()}"
                val texture = NativeImageBackedTexture(
                    Supplier { textureName },
                    nativeImage
                )

                // Создаем идентификатор
                val identifier = Identifier.of("hypixelcry", "texture_${scriptCacheId}_${textureCounter.get()}")

                // Альтернативный способ создания Identifier для разных версий Minecraft
                // val identifier = try {
                //     Identifier.of("hypixelcry", "texture_${scriptCacheId}_${textureCounter.get()}")
                // } catch (e: Exception) {
                //     Identifier("hypixelcry", "texture_${scriptCacheId}_${textureCounter.get()}")
                // }

                // Регистрируем текстуру
                mc.textureManager.registerTexture(identifier, texture)

                // Сохраняем в кэш текущего скрипта
                scriptCache[path] = identifier

                return identifier
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun typename(): String = "2d_renderer"
    override fun tojstring(): String = "2DRenderObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return LuaValue.TUSERDATA
    }
}