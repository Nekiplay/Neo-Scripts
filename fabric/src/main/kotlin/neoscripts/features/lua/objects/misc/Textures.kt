package com.nekiplay.neoscripts.features.lua.objects.misc

import com.mojang.blaze3d.opengl.GlTextureView
import com.mojang.blaze3d.platform.NativeImage
import com.nekiplay.neoscripts.features.lua.LuaScript
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.neoscripts.mixins.SpriteContentsAccessor
import com.nekiplay.neoscripts.mixins.renderer.ItemStackRenderStateAccessor
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.model.EntityModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.Block
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Supplier

class Textures(private val script: LuaScript? = null) : LuaValue() {

    init {
        instances.add(this)
    }

    override fun typename(): String = "textures"
    override fun tojstring(): String = "TexturesObject"
    override fun isnil(): Boolean = false
    override fun type(): Int = TUSERDATA

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "getFromItem", "getItemTexture", "item" -> GetFromItem()
            "getFromBlock", "getBlockTexture", "block" -> GetFromBlock()
            "getFromEntity", "getSkin", "entity" -> GetFromEntity()
            "clear", "clearCache", "cleanup" -> ClearCache()
            else -> super.get(key)
        }
    }

    private inner class GetFromItem : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val stack = arg.toItemStack() ?: return NIL
            val key = "item|" + stack.cacheKey()
            cache[key]?.let { return it }
            val handle = ItemTexture(this@Textures, key)
            cache[key] = handle

            try {
                val sprite = extractSprite(stack) ?: return handle
                val image = copyFirstFrame(sprite) ?: return handle
                Pipeline.registerTexture(handle, image)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return handle
        }
    }

    private inner class GetFromBlock : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val stack = arg.toBlockStack() ?: return NIL
            val key = "block|" + stack.cacheKey()
            cache[key]?.let { return it }
            val handle = ItemTexture(this@Textures, key)
            cache[key] = handle
            Pipeline.pendingJobs.add(Pipeline.CaptureJob(this@Textures, stack, handle))
            return handle
        }
    }

    private inner class GetFromEntity : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val entity = arg.toEntity() ?: return NIL
            val texId = resolveEntityTexture(entity) ?: return NIL
            val key = "skin|" + texId.toString()
            cache[key]?.let { return it }

            val handle = ItemTexture(this@Textures, key, owns = false)
            handle.identifier = texId
            cache[key] = handle
            handle.gpuId()
            return handle
        }
    }

    private inner class ClearCache : ZeroArgFunction() {
        override fun call(): LuaValue {
            cleanup()
            return NIL
        }
    }

    class ItemTexture internal constructor(
        private val owner: Textures,
        private val cacheKey: String?,
        private val owns: Boolean = true
    ) : LuaUserdata(AtomicInteger(0)) {

        internal var identifier: Identifier? = null

        @Volatile
        internal var released = false

        override fun get(key: LuaValue): LuaValue {
            return when (key.tojstring()) {
                "getId", "getGlId", "getTextureId" -> GetId()
                "isReady" -> IsReady()
                "release" -> Release()
                else -> super.get(key)
            }
        }

        private inner class GetId : ZeroArgFunction() {
            override fun call(): LuaValue = valueOf(gpuId())
        }

        private inner class IsReady : ZeroArgFunction() {
            override fun call(): LuaValue = valueOf(gpuId() > 0)
        }

        private inner class Release : ZeroArgFunction() {
            override fun call(): LuaValue {
                releaseTexture()
                return NIL
            }
        }

        fun gpuId(): Int {
            val current = (m_instance as AtomicInteger).get()
            if (current != 0 || owns || released) return current

            // Non-owning handle: texture may appear later (e.g. skin still downloading).
            val id = identifier ?: return 0
            return try {
                val view = Minecraft.getInstance().textureManager.getTexture(id)?.getTextureView() as? GlTextureView
                val glId = view?.texture()?.glId() ?: 0
                if (glId != 0) (m_instance as AtomicInteger).set(glId)
                glId
            } catch (e: Exception) {
                0
            }
        }

        fun releaseTexture() {
            try {
                if (owns) {
                    identifier?.let { id ->
                        Minecraft.getInstance().textureManager.release(id)
                    }
                }
            } catch (e: Exception) {
                println("Error releasing item texture: ${e.message}")
            } finally {
                released = true
                identifier = null
                (m_instance as AtomicInteger).set(0)
                cacheKey?.let { owner.cache.remove(it, this) }
            }
        }

        override fun typename(): String = "texture"
    }

    /** Resolves the skin texture identifier of an entity (player skins or living-entity textures). */
    private fun resolveEntityTexture(entity: Entity): Identifier? {
        return try {
            if (entity is AbstractClientPlayer) {
                return entity.skin.body().texturePath()
            }
            val mc = Minecraft.getInstance()
            val state = mc.entityRenderDispatcher.extractEntity(entity, 0f) as? LivingEntityRenderState
                ?: return null
            @Suppress("UNCHECKED_CAST")
            val renderer = mc.entityRenderDispatcher.getRenderer(state)
                as? LivingEntityRenderer<Entity, LivingEntityRenderState, EntityModel<in LivingEntityRenderState>>
            renderer?.getTextureLocation(state)
        } catch (e: Exception) {
            null
        }
    }

    /** Releases every GPU texture owned by this script instance. Called automatically on script unload. */
    fun cleanup() {        Pipeline.removeJobsOf(this)
        for (handle in cache.values.toList()) {
            handle.releaseTexture()
        }
        cache.clear()
        instances.remove(this)
    }

    private val cache = ConcurrentHashMap<String, ItemTexture>()

    companion object Pipeline {
        private val instances = CopyOnWriteArrayList<Textures>()
        private val textureCounter = AtomicLong()
        private val pendingJobs = ConcurrentLinkedQueue<CaptureJob>()

        @Volatile
        private var activeBatch: List<CaptureJob>? = null

        @Volatile
        private var captureInFlight = false

        @Volatile
        private var captureSide = 0

        internal class CaptureJob(
            val owner: Textures,
            val stack: ItemStack,
            val handle: ItemTexture
        )

        internal fun removeJobsOf(owner: Textures) {
            pendingJobs.removeIf { it.owner === owner }
        }

        /** Called from the HUD layer each frame before scripts render. */
        @JvmStatic
        fun onGuiExtract(graphics: GuiGraphicsExtractor) {
            if (captureInFlight || pendingJobs.isEmpty()) return
            val mc = Minecraft.getInstance()
            val target = mc.gameRenderer.mainRenderTarget() ?: return
            if (target.width < 32 || target.height < 32) return

            val ratio = target.width.toFloat() / graphics.guiWidth().coerceAtLeast(1).toFloat()
            val side = (16 * ratio).toInt().coerceAtMost(target.height)
            if (side < 1) return

            val maxRows = (target.height / side).coerceAtMost(pendingJobs.size)
            val batch = ArrayList<CaptureJob>(maxRows)
            repeat(maxRows) {
                val job = pendingJobs.poll() ?: return@repeat
                batch.add(job)
            }
            if (batch.isEmpty()) return

            for ((index, job) in batch.withIndex()) {
                val y = index * 16
                graphics.fill(0, y, 16, y + 16, -0x1000000)
                graphics.item(job.stack, 0, y)
                graphics.fill(16, y, 32, y + 16, -1)
                graphics.item(job.stack, 16, y)
            }

            activeBatch = batch
            captureSide = side
            captureInFlight = true
        }

        /** Called after a fully rendered frame. */
        @JvmStatic
        fun onFrameRendered() {
            if (!captureInFlight) return
            val batch = activeBatch ?: run {
                captureInFlight = false
                return
            }
            activeBatch = null
            val side = captureSide
            captureSide = 0
            if (side <= 0) {
                captureInFlight = false
                return
            }

            val target = Minecraft.getInstance().gameRenderer.mainRenderTarget()
            if (target == null) {
                captureInFlight = false
                return
            }

            try {
                Screenshot.takeScreenshot(target) { full ->
                    try {
                        for ((index, job) in batch.withIndex()) {
                            if (job.handle.released) continue
                            val y = index * side
                            if (y + side > full.height) break
                            val image = reconstructAlpha(full, y, side)
                            if (image != null) {
                                registerTexture(job, image)
                            }
                        }
                    } finally {
                        full.close()
                        captureInFlight = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                captureInFlight = false
            }
        }

        /**
         * The same icon is drawn over black at x=[0, side) and white at
         * x=[side, 2*side): observed = C*A + B*(1-A), so
         * A = 1 - (obsWhite - obsBlack)/255 and C = obsBlack / A.
         */
        private fun reconstructAlpha(full: NativeImage, y0: Int, side: Int): NativeImage? {
            return try {
                val out = NativeImage(NativeImage.Format.RGBA, side, side, false)
                for (y in 0 until side) {
                    for (x in 0 until side) {
                        val blackPx = full.getPixel(x, y0 + y)
                        val whitePx = full.getPixel(x + side, y0 + y)

                        val c0b = blackPx and 0xFF
                        val c1b = blackPx shr 8 and 0xFF
                        val c2b = blackPx shr 16 and 0xFF
                        val c0w = whitePx and 0xFF
                        val c1w = whitePx shr 8 and 0xFF
                        val c2w = whitePx shr 16 and 0xFF

                        var diff = ((c0w - c0b) + (c1w - c1b) + (c2w - c2b)) / 3.0
                        if (diff < 0) diff = 0.0
                        if (diff > 255) diff = 255.0
                        val alpha = 1.0 - diff / 255.0

                        val outPx: Int = if (alpha <= 0.004) {
                            0
                        } else {
                            val oc0 = clamp255(Math.round(c0b / alpha).toInt())
                            val oc1 = clamp255(Math.round(c1b / alpha).toInt())
                            val oc2 = clamp255(Math.round(c2b / alpha).toInt())
                            val oa = clamp255(Math.round(alpha * 255.0).toInt())
                            (oa shl 24) or (oc2 shl 16) or (oc1 shl 8) or oc0
                        }
                        out.setPixel(x, y, outPx)
                    }
                }
                out
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        internal fun registerTexture(job: CaptureJob, image: NativeImage) {
            registerTexture(job.handle, image)
        }

        private fun registerTexture(handle: ItemTexture, image: NativeImage) {
            try {
                if (handle.released) {
                    image.close()
                    return
                }
                val name = "item_texture_${textureCounter.incrementAndGet()}"
                val identifier = Identifier.fromNamespaceAndPath("neoscripts", name)
                val manager = Minecraft.getInstance().textureManager
                manager.register(identifier, DynamicTexture(Supplier { name }, image))

                val view = manager.getTexture(identifier)?.getTextureView() as? GlTextureView
                val glId = view?.texture()?.glId() ?: 0
                handle.identifier = identifier
                (handle.m_instance as AtomicInteger).set(glId)
            } catch (e: Exception) {
                e.printStackTrace()
                image.close()
            }
        }

        private fun extractSprite(stack: ItemStack): TextureAtlasSprite? {
            val mc = Minecraft.getInstance()
            return try {
                val state = ItemStackRenderState()
                mc.itemModelResolver.updateForTopItem(
                    state, stack, ItemDisplayContext.GUI, mc.level, null, 0
                )
                val accessor = state as ItemStackRenderStateAccessor
                var sprite: TextureAtlasSprite? = null
                val layers = accessor.layers
                val count = accessor.activeLayerCount
                for (i in 0 until count) {
                    for (quad in layers[i].prepareQuadList()) {
                        sprite = quad.materialInfo()?.sprite()
                        if (sprite != null) break
                    }
                    if (sprite != null) break
                }
                sprite ?: state.pickParticleMaterial(RandomSource.create())?.sprite()
            } catch (e: Exception) {
                null
            }
        }

        private fun copyFirstFrame(sprite: TextureAtlasSprite): NativeImage? {
            return try {
                val contents = sprite.contents()
                val original = (contents as SpriteContentsAccessor).originalImage
                val w = contents.width().coerceAtLeast(1)
                val h = contents.height().coerceAtLeast(1)
                val out = NativeImage(NativeImage.Format.RGBA, w, h, false)
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        out.setPixel(x, y, original.getPixel(x, y))
                    }
                }
                out
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun clamp255(v: Int): Int = v.coerceIn(0, 255)

        private fun ItemStack.cacheKey(): String {
            val id = BuiltInRegistries.ITEM.getKey(item)
            return id.toString() + "#" + Integer.toHexString(hashCode())
        }
    }

    /**
     * luaj's touserdata() unwraps LuaUserdata to the raw m_instance, so accept
     * both the wrapper class and the wrapped vanilla object.
     */
    private fun LuaValue.toItemStack(): ItemStack? {
        return when {
            isuserdata() -> {
                when (val u = touserdata()) {
                    is LuaItemStack -> u.stack
                    is ItemStack -> u
                    else -> null
                }
            }
            isstring() -> {
                val optional = BuiltInRegistries.ITEM.get(Identifier.parse(tojstring()))
                if (optional.isPresent) ItemStack(optional.get().value()) else null
            }
            isint() -> {
                val optional = BuiltInRegistries.ITEM.get(toint())
                if (optional.isPresent) ItemStack(optional.get().value()) else null
            }
            else -> null
        }
    }

    private fun LuaValue.toBlockStack(): ItemStack? {
        toItemStack()?.let { return it }
        return when {
            isuserdata() -> {
                val block = when (val u = touserdata()) {
                    is LuaBlockState -> u.blockState.block
                    is net.minecraft.world.level.block.state.BlockState -> u.block
                    else -> null
                } ?: return null
                if (block.asItem() != net.minecraft.world.item.Items.AIR) ItemStack(block.asItem()) else null
            }
            isstring() -> {
                val id = Identifier.parse(tojstring())
                val optional = BuiltInRegistries.BLOCK.get(id)
                if (optional.isPresent) {
                    val block: Block = optional.get().value()
                    if (block.asItem() != net.minecraft.world.item.Items.AIR) ItemStack(block.asItem()) else null
                } else null
            }
            else -> null
        }
    }

    private fun LuaValue.toEntity(): Entity? {
        if (!isuserdata()) return null
        return when (val u = touserdata()) {
            is LuaEntity -> u.entity
            is Entity -> u
            else -> null
        }
    }
}
