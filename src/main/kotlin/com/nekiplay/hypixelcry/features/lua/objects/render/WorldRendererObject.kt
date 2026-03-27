package com.nekiplay.hypixelcry.features.lua.objects.render

import com.logisticscraft.occlusionculling.util.Vec3d
import com.mojang.blaze3d.platform.NativeImage
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.utils.render.primitive.PrimitiveCollector
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import net.minecraft.resources.Identifier
import net.minecraft.util.CommonColors
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
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
            "renderFilledCircle" -> RenderFilledCircleFunction()
            "renderOutline" -> RenderOutlineFunction()
            "renderOutlineCircle" -> RenderOutlineCircleFunction()
            "renderCylinder" -> RenderCylinderFunction()
            "renderSphere" -> RenderSphereFunction()
            "renderText" -> RenderTextFunction()
            "renderLinesFromPoints" -> RenderLinesFromPointsFunction()
            "renderLineFromCursor" -> RenderLineFromCursorFunction()
            "renderImage" -> RenderImageFunction()
            "renderBeaconBeam" -> RenderBeaconBeamFunction()
            "renderQuad" -> SubmitQuadFunction()
            "renderHologramBlock" -> RenderHologramBlockFunction()
            "renderBlock" -> RenderBlockFunction()
            "renderItem" -> RenderItemFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class RenderSphereFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optdouble(1, 0.0)
            val y = args.optdouble(2, 0.0)
            val z = args.optdouble(3, 0.0)
            val radius = args.optdouble(4, 1.0)
            val segments = args.optint(5, 1)
            val rings = args.optint(6, 4)
            val red = args.optint(7, 0)
            val green = args.optint(8, 0)
            val blue = args.optint(9, 0)
            val alpha = args.optint(10, 0)
            val throughWalls = args.optboolean(11, true)

            context.submitSphere(Vec3(x, y, z), radius.toFloat(), segments, rings, getArgb(alpha, red, green, blue), throughWalls)
            return TRUE
        }
    }

    private inner class RenderCylinderFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optdouble(1, 0.0)
            val y = args.optdouble(2, 0.0)
            val z = args.optdouble(3, 0.0)
            val radius = args.optdouble(4, 1.0)
            val height = args.optdouble(5, 1.0)
            val segments = args.optint(6, 1)
            val red = args.optint(7, 0)
            val green = args.optint(8, 0)
            val blue = args.optint(9, 0)
            val alpha = args.optint(10, 0)
            val throughWalls = args.optboolean(11, true)

            context.submitCylinder(Vec3(x, y, z), radius.toFloat(), height.toFloat(), segments, getArgb(alpha, red, green, blue), throughWalls)
            return TRUE
        }
    }

    private inner class RenderItemFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optdouble(1, 0.0)
            val y = args.optdouble(2, 0.0)
            val z = args.optdouble(3, 0.0)
            val id = args.optjstring(4, "minecraft:stone")

            val identifier = Identifier.bySeparator(id, ':')
            context.submitItem(Vec3d(x, y, z), identifier)
            return TRUE
        }
    }

    private inner class RenderBlockFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optint(1, 0)
            val y = args.optint(2, 0)
            val z = args.optint(3, 0)
            val id = args.optint(4, 1)

            val blockState = Block.stateById(id)
            context.submitBlock(BlockPos(x, y, z), blockState)
            return TRUE
        }
    }


    private inner class RenderHologramBlockFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optint(1, 0)
            val y = args.optint(2, 0)
            val z = args.optint(3, 0)
            val id = args.optint(4, 1)

            val blockState = Block.stateById(id)
            context.submitBlockHologram(BlockPos(x, y, z), blockState)
            return TRUE
        }
    }
                val z = if (table.get("z").isnumber()) table.get("z").toint() else 0
                val id = if (table.get("id").isnumber()) table.get("id").toint() else 1

                val blockState = Block.stateById(id)

                context.submitBlockHologram(BlockPos(x, y, z), blockState)
                return TRUE
            }
            return NIL
        }
    }

    private inner class SubmitQuadFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val table = args.arg1()
            if (!table.istable()) return NIL

            val pointsArray = table.get("points")
            val points = if (pointsArray.istable() && pointsArray.length() == 4) {
                Array(4) { index ->
                    val point = pointsArray.get(index + 1)
                    Vec3(
                        point.get("x").optdouble(0.0),
                        point.get("y").optdouble(0.0),
                        point.get("z").optdouble(0.0)
                    )
                }
            } else {
                Array(4) { index ->
                    val pointTable = table.get("point${index + 1}")
                    Vec3(
                        pointTable.get("x").optdouble(0.0),
                        pointTable.get("y").optdouble(0.0),
                        pointTable.get("z").optdouble(0.0)
                    )
                }
            }

            val red = table.get("red").optint(0)
            val green = table.get("green").optint(0)
            val blue = table.get("blue").optint(0)
            val alpha = table.get("alpha").optint(0)

            val colorComponents = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f
            )
            val alphaComponent = alpha / 255.0f

            val throughWalls = table.get("throughWalls").optboolean(false)

            context.submitQuad(points, colorComponents, alphaComponent, throughWalls)
            return TRUE
        }
    }

    private inner class RenderBeaconBeamFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optint(1, 0)
            val y = args.optint(2, 0)
            val z = args.optint(3, 0)
            val red = args.optint(4, 0)
            val green = args.optint(5, 0)
            val blue = args.optint(6, 0)

            val colorComponents = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f
            )

            context.submitBeaconBeam(BlockPos(x, y, z), colorComponents)
            return TRUE
        }
    }

    private inner class RenderFilledCircleFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optdouble(1, 0.0)
            val y = args.optdouble(2, 0.0)
            val z = args.optdouble(3, 0.0)
            val radius = args.optdouble(4, 1.0)
            val segments = args.optint(5, 8)
            val red = args.optint(6, 0)
            val green = args.optint(7, 0)
            val blue = args.optint(8, 0)
            val alpha = args.optint(9, 0)
            val throughWalls = args.optboolean(10, true)

            context.submitFilledCircle(Vec3(x, y, z), radius.toFloat(), segments, getArgb(alpha, red, green, blue), throughWalls)
            return TRUE
        }
    }

    private inner class RenderOutlineCircleFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optdouble(1, 0.0)
            val y = args.optdouble(2, 0.0)
            val z = args.optdouble(3, 0.0)
            val radius = args.optdouble(4, 1.0)
            val segments = args.optint(5, 8)
            val thickness = args.optdouble(6, 1.0)
            val red = args.optint(7, 0)
            val green = args.optint(8, 0)
            val blue = args.optint(9, 0)
            val alpha = args.optint(10, 0)
            val throughWalls = args.optboolean(11, true)

            context.submitOutlinedCircle(Vec3(x, y, z), radius.toFloat(), thickness.toFloat(), segments, getArgb(alpha, red, green, blue), throughWalls)
            return TRUE
        }
    }

    private inner class RenderFilledFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optdouble(1, 0.0)
            val y = args.optdouble(2, 0.0)
            val z = args.optdouble(3, 0.0)
            val x2 = args.optdouble(4, 0.0)
            val y2 = args.optdouble(5, 0.0)
            val z2 = args.optdouble(6, 0.0)
            val red = args.optint(7, 0)
            val green = args.optint(8, 0)
            val blue = args.optint(9, 0)
            val alpha = args.optint(10, 0)
            val throughWalls = args.optboolean(11, true)

            val colorComponents = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f
            )
            val alphaComponent = alpha.toFloat() / 255.0f

            val hasSecondPosition = args.narg() >= 6 && (x2 != 0.0 || y2 != 0.0 || z2 != 0.0)
            if (hasSecondPosition) {
                context.submitFilledBox(AABB(Vec3(x, y, z), Vec3(x2, y2, z2)), colorComponents, alphaComponent, throughWalls)
            } else {
                context.submitFilledBox(BlockPos(x.toInt(), y.toInt(), z.toInt()), colorComponents, alphaComponent, throughWalls)
            }
            return TRUE
        }
    }

    private inner class RenderOutlineFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optdouble(1, 0.0)
            val y = args.optdouble(2, 0.0)
            val z = args.optdouble(3, 0.0)
            val x2 = args.optdouble(4, 0.0)
            val y2 = args.optdouble(5, 0.0)
            val z2 = args.optdouble(6, 0.0)
            val red = args.optint(7, 0)
            val green = args.optint(8, 0)
            val blue = args.optint(9, 0)
            val alpha = args.optint(10, 0)
            val lineWidth = args.optdouble(11, 1.0).toFloat()
            val throughWalls = args.optboolean(12, true)

            val colorComponents = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f,
                alpha.toFloat() / 255.0f
            )

            val hasSecondPosition = args.narg() >= 6 && (x2 != 0.0 || y2 != 0.0 || z2 != 0.0)
            if (hasSecondPosition) {
                context.submitOutlinedBox(AABB(Vec3(x, y, z), Vec3(x2, y2, z2)), colorComponents, lineWidth, throughWalls)
            } else {
                context.submitOutlinedBox(BlockPos(x.toInt(), y.toInt(), z.toInt()), colorComponents, lineWidth, throughWalls)
            }
            return TRUE
        }
    }
    }

    fun getArgb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private inner class RenderTextFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val x = args.optdouble(1, 0.0)
            val y = args.optdouble(2, 0.0)
            val z = args.optdouble(3, 0.0)
            val pos = Vec3(x, y, z)

            val text = args.optjstring(4, "Empty")
            val scale = args.optdouble(5, 1.0).toFloat()
            val throughWalls = args.optboolean(6, true)

            val red = args.optint(7, -1)
            val green = args.optint(8, -1)
            val blue = args.optint(9, -1)

            val color: Int = if (red in 0..255 && green in 0..255 && blue in 0..255) {
                (255 shl 24) or (red shl 16) or (green shl 8) or blue
            } else {
                CommonColors.WHITE
            }

            val qx = args.optdouble(10, 0.0)
            val qy = args.optdouble(11, 0.0)
            val qz = args.optdouble(12, 0.0)
            val qw = args.optdouble(13, 0.0)

            val hasRotation = (qx != 0.0 || qy != 0.0 || qz != 0.0 || qw != 0.0)
            val component = Component.literal(text)

            if (hasRotation) {
                val quaternion = Quaternionf(qx, qy, qz, qw)
                context.submitText(
                    component,
                    pos,
                    color,
                    scale,
                    0.5f,
                    quaternion,
                    throughWalls
                )
            } else {
                context.submitText(
                    component,
                    pos,
                    color,
                    scale,
                    0.5f,
                    throughWalls
                )
            }
            return TRUE
        }
    }
            }

            return TRUE
        }
    }

    private inner class RenderLinesFromPointsFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val table = args.arg1()
            if (!table.istable()) return NIL

            val pointsTable = table.get("points")
            if (!pointsTable.istable()) return NIL

            val pointsList = mutableListOf<Vec3>()
            var i = 0
            while (true) {
                val pointTable = pointsTable.get(i)
                if (pointTable.istable()) {
                    val x = pointTable.get("x").optdouble(0.0)
                    val y = pointTable.get("y").optdouble(0.0)
                    val z = pointTable.get("z").optdouble(0.0)
                    pointsList.add(Vec3(x, y, z))
                    i++
                } else {
                    if (i == 0) {
                        i = 1
                        continue
                    }
                    break
                }
            }

            if (pointsList.size < 2) return NIL

            val red = table.get("red").optint(0)
            val green = table.get("green").optint(0)
            val blue = table.get("blue").optint(0)
            val alpha = table.get("alpha").optint(0)
            val lineWidth = table.get("line_width").optdouble(1.0).toFloat()
            val throughWalls = table.get("through_walls").optboolean(true)

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

            val x = args.optdouble(1, 0.0)
            val y = args.optdouble(2, 0.0)
            val z = args.optdouble(3, 0.0)
            val red = args.optint(4, 0)
            val green = args.optint(5, 0)
            val blue = args.optint(6, 0)
            val alpha = args.optint(7, 0)
            val lineWidth = args.optdouble(8, 1.0).toFloat()

            val colorComponents = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f,
            )
            val alphaComponent = alpha.toFloat() / 255.0f

            val pos = Vec3(x, y, z)
            context.submitLineFromCursor(pos, colorComponents, alphaComponent, lineWidth)
            return TRUE
        }
    }

    private inner class RenderImageFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (context == null) return NIL

            val path = args.optjstring(1, "") ?: return NIL
            if (path.isEmpty()) return NIL

            val x = args.optdouble(2, 0.0)
            val y = args.optdouble(3, 0.0)
            val z = args.optdouble(4, 0.0)
            val width = args.optdouble(5, 0.0).toFloat()
            val height = args.optdouble(6, 0.0).toFloat()
            val regionWidth = args.optdouble(7, 1.0).toFloat()
            val regionHeight = args.optdouble(8, 1.0).toFloat()
            val ox = args.optdouble(9, 0.0)
            val oy = args.optdouble(10, 0.0)
            val oz = args.optdouble(11, 0.0)
            val red = args.optint(12, 255)
            val green = args.optint(13, 255)
            val blue = args.optint(14, 255)
            val alpha = args.optint(15, 255)
            val throughWalls = args.optboolean(16, true)

            val rgb = floatArrayOf(
                red.toFloat() / 255.0f,
                green.toFloat() / 255.0f,
                blue.toFloat() / 255.0f
            )
            val alphaFloat = alpha.toFloat() / 255.0f

            try {
                val identifier = loadTexture(path)
                if (identifier != null) {
                    context.submitTexturedQuad(Vec3(x, y, z), width, height, regionWidth, regionHeight, Vec3(ox, oy, oz), identifier, rgb, alphaFloat, throughWalls)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return TRUE
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
                val texture = DynamicTexture(
                    Supplier { textureName },
                    nativeImage
                )

                // Создаем идентификатор
                val identifier = Identifier.fromNamespaceAndPath("hypixelcry", "texture_${scriptCacheId}_${TwoRenderObject.Companion.textureCounter.get()}")
                mc.textureManager.register(identifier, texture)

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
