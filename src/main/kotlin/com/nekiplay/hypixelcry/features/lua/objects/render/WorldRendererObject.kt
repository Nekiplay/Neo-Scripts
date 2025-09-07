package com.nekiplay.hypixelcry.features.lua.objects.render

import com.nekiplay.hypixelcry.utils.render.RenderHelper
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction

class WorldRendererObject(private val context: WorldRenderContext?): LuaValue() {
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
            else -> NIL
        } as LuaValue
    }

    private inner class RenderFilledFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val x = if (table.get("x").isnumber()) table.get("x").toint() else 0
                val y = if (table.get("y").isnumber()) table.get("y").toint() else 0
                val z = if (table.get("z").isnumber()) table.get("z").toint() else 0

                val red = if (table.get("red").isnumber()) table.get("red").toint() else 0
                val green = if (table.get("green").isnumber()) table.get("green").toint() else 0
                val blue = if (table.get("blue").isnumber()) table.get("blue").toint() else 0
                val alpha = if (table.get("alpha").isnumber()) table.get("alpha").toint() else 0

                val throughWalls = if (table.get("through_walls").isboolean()) table.get("through_walls").toboolean() else true

                val colorComponents = floatArrayOf(
                    red.toFloat() / 255.0f,
                    green.toFloat() / 255.0f,
                    blue.toFloat() / 255.0f
                )

                val alphaComponent = alpha.toFloat() / 255.0f

                RenderHelper.renderFilled(context, BlockPos(x, y, z), colorComponents, alphaComponent, throughWalls)
                return TRUE
            }
            return NIL
        }
    }

    private inner class RenderOutlineFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val x = if (table.get("x").isnumber()) table.get("x").toint() else 0
                val y = if (table.get("y").isnumber()) table.get("y").toint() else 0
                val z = if (table.get("z").isnumber()) table.get("z").toint() else 0

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


                RenderHelper.renderOutline(context, BlockPos(x, y, z), colorComponents, lineWidth, throughWalls)
                return TRUE
            }
            return NIL
        }
    }

    private inner class RenderTextFunction : OneArgFunction() {
        override fun call(table: LuaValue): LuaValue {
            if (table.istable() && context != null) {
                val x: Double = if (table.get("x").isnumber()) table.get("x").todouble() else 0.0
                val y: Double = if (table.get("y").isnumber()) table.get("y").todouble() else 0.0
                val z: Double = if (table.get("z").isnumber()) table.get("z").todouble() else 0.0

                val text = if (table.get("text").isstring()) table.get("text").tojstring() else ""
                val scale = if (table.get("scale").isnumber()) table.get("scale").tofloat() else 1f

                val color = if (table.get("color").isnumber()) table.get("color").toint() else 0

                val throughWalls = if (table.get("through_walls").isboolean()) table.get("through_walls").toboolean() else true
                val pos = Vec3d(x, y, z)

                RenderHelper.renderText(context,
                    Text.of(text).asOrderedText(),
                    pos,
                    color,
                    scale,
                    0f,
                    throughWalls
                );
                return TRUE
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
                        RenderHelper.renderLinesFromPoints(
                            context,
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

                RenderHelper.renderLineFromCursor(
                    context,
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

    override fun typename(): String = "world_renderer"
    override fun tojstring(): String = "WorldRenderObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return LuaValue.TUSERDATA
    }
}
