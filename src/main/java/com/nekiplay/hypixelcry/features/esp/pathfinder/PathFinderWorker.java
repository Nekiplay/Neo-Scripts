package com.nekiplay.hypixelcry.features.esp.pathfinder;

import com.nekiplay.hypixelcry.HypixelCry;
import com.nekiplay.hypixelcry.annotations.Init;
import com.nekiplay.hypixelcry.events.world.ClientChunkLoadEvent;
import com.nekiplay.hypixelcry.pathfinder.calculate.Path;
import com.nekiplay.hypixelcry.pathfinder.calculate.path.AStarPathFinder;
import com.nekiplay.hypixelcry.pathfinder.goal.Goal;
import com.nekiplay.hypixelcry.pathfinder.movement.CalculationContext;
import com.nekiplay.hypixelcry.utils.render.RenderHelper;
import com.nekiplay.hypixelcry.utils.scheduler.Scheduler;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.nekiplay.hypixelcry.HypixelCry.mc;

public class PathFinderWorker {
    private static final ExecutorService PATH_FINDER_EXECUTOR = Executors.newFixedThreadPool(12);
    private static final Map<String, PathData> PATHS = new ConcurrentHashMap<>();
    private static final Queue<PathResult> PATH_RESULTS = new ConcurrentLinkedQueue<>();
    private static final double RECALCULATION_DISTANCE = 9.0;
    private static final int CHUNK_UPDATE_RADIUS = 1;
    private static final int ENDPOINT_CHUNK_CHECK_RADIUS = 2; // Radius to check around endpoint for chunk loads

    public static class PathData {
        public final BlockPos end;
        public final float[] color;
        public final String endText;
        public List<BlockPos> blocks = new ArrayList<>();
        public List<BlockPos> remainingPath = new ArrayList<>();
        public int furthestReachedIndex = 0;
        public int currentVisibleFromIndex = 0;
        public boolean needsUpdate = true;
        public int lastChunkX = Integer.MIN_VALUE;
        public int lastChunkZ = Integer.MIN_VALUE;
        public boolean chunksUpdated = false;
        public boolean endpointChunksUpdated = false;
        public boolean smoothes = true;
        public boolean allow_update = true;

        public PathData(BlockPos end, float[] color, String endText) {
            this.end = end;
            this.color = color;
            this.endText = endText;
        }
    }

    private record PathResult(String pathId, List<BlockPos> blocks) {
    }

