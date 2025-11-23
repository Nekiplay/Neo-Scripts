package com.nekiplay.hypixelcry.features.lua.objects.render

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector
import net.minecraft.block.BlockState
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.Vec3d
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

class WorldRendererObject(private val context: PrimitiveCollector?): LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "renderFilled" -> RenderFilledFunction()
            "renderOutline" -> RenderOutlineFunction()
            "renderText" -> RenderTextFunction()
            "renderLinesFromPoints" -> RenderLinesFromPointsFunction()
            "renderLineFromCursor" -> RenderLineFromCursorFunction()
            "renderImage" -> RenderImageFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class RenderFilledFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val x = if (table.get("x").isnumber()) table.get("x").todouble() else 0.0
                val y = if (table.get("y").isnumber()) table.get("y").todouble() else 0.0
                val z = if (table.get("z").isnumber()) table.get("z").todouble() else 0.0

                val x2 = if (table.get("x2").isnumber()) table.get("x2").todouble() else null
                val y2 = if (table.get("y2").isnumber()) table.get("y2").todouble() else null
                val z2 = if (table.get("z2").isnumber()) table.get("z2").todouble() else null

                val red = if (table.get("red").isnumber()) table.get("red").toint() else 0
                val green = if (table.get("green").isnumber()) table.get("green").toint() else 0
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 0
                val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 0

                val throughWalls = if (table.get("through_walls").isboolean()) table.get("through_walls").toboolean() else true

                val blockStateObject = table.get("blockState")
                val blockState = when {
                    blockStateObject.isuserdata() && blockStateObject.touserdata() is LuaBlockState -> (blockStateObject.touserdata() as LuaBlockState).getBlockState()
                    blockStateObject.isuserdata() && blockStateObject.touserdata() is BlockState -> blockStateObject.touserdata() as BlockState
                    else -> null
                }

                val colorComponents = floatArrayOf(
                    red.toFloat() / 255.0f,
                    green.toFloat() / 255.0f,
                    blue.toFloat() / 255.0f
                )

                val alphaComponent = alpha.toFloat() / 255.0f

                if (blockState == null) {
                    context.submitFilledBox(BlockPos(x.toInt(), y.toInt(), z.toInt()), colorComponents, alphaComponent, throughWalls)
                }
                else if (x2 != null && y2 != null && z2 != null) {
                    context.submitFilledBox(Box(Vec3d(x, y, z), Vec3d(x2, y2, z2)), colorComponents, alphaComponent, throughWalls)
                }
                else {
                    context.submitFilledBox(blockState.getCollisionShape(mc.world, BlockPos(x.toInt(), y.toInt(), z.toInt())).boundingBox, colorComponents, alphaComponent, throughWalls)
                }
                return TRUE
            }
            return NIL
        }
    }

    private inner class RenderOutlineFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val x = if (table.get("x").isnumber()) table.get("x").todouble() else 0.0
                val y = if (table.get("y").isnumber()) table.get("y").todouble() else 0.0
                val z = if (table.get("z").isnumber()) table.get("z").todouble() else 0.0

                val x2 = if (table.get("x2").isnumber()) table.get("x2").todouble() else null
                val y2 = if (table.get("y2").isnumber()) table.get("y2").todouble() else null
                val z2 = if (table.get("z2").isnumber()) table.get("z2").todouble() else null

                val red = if (table.get("red").isnumber()) table.get("red").toint() else 0
                val green = if (table.get("green").isnumber()) table.get("green").toint() else 0
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 0
                val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 0

                val lineWidth = if (table.get("line_width").isnumber()) table.get("line_width").tofloat() else 1.0f

                val throughWalls = if (table.get("through_walls").isboolean()) table.get("through_walls").toboolean() else true

                val colorComponents = floatArrayOf(
                    red.toFloat() / 255.0f,
                    green.toFloat() / 255.0f,
                    blue.toFloat() / 255.0f,
                    alpha.toFloat() / 255.0f
                )

                val blockStateObject = table.get("blockState")
                val blockState = when {
                    blockStateObject.isuserdata() && blockStateObject.touserdata() is LuaBlockState -> (blockStateObject.touserdata() as LuaBlockState).getBlockState()
                    blockStateObject.isuserdata() && blockStateObject.touserdata() is BlockState -> blockStateObject.touserdata() as BlockState
                    else -> null
                }

                if (blockState == null) {
                    context.submitOutlinedBox(BlockPos(x.toInt(), y.toInt(), z.toInt()), colorComponents, lineWidth, throughWalls)
                }
                else if (x2 != null && y2 != null && z2 != null) {
                    context.submitOutlinedBox(Box(Vec3d(x, y, z), Vec3d(x2, y2, z2)), colorComponents, lineWidth, throughWalls)
                }
                else {
                    context.submitOutlinedBox(blockState.getCollisionShape(mc.world, BlockPos(x.toInt(), y.toInt(), z.toInt())).boundingBox, colorComponents, lineWidth, throughWalls)
                }
                return TRUE
            }
            return NIL
        }
    }

    fun getArgb(red: Int, green: Int, blue: Int): Int {
        return (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private inner class RenderTextFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val x: Double = if (table.get("x").isnumber()) table.get("x").todouble() else 0.0
                val y: Double = if (table.get("y").isnumber()) table.get("y").todouble() else 0.0
                val z: Double = if (table.get("z").isnumber()) table.get("z").todouble() else 0.0

                val text = if (table.get("text").isstring()) table.get("text").tojstring() else "Empty"
                val scale = if (table.get("scale").isnumber()) table.get("scale").tofloat() else 1f

                val red = if (table.get("red").isnumber()) table.get("red").toint() else -1
                val green = if (table.get("green").isnumber()) table.get("green").toint() else -1
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else -1

                val throughWalls = if (table.get("through_walls").isboolean()) table.get("through_walls").toboolean() else true
                val pos = Vec3d(x, y, z)

                if (red != -1 && green != -1 && blue != -1) {
                    context.submitText(
                        Text.of(text),
                        pos,
                        ColorHelper.fromAbgr(getArgb(red, green, blue)),
                        scale,
                        0.5f,
                        throughWalls
                    );
                    return TRUE
                }
                else {
                    context.submitText(
                        Text.of(text),
                        pos,
                        -1,
                        scale,
                        0.5f,
                        throughWalls
                    );
                    return TRUE
                }
            }
            return NIL
        }
    }

    private inner class RenderLinesFromPointsFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                // Parse points array
                val pointsTable = table.get("points")
                if (pointsTable.istable()) {
                    val pointsList = mutableListOf<Vec3d>()
                    var i = 0
                    while (true) {
                        val pointTable = pointsTable.get(i)
                        if (pointTable.istable()) {
                            val x = if (pointTable.get("x").isnumber()) pointTable.get("x").todouble() else 0.0
                            val y = if (pointTable.get("y").isnumber()) pointTable.get("y").todouble() else 0.0
                            val z = if (pointTable.get("z").isnumber()) pointTable.get("z").todouble() else 0.0
                            pointsList.add(Vec3d(x, y, z))
                            i++
                        } else {
                            // If 0-based fails, try 1-based
                            if (i == 0) {
                                i = 1
                                continue
                            }
                            break
                        }
                    }

                    if (pointsList.size >= 2) {
                        // Parse color
                        val red = if (table.get("red").isnumber()) table.get("red").toint() else 0
                        val green = if (table.get("green").isnumber()) table.get("green").toint() else 0
                        val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 0
                        val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 0

                        // Parse line width
                        val lineWidth = if (table.get("line_width").isnumber()) table.get("line_width").tofloat() else 1.0f

                        // Parse through walls
                        val throughWalls = if (table.get("through_walls").isboolean()) table.get("through_walls").toboolean() else true

                        val colorComponents = floatArrayOf(
                            red.toFloat() / 255.0f,
                            green.toFloat() / 255.0f,
                            blue.toFloat() / 255.0f,
                        )

                        val alphaComponent = alpha.toFloat() / 255.0f

                        // Call the render method
                        context.submitLinesFromPoints(
                            pointsList.toTypedArray(),
                            colorComponents,
                            alphaComponent,
                            lineWidth,
                            throughWalls
                        )
                        return TRUE
                    }
                }
            }
            return NIL
        }
    }

    private inner class RenderLineFromCursorFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val x: Double = if (table.get("x").isnumber()) table.get("x").todouble() else 0.0
                val y: Double = if (table.get("y").isnumber()) table.get("y").todouble() else 0.0
                val z: Double = if (table.get("z").isnumber()) table.get("z").todouble() else 0.0

                val red = if (table.get("red").isnumber()) table.get("red").toint() else 0
                val green = if (table.get("green").isnumber()) table.get("green").toint() else 0
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 0
                val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 0

                val lineWidth = if (table.get("line_width").isnumber()) table.get("line_width").tofloat() else 1.0f

                val colorComponents = floatArrayOf(
                    red.toFloat() / 255.0f,
                    green.toFloat() / 255.0f,
                    blue.toFloat() / 255.0f,
                )
                val alphah: Float =  alpha.toFloat() / 255.0f

                val pos = Vec3d(x, y, z)

                context.submitLineFromCursor(
                    pos,
                    colorComponents,
                    alphah,
                    lineWidth,
                );
                return TRUE
            }
            return NIL
        }
    }

    private inner class RenderImageFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val path = if (table.get("path").isstring()) table.get("path").tojstring() else return NIL

                val x: Double = if (table.get("x").isnumber()) table.get("x").todouble() else 0.0
                val y: Double = if (table.get("y").isnumber()) table.get("y").todouble() else 0.0
                val z: Double = if (table.get("z").isnumber()) table.get("z").todouble() else 0.0

                val ox: Double = if (table.get("offset_x").isnumber()) table.get("offset_x").todouble() else 0.0
                val oy: Double = if (table.get("offset_y").isnumber()) table.get("offset_y").todouble() else 0.0
                val oz: Double = if (table.get("offset_z").isnumber()) table.get("offset_z").todouble() else 0.0

                val width: Float = if (table.get("width").isnumber()) table.get("width").tofloat() else 0f
                val height: Float = if (table.get("height").isnumber()) table.get("height").tofloat() else 0f

                val regionWidth: Float = if (table.get("region_width").isnumber()) table.get("region_width").tofloat() else 1f
                val regionHeight: Float = if (table.get("region_height").isnumber()) table.get("region_height").tofloat() else 1f

                val r: Float = (if (table.get("red").isnumber()) table.get("red").todouble() else 255.0).toFloat() / 255f
                val g: Float = (if (table.get("green").isnumber()) table.get("green").todouble() else 255.0).toFloat() / 255f
                val b: Float = (if (table.get("blue").isnumber()) table.get("blue").todouble() else 255.0).toFloat() / 255f

                val alpha: Float = (if (table.get("alpha").isnumber()) table.get("alpha").todouble() else 255.0).toFloat() / 255f

                val throughWalls = if (table.get("through_walls").isboolean()) table.get("through_walls").toboolean() else true

                val rgb: FloatArray = floatArrayOf(r, g, b)

                try {
                    val identifier = loadTexture(path)
                    if (identifier != null) {
                        context.submitTexturedQuad(Vec3d(x, y, z), width, height, regionWidth, regionHeight, Vec3d(
                            ox, oy, oz
                        ), identifier, rgb, alpha, throughWalls)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return NIL
        }
    }

    /**
     * Загружает текстуру из файла и возвращает её Identifier
     */
    private fun loadTexture(path: String): Identifier? {
        val scriptCacheId = "wd_global"

        // Проверяем кэш для текущего скрипта
        val scriptCache = TwoRenderObject.Companion.textureCache.getOrPut(scriptCacheId) { ConcurrentHashMap() }
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
                val textureName = "hypixelcry:texture_${scriptCacheId}_${TwoRenderObject.Companion.textureCounter.getAndIncrement()}"
                val texture = NativeImageBackedTexture(
                    Supplier { textureName },
                    nativeImage
                )

                // Создаем идентификатор
                val identifier = Identifier.of("hypixelcry", "texture_${scriptCacheId}_${TwoRenderObject.Companion.textureCounter.get()}")
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

    override fun typename(): String = "world_renderer"
    override fun tojstring(): String = "WorldRenderObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return LuaValue.TUSERDATA
    }
}
