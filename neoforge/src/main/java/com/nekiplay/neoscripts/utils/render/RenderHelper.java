package com.nekiplay.neoscripts.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.nekiplay.neoscripts.Main;
import com.nekiplay.neoscripts.utils.render.primitive.PrimitiveCollectorImpl;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = Main.ID, value = Dist.CLIENT)
public class RenderHelper {
    private static final Minecraft CLIENT = Minecraft.getInstance();
    public static PrimitiveCollectorImpl collector;

    // Инициализация больше не нужна — подписка происходит автоматически через аннотацию
    // Если нужна дополнительная инициализация, можно вызвать метод из главного класса мода.

    /**
     * Этап до начала рендера мира (аналог END_EXTRACTION)
     * Начинаем сбор примитивов.
     */
    @SubscribeEvent
    public static void onAfterSky(RenderLevelStageEvent.AfterSky event) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("neoscripts_primitiveCollection");

        Matrix4f projectionMatrix = Minecraft.getInstance().gameRenderer.getProjectionMatrix(Minecraft.getInstance().options.fov().get());
        Matrix4f modelViewMatrix = event.getModelViewMatrix();

        Frustum frustum = new Frustum(modelViewMatrix, projectionMatrix);

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double cameraX = camera.position().x;
        double cameraY = camera.position().y;
        double cameraZ = camera.position().z;

        frustum.prepare(cameraX, cameraY, cameraZ);

        collector = new PrimitiveCollectorImpl(event.getLevelRenderState(), frustum);


        WorldRenderExtractionCallback extract = new WorldRenderExtractionCallback(collector);
        NeoForge.EVENT_BUS.post(extract);

        collector.endCollection();
        profiler.pop();
    }

    /**
     * Этап после отрисовки твёрдых блоков, перед сущностями (аналог BEFORE_ENTITIES)
     * Отправляем стандартные субмиттаблы.
     */
    @SubscribeEvent
    public static void onAfterSolidBlocks(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        if (collector == null) return;
        ProfilerFiller profiler = Profiler.get();
        profiler.push("neoscripts_submitVanillaSubmittables");
        //collector.dispatchVanillaSubmittables(event.getLevelRenderState(), event.getLevelRenderState());
        profiler.pop();
    }

    /**
     * Этап после отрисовки всех сущностей (аналог END_MAIN)
     * Завершаем сбор и выполняем отрисовку.
     */
    @SubscribeEvent
    public static void onAfterEntities(RenderLevelStageEvent.AfterEntities event) {
        if (collector == null) return;
        ProfilerFiller profiler = Profiler.get();

        profiler.push("neoscripts_submitPrimitives");
        collector.dispatchPrimitivesToRenderers(event.getLevelRenderState().cameraRenderState);
        collector = null;
        profiler.pop();

        profiler.push("neoscripts_executeDraws");
        Renderer.executeDraws(); // предполагается, что Renderer — ваш класс для отрисовки
        profiler.pop();
    }

    // Остальные вспомогательные методы без изменений
    public static void runOnRenderThread(Runnable runnable) {
        if (RenderSystem.isOnRenderThread()) {
            runnable.run();
        } else {
            CLIENT.execute(runnable);
        }
    }

    public static DeltaTracker getTickCounter() {
        return CLIENT.getDeltaTracker();
    }

    public static Camera getCamera() {
        return CLIENT.gameRenderer.getMainCamera();
    }

    @Nullable
    public static AABB getBlockBoundingBox(ClientLevel world, BlockPos pos) {
        return getBlockBoundingBox(world, world.getBlockState(pos), pos);
    }

    @Nullable
    public static AABB getBlockBoundingBox(ClientLevel world, BlockState state, BlockPos pos) {
        VoxelShape shape = state.getShape(world, pos).singleEncompassing();
        return shape.isEmpty() ? null : shape.bounds().move(pos);
    }
}