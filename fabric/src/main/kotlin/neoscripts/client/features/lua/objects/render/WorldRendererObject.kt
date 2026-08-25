package com.nekiplay.neoscripts.client.features.lua.objects.render

import com.mojang.blaze3d.platform.NativeImage
import com.nekiplay.neoscripts.ClientMain.mc
import com.nekiplay.neoscripts.client.utils.render.primitive.PrimitiveCollector
import com.nekiplay.neoscripts.client.sugar.isBlock
import com.nekiplay.neoscripts.client.sugar.isBlockPos
import com.nekiplay.neoscripts.client.sugar.isBox
import com.nekiplay.neoscripts.client.sugar.isVector
import com.nekiplay.neoscripts.client.sugar.toBlock
import com.nekiplay.neoscripts.client.sugar.toBlockPos
import com.nekiplay.neoscripts.client.sugar.toBox
import com.nekiplay.neoscripts.client.sugar.toVector
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaBox
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.CommonColors
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import java.io.File
import java.io.FileInputStream
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier

class WorldRendererObject(private val context: PrimitiveCollector?): LuaValue() {
    companion object {
        private const val GLYPH_CACHE_MAX = 256

        private val glyphCache = object : LinkedHashMap<String, Font.PreparedText>(GLYPH_CACHE_MAX, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Font.PreparedText>): Boolean {
                return size > GLYPH_CACHE_MAX
            }
        }