    @Init
    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(PathFinderWorker::onRenderWorldLast);
        Scheduler.INSTANCE.scheduleCyclic(PathFinderWorker::onClientTick, 1);
        ClientChunkLoadEvent.EVENT.register(PathFinderWorker::chunkLoad);
    }

    private static void chunkLoad(ClientWorld clientWorld, Chunk chunk) {
        if (mc.player == null || mc.world == null) return;

        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        for (PathData pathData : PATHS.values()) {
            pathData.endpointChunksUpdated = true;
            pathData.needsUpdate = true;
        }
    }

    private static void onClientTick() {
        if (mc.player == null || mc.world == null) return;

        BlockPos currentPos = mc.player.getBlockPos().add(0, -1, 0);
        processPathResults();

        if (HypixelCry.config.misc.pathFinderESP.enabled) {
            PATHS.values().forEach(pathData -> {
                if (pathData.allow_update) {
                    updatePath(currentPos, pathData);
                }
            });
        }
    }

    private static void processPathResults() {
        while (!PATH_RESULTS.isEmpty()) {
            PathResult result = PATH_RESULTS.poll();
            Optional.ofNullable(PATHS.get(result.pathId)).ifPresent(data -> {
                data.blocks = result.blocks;
                data.furthestReachedIndex = 0;
                data.currentVisibleFromIndex = 0;
                data.chunksUpdated = false;
                data.endpointChunksUpdated = false;
                data.needsUpdate = false;
            });
        }
    }

    private static void updatePath(BlockPos currentPos, PathData pathData) {
        updateChunkData(currentPos, pathData);
        updateRemainingPath(currentPos, pathData);

        if (shouldRecalculatePath(currentPos, pathData)) {
            recalculatePath(currentPos, pathData);
        }
    }

    private static void updateChunkData(BlockPos currentPos, PathData pathData) {
        int currentChunkX = currentPos.getX() >> 4;
        int currentChunkZ = currentPos.getZ() >> 4;

        if (Math.abs(currentChunkX - pathData.lastChunkX) > CHUNK_UPDATE_RADIUS ||
                Math.abs(currentChunkZ - pathData.lastChunkZ) > CHUNK_UPDATE_RADIUS) {
            pathData.chunksUpdated = true;
            pathData.lastChunkX = currentChunkX;
            pathData.lastChunkZ = currentChunkZ;
        }
    }

    private static void recalculatePath(BlockPos currentPos, PathData pathData) {
        PATH_FINDER_EXECUTOR.submit(() -> {
            CalculationContext ctx = new CalculationContext();
            BlockPos targetPos = getNearestLoadedPos(ctx, pathData.end);

            AStarPathFinder finder = new AStarPathFinder(
                    currentPos.getX(), currentPos.getY(), currentPos.getZ(),
                    new Goal(targetPos.getX(), targetPos.getY(), targetPos.getZ(), ctx),
                    ctx
            );

            Optional<Path> calculatedPath = Optional.ofNullable(finder.calculatePath());
            calculatedPath.map(path -> pathData.smoothes ? path.getSmoothedPath() : path.getPath())
                    .ifPresent(path -> {
                        pathData.remainingPath = path;
                        PATH_RESULTS.add(new PathResult(getPathId(pathData), path));
                    });
        });
    }

    private static String getPathId(PathData pathData) {
        return PATHS.entrySet().stream()
                .filter(entry -> entry.getValue() == pathData)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("");
    }

    private static BlockPos getNearestLoadedPos(CalculationContext ctx, BlockPos target) {
        if (ctx.getWorld().isPosLoaded(target)) {
            return target;
        }

        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos farthestLoaded = playerPos;

        int dx = target.getX() - playerPos.getX();
        int dz = target.getZ() - playerPos.getZ();
        double length = Math.sqrt(dx * dx + dz * dz);

        if (length <= 0) {
            return playerPos;
        }

        double stepX = dx / length;
        double stepZ = dz / length;

        int maxDistance = 16 * 16;

        for (int i = 1; i <= maxDistance; i++) {
            int checkX = playerPos.getX() + (int)(stepX * i);
            int checkZ = playerPos.getZ() + (int)(stepZ * i);
            BlockPos checkPos = new BlockPos(checkX, playerPos.getY(), checkZ);

            if (ctx.getWorld().isPosLoaded(checkPos)) {
                farthestLoaded = checkPos;
            } else {
                break;
            }
        }

        return farthestLoaded;
    }

    private static boolean shouldRecalculatePath(BlockPos currentPos, PathData pathData) {
        if (pathData.needsUpdate || pathData.blocks.isEmpty()) {
            return true;
        }

        if (pathData.endpointChunksUpdated) {
            return true;
        }

        BlockPos endPos = pathData.blocks.getLast();
        double distanceToEnd = currentPos.getSquaredDistance(endPos);
        if (distanceToEnd < RECALCULATION_DISTANCE * RECALCULATION_DISTANCE) {
            return true;
        }

        if (!isPathToLoadedArea(currentPos, pathData)) {
            return true;
        }

        BlockPos nearest = findNearestPathPoint(currentPos, pathData.blocks);
        return nearest == null || currentPos.getSquaredDistance(nearest) > RECALCULATION_DISTANCE * RECALCULATION_DISTANCE;
    }

    private static boolean isPathToLoadedArea(BlockPos playerPos, PathData pathData) {
        if (mc.world == null) return false;

        int checkLength = Math.min(5, pathData.blocks.size());
        for (int i = pathData.blocks.size() - 1; i >= pathData.blocks.size() - checkLength; i--) {
            BlockPos pathPos = pathData.blocks.get(i);
            if (!mc.world.isPosLoaded(pathPos)) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos findNearestPathPoint(BlockPos playerPos, List<BlockPos> path) {
        if (path == null || path.isEmpty()) return null;
        if (path.size() < 2) return path.getFirst();

        return IntStream.range(0, path.size() - 1)
                .mapToObj(i -> getClosestPointOnSegment(playerPos, path.get(i), path.get(i + 1)))
                .min(Comparator.comparingDouble(playerPos::getSquaredDistance))
                .orElse(null);
    }

    private static BlockPos getClosestPointOnSegment(BlockPos point, BlockPos start, BlockPos end) {
        double lineX = end.getX() - start.getX();
        double lineY = end.getY() - start.getY();
        double lineZ = end.getZ() - start.getZ();

        double pointX = point.getX() - start.getX();
        double pointY = point.getY() - start.getY();
        double pointZ = point.getZ() - start.getZ();

        double dot = pointX * lineX + pointY * lineY + pointZ * lineZ;
        double t = Math.max(0, Math.min(1, dot / (lineX*lineX + lineY*lineY + lineZ*lineZ)));

        return new BlockPos(
                (int) (start.getX() + t * lineX),
                (int) (start.getY() + t * lineY),
                (int) (start.getZ() + t * lineZ)
        );
    }

    private static void updateRemainingPath(BlockPos playerPos, PathData pathData) {
        if (pathData.remainingPath.isEmpty()) return;

        BlockPos nearest = findNearestPathPoint(playerPos, pathData.remainingPath);
        if (nearest == null) return;

        int nearestIndex = pathData.remainingPath.indexOf(nearest);
        if (nearestIndex > 0) {
            pathData.remainingPath = pathData.remainingPath.subList(nearestIndex, pathData.remainingPath.size());
        }
    }

    private static void onRenderWorldLast(WorldRenderContext context) {
        if (mc.world == null || PATHS.isEmpty() || !HypixelCry.config.esp.pathFinderESP.enabled) return;

        PATHS.values().forEach(pathData -> renderPath(pathData, context));
    }

    private static void renderPath(PathData pathData, WorldRenderContext context) {
        if (pathData.blocks == null || pathData.blocks.isEmpty() || pathData.remainingPath.isEmpty()) return;

        BlockPos endPos = pathData.blocks.getLast();
        renderPathLines(pathData, endPos, context);
        renderEndPoint(pathData, endPos, context);
    }

    private static void renderPathLines(PathData pathData, BlockPos endPos, WorldRenderContext context) {
        BlockPos prevPos = pathData.blocks.getFirst();
        renderStartPoint(pathData, prevPos, context);
        for (int i = 1; i < pathData.blocks.size(); i++) {
            BlockPos currentPos = pathData.blocks.get(i);
            Vec3d[] points = new Vec3d[] {
                    prevPos.toCenterPos(),
                    currentPos.toCenterPos()
            };
            RenderHelper.renderLinesFromPoints(context, points, pathData.color, pathData.color[3], 3f, true);
            if (HypixelCry.config.esp.pathFinderESP.enableSubPoints) {
                RenderHelper.renderFilled(context, currentPos, pathData.color, pathData.color[3], true);
                RenderHelper.renderOutline(context, currentPos, pathData.color, 1f, true);
            }
            prevPos = currentPos;
        }
    }

    private static void renderStartPoint(PathData pathData, BlockPos startPos, WorldRenderContext context) {
        RenderHelper.renderFilled(context, startPos, pathData.color, pathData.color[3], true);
        RenderHelper.renderOutline(context, startPos, pathData.color, 4f, true);

        //int r = (int) (pathData.color[0] * 255);
        //int g = (int) (pathData.color[1] * 255);
        //int b = (int) (pathData.color[2] * 255);

        //int color = (255 << 24) | (r << 16) | (g << 8) | b;

        //RenderHelper.renderText(context, Text.of(pathData.endText).asOrderedText(), startPos.toCenterPos().add(0, 1, 0), color, 1, 0.5f, true);
        //RenderHelper.renderText(context, Text.of("Start").asOrderedText(), startPos.toCenterPos().add(0, 1.2, 0), color, 1, 1.5f, true);
    }

    private static void renderEndPoint(PathData pathData, BlockPos endPos, WorldRenderContext context) {
        RenderHelper.renderFilled(context, endPos, pathData.color, pathData.color[3], true);
        RenderHelper.renderOutline(context, endPos, pathData.color, 4f, true);

        int r = (int) (pathData.color[0] * 255);
        int g = (int) (pathData.color[1] * 255);
        int b = (int) (pathData.color[2] * 255);

        int color = (255 << 24) | (r << 16) | (g << 8) | b;

        RenderHelper.renderText(context, Text.of(pathData.endText).asOrderedText(), endPos.toCenterPos().add(0, 1, 0), color, 1, 0.5f, true);
    }

    // API methods
    public static void addOrUpdatePath(String id, BlockPos end, float[] color, String endText, boolean smooth, boolean allow_update) {
        PathData newData = new PathData(end, color, endText);
        if (!end.equals(Optional.ofNullable(PATHS.get(id)).map(data -> data.end).orElse(null))) {
            newData.needsUpdate = true;
        }
        newData.smoothes = smooth;
        newData.allow_update = allow_update;
        PATHS.put(id, newData);
    }

    public static void removePath(String id) {
        PATHS.remove(id);
    }

    public static void clearAllPaths() {
        PATHS.clear();
    }

    public static boolean hasPath(String id) {
        return PATHS.containsKey(id);
    }

    public static List<BlockPos> getPathBlocks(String id) {
        PathData data = PATHS.get(id);
        return data != null ? new ArrayList<>(data.blocks) : Collections.emptyList();
    }

    public static void shutdown() {
        PATH_FINDER_EXECUTOR.shutdownNow();
    }
}