        fun clearGlyphCache() {
            synchronized(glyphCache) {
                glyphCache.clear()
            }
        }
    }

    private fun getOrPrepareGlyphs(text: String, color: Int): Font.PreparedText {
        val key = "$text|$color"

        synchronized(glyphCache) {
            glyphCache[key]?.let { return it }
        }

        val font = mc.font
        val formatted = Component.literal(text).getVisualOrderText()
        val xOffset = -font.width(formatted) / 2f
        val glyphs = font.prepareText(formatted, xOffset, 0.5f, color, false, false, 0)

        synchronized(glyphCache) {
            glyphCache[key] = glyphs
        }
        return glyphs
    }

    override fun call(): LuaValue {
        return this
    }

    private val functions: Map<LuaValue, LuaValue> by lazy {
        hashMapOf(
            LuaValue.valueOf("renderFilled") to RenderFilledFunction(),
            LuaValue.valueOf("renderFilledCircle") to RenderFilledCircleFunction(),
            LuaValue.valueOf("renderOutline") to RenderOutlineFunction(),
            LuaValue.valueOf("renderOutlineCircle") to RenderOutlineCircleFunction(),
            LuaValue.valueOf("renderCylinder") to RenderCylinderFunction(),
            LuaValue.valueOf("renderSphere") to RenderSphereFunction(),
            LuaValue.valueOf("renderText") to RenderTextFunction(),
            LuaValue.valueOf("renderLinesFromPoints") to RenderLinesFromPointsFunction(),
            LuaValue.valueOf("renderLineFromCursor") to RenderLineFromCursorFunction(),
            LuaValue.valueOf("renderImage") to RenderImageFunction(),
            LuaValue.valueOf("renderQuad") to SubmitQuadFunction(),
            LuaValue.valueOf("renderHologramBlock") to RenderHologramBlockFunction()
        )
    }

    private fun parseBlockPos(arg1: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): BlockPos? {
        return when {
            arg1 != null && arg1.isBlockPos() -> arg1.toBlockPos()
            arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true -> {
                BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
            }
            else -> null
        }
    }

    private fun parseBlockPosWithBlockState(
        arg1: LuaValue?,
        arg2: LuaValue?,
        arg3: LuaValue?,
        arg4: LuaValue?
    ): Pair<BlockPos, BlockState>? {
        return when {
            arg1 != null && arg1.isBlockPos() -> {
                val pos = arg1.toBlockPos()
                val state = parseBlockState(arg2)
                if (pos != null && state != null) pos to state else null
            }
            arg1?.isnumber() == true && arg2?.isnumber() == true && arg3?.isnumber() == true -> {
                val pos = BlockPos(arg1.toint(), arg2.toint(), arg3.toint())
                val state = parseBlockState(arg4)
                if (state != null) pos to state else null
            }
            else -> null
        }
    }

    private fun parseBlockState(arg: LuaValue?): BlockState? {
        return when {
            arg != null && arg.isBlock() -> arg.toBlock()
            arg?.isnumber() == true -> {
                Block.stateById(arg.toint())
            }
            else -> null
        }
    }

    private fun parseVec3(args: Varargs): Pair<Vec3, Int>? {
        return when {
            args.arg(1).isVector() -> args.arg(1).toVector()?.let { it to 2 }
            args.arg(1).isnumber() && args.arg(2).isnumber() && args.arg(3).isnumber() -> {
                val vec = Vec3(args.arg(1).todouble(), args.arg(2).todouble(), args.arg(3).todouble())
                vec to 4
            }
            else -> null
        }
    }

    override fun get(key: LuaValue): LuaValue {
        if (key.type() != TSTRING) return NIL
        return functions[key] ?: NIL
    }

    private inner class RenderSphereFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val (pos, offset) = parseVec3(args) ?: return NIL
            val radius = args.optdouble(offset, 1.0)
            val segments = args.optint(offset + 1, 1)
            val rings = args.optint(offset + 2, 4)
            val red = args.optint(offset + 3, 0)
            val green = args.optint(offset + 4, 0)
            val blue = args.optint(offset + 5, 0)
            val alpha = args.optint(offset + 6, 0)
            val throughWalls = args.optboolean(offset + 7, true)

            context.submitSphere(pos, radius.toFloat(), segments, rings, getArgb(alpha, red, green, blue), throughWalls)
            return TRUE
        }
    }

    private inner class RenderCylinderFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val (pos, offset) = parseVec3(args) ?: return NIL
            val radius = args.optdouble(offset, 1.0)
            val height = args.optdouble(offset + 1, 1.0)
            val segments = args.optint(offset + 2, 1)
            val red = args.optint(offset + 3, 0)
            val green = args.optint(offset + 4, 0)
            val blue = args.optint(offset + 5, 0)
            val alpha = args.optint(offset + 6, 0)
            val throughWalls = args.optboolean(offset + 7, true)

            context.submitCylinder(pos, radius.toFloat(), height.toFloat(), segments, getArgb(alpha, red, green, blue), throughWalls)
            return TRUE
        }
    }

    private inner class RenderHologramBlockFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val (blockPos, blockState) = parseBlockPosWithBlockState(args.arg(1), args.arg(2), args.arg(3), args.arg(4))
                ?: return NIL

            val alpha = args.arg(5).checkdouble()
            context.submitBlockHologram(blockPos, blockState, alpha.toFloat())
            return TRUE
        }
    }

    private inner class SubmitQuadFunction : VarArgFunction() {
        private fun parsePoint(arg: LuaValue): Vec3? {
            return when {
                arg.isVector() -> arg.toVector()
                arg.istable() -> {
                    val x = arg.get("x").optdouble(0.0)
                    val y = arg.get("y").optdouble(0.0)
                    val z = arg.get("z").optdouble(0.0)
                    Vec3(x, y, z)
                }
                else -> null
            }
        }

        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val pointsTable = args.arg1()
            if (!pointsTable.istable()) return NIL

            val pointsList = mutableListOf<Vec3>()
            var i = 1
            while (true) {
                val point = pointsTable.get(i)
                if (point.istable() || point.isuserdata()) {
                    val vec = parsePoint(point)
                    if (vec != null) {
                        pointsList.add(vec)
                    }
                    i++
                } else {
                    break
                }
            }

            if (pointsList.size < 3) return NIL

            val points = pointsList.take(4).toTypedArray()

            val red = args.optint(2, 0)
            val green = args.optint(3, 0)
            val blue = args.optint(4, 0)
            val alpha = args.optint(5, 0)
            val throughWalls = args.optboolean(6, false)

            val colorComponents = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f
            )
            val alphaComponent = alpha / 255.0f

            context.submitQuad(points, colorComponents, alphaComponent, throughWalls)
            return TRUE
        }
    }

    private inner class RenderFilledCircleFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val (pos, offset) = parseVec3(args) ?: return NIL
            val radius = args.optdouble(offset, 1.0)
            val segments = args.optint(offset + 1, 8)
            val red = args.optint(offset + 2, 0)
            val green = args.optint(offset + 3, 0)
            val blue = args.optint(offset + 4, 0)
            val alpha = args.optint(offset + 5, 0)
            val throughWalls = args.optboolean(offset + 6, true)

            context.submitFilledCircle(pos, radius.toFloat(), segments, getArgb(alpha, red, green, blue), throughWalls)
            return TRUE
        }
    }

    private inner class RenderOutlineCircleFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val (pos, offset) = parseVec3(args) ?: return NIL
            val radius = args.optdouble(offset, 1.0)
            val segments = args.optint(offset + 1, 8)
            val thickness = args.optdouble(offset + 2, 1.0)
            val red = args.optint(offset + 3, 0)
            val green = args.optint(offset + 4, 0)
            val blue = args.optint(offset + 5, 0)
            val alpha = args.optint(offset + 6, 0)
            val throughWalls = args.optboolean(offset + 7, true)

            context.submitOutlinedCircle(pos, radius.toFloat(), thickness.toFloat(), segments, getArgb(alpha, red, green, blue), throughWalls)
            return TRUE
        }
    }

    private inner class RenderFilledFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val boxArg = args.arg(1)
            val box = if (boxArg.isBox()) boxArg.toBox() else null
            if (box == null) return NIL

            val red = args.optint(2, 0)
            val green = args.optint(3, 0)
            val blue = args.optint(4, 0)
            val alpha = args.optint(5, 0)
            val throughWalls = args.optboolean(6, true)

            val colorComponents = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f
            )
            val alphaComponent = alpha.toFloat() / 255.0f

            context.submitFilledBox(box, colorComponents, alphaComponent, throughWalls)
            return TRUE
        }
    }

    private inner class RenderOutlineFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val boxArg = args.arg(1)
            val box = if (boxArg.isBox()) boxArg.toBox() else null
            if (box == null) return NIL

            val red = args.optint(2, 0)
            val green = args.optint(3, 0)
            val blue = args.optint(4, 0)
            val alpha = args.optint(5, 0)
            val lineWidth = args.optdouble(6, 1.0).toFloat()
            val throughWalls = args.optboolean(7, true)

            val colorComponents = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f,
                alpha.toFloat() / 255.0f
            )

            context.submitOutlinedBox(box, colorComponents, lineWidth, throughWalls)
            return TRUE
        }
    }

    fun getArgb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private inner class RenderTextFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val (pos, offset) = parseVec3(args) ?: return NIL

            val text = args.optjstring(offset, "Empty")
            val scale = args.optdouble(offset + 1, 1.0).toFloat()

            val red = args.optint(offset + 2, -1)
            val green = args.optint(offset + 3, -1)
            val blue = args.optint(offset + 4, -1)

            val color: Int = if (red in 0..255 && green in 0..255 && blue in 0..255) {
                (255 shl 24) or (red shl 16) or (green shl 8) or blue
            } else {
                CommonColors.WHITE
            }

            val throughWalls = args.optboolean(offset + 5, true)

            val glyphs = getOrPrepareGlyphs(text, color)

            context.submitTextPrepared(
                glyphs,
                pos,
                scale,
                0.5f,
                throughWalls
            )
            return TRUE
        }
    }

    private inner class RenderLinesFromPointsFunction : VarArgFunction() {
        private fun parsePoint(arg: LuaValue): Vec3? {
            return when {
                arg.isVector() -> arg.toVector()
                arg.istable() -> {
                    val x = arg.get("x").optdouble(0.0)
                    val y = arg.get("y").optdouble(0.0)
                    val z = arg.get("z").optdouble(0.0)
                    Vec3(x, y, z)
                }
                else -> null
            }
        }

        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val pointsTable = args.arg1()
            if (!pointsTable.istable()) return NIL

            val pointsList = mutableListOf<Vec3>()
            var i = 1
            while (true) {
                val point = pointsTable.get(i)
                if (point.istable() || point.isuserdata()) {
                    val vec = parsePoint(point)
                    if (vec != null) {
                        pointsList.add(vec)
                    }
                    i++
                } else {
                    break
                }
            }

            if (pointsList.size < 2) return NIL

            val red = args.optint(2, 0)
            val green = args.optint(3, 0)
            val blue = args.optint(4, 0)
            val alpha = args.optint(5, 0)
            val lineWidth = args.optdouble(6, 1.0).toFloat()
            val throughWalls = args.optboolean(7, true)

            val colorComponents = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f,
            )
            val alphaComponent = alpha.toFloat() / 255.0f

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

    private inner class RenderLineFromCursorFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val (pos, offset) = parseVec3(args) ?: return NIL
            val red = args.optint(offset, 0)
            val green = args.optint(offset + 1, 0)
            val blue = args.optint(offset + 2, 0)
            val alpha = args.optint(offset + 3, 0)
            val lineWidth = args.optdouble(offset + 4, 1.0).toFloat()

            val colorComponents = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f,
            )
            val alphaComponent = alpha.toFloat() / 255.0f

            context.submitLineFromCursor(pos, colorComponents, alphaComponent, lineWidth)
            return TRUE
        }
    }

    private inner class RenderImageFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val path = args.optjstring(1, "") ?: return NIL
            if (path.isEmpty()) return NIL

            val directVec = if (args.arg(1).isVector()) args.arg(1).toVector() else null
            val (pos, offset) = when {
                directVec != null -> directVec to 2
                args.arg(2).isnumber() && args.arg(3).isnumber() && args.arg(4).isnumber() -> {
                    val vec = Vec3(args.arg(2).todouble(), args.arg(3).todouble(), args.arg(4).todouble())
                    vec to 5
                }
                else -> return NIL
            }

            val width = args.optdouble(offset, 0.0).toFloat()
            val height = args.optdouble(offset + 1, 0.0).toFloat()
            val regionWidth = args.optdouble(offset + 2, 1.0).toFloat()
            val regionHeight = args.optdouble(offset + 3, 1.0).toFloat()
            val ox = args.optdouble(offset + 4, 0.0)
            val oy = args.optdouble(offset + 5, 0.0)
            val oz = args.optdouble(offset + 6, 0.0)
            val red = args.optint(offset + 7, 255)
            val green = args.optint(offset + 8, 255)
            val blue = args.optint(offset + 9, 255)
            val alpha = args.optint(offset + 10, 255)
            val throughWalls = args.optboolean(offset + 11, true)

            val rgb = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f
            )
            val alphaFloat = alpha.toFloat() / 255.0f

            try {
                val identifier = loadTexture(path)
                if (identifier != null) {
                    context.submitTexturedQuad(pos, width, height, regionWidth, regionHeight, Vec3(ox, oy, oz), identifier, rgb, alphaFloat, throughWalls)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return TRUE
        }
    }

    private fun loadTexture(path: String): Identifier? {
        val scriptCacheId = "wd_global"

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

                val textureName = "neoscripts:texture_${scriptCacheId}_${TwoRenderObject.Companion.textureCounter.getAndIncrement()}"
                val texture = DynamicTexture(
                    Supplier { textureName },
                    nativeImage
                )

                val identifier = Identifier.fromNamespaceAndPath("neoscripts", "texture_${scriptCacheId}_${TwoRenderObject.Companion.textureCounter.get()}")
                mc.textureManager.register(identifier, texture)

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
        return TUSERDATA
    }
}
