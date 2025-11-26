package com.nekiplay.hypixelcry.pathfinder.calculate.path

import com.nekiplay.hypixelcry.pathfinder.calculate.Path
import com.nekiplay.hypixelcry.pathfinder.calculate.PathNode
import com.nekiplay.hypixelcry.pathfinder.calculate.openset.BinaryHeapOpenSet
import com.nekiplay.hypixelcry.pathfinder.goal.Goal
import com.nekiplay.hypixelcry.pathfinder.movement.CalculationContext
import com.nekiplay.hypixelcry.pathfinder.movement.MovementResult
import com.nekiplay.hypixelcry.pathfinder.movement.Moves
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos

class AStarPathFinder(val startX: Int, val startY: Int, val startZ: Int, val goal: Goal, val ctx: CalculationContext) {
    private val closedSet: Long2ObjectMap<PathNode> = Long2ObjectOpenHashMap()
    private var calculating = false
    private var closestNode: PathNode? = null
    private var closestDistance = Double.MAX_VALUE
    private var iterations = 0

    fun calculatePath(): Path? {
        calculating = true
        iterations = 0
        val openSet = BinaryHeapOpenSet()
        val startNode = PathNode(startX, startY, startZ, goal)
        val res = MovementResult()
        val moves = Moves.entries.toTypedArray()
        startNode.costSoFar = 0.0
        startNode.totalCost = startNode.costToEnd
        openSet.add(startNode)

        closestNode = startNode
        closestDistance = startNode.costToEnd

        while (!openSet.isEmpty() && calculating && iterations < ctx.maxIterations) {
            iterations++

            if (iterations % ctx.stepSize != 0) continue

            val currentNode = openSet.poll()

            if (currentNode.costToEnd < closestDistance) {
                closestNode = currentNode
                closestDistance = currentNode.costToEnd
            }

            if (goal.isAtGoal(currentNode.x, currentNode.y, currentNode.z)) {
                return Path(startNode, currentNode, goal, ctx)
            }

            for (move in moves) {
                res.reset()
                move.calculate(ctx, currentNode.x, currentNode.y, currentNode.z, res)
                var cost = res.cost
                val isLoaded = isChunkLoaded(res.x, res.y, res.z)
                if (!isLoaded) {
                    cost = ctx.cost.INF_COST / 2 // Достаточно высокая стоимость, но путь возможен
                }

                if (cost >= ctx.cost.INF_COST) continue

                val neighbourNode = getNode(res.x, res.y, res.z, PathNode.longHash(res.x, res.y, res.z))
                val neighbourCostSoFar = currentNode.costSoFar + cost

                if (neighbourNode.costSoFar > neighbourCostSoFar) {
                    neighbourNode.parentNode = currentNode
                    neighbourNode.costSoFar = neighbourCostSoFar
                    neighbourNode.totalCost = neighbourCostSoFar + neighbourNode.costToEnd

                    if (neighbourNode.heapPosition == -1) {
                        openSet.add(neighbourNode)
                    } else {
                        openSet.relocate(neighbourNode)
                    }
                }
            }
        }
        calculating = false

        closestNode?.let {
            if (it != startNode) {
                return Path(startNode, it, goal, ctx)
            }
        }
        return null
    }

    private fun isChunkLoaded(x: Int, y: Int, z: Int): Boolean {
        // Добавляем проверку на граничные блоки
        return ctx.world?.let { world ->
            val pos = BlockPos(x, y, z)
            world.isLoaded(pos) ||
                    world.isLoaded(pos.offset(1, 0, 0)) ||
                    world.isLoaded(pos.offset(-1, 0, 0)) ||
                    world.isLoaded(pos.offset(0, 0, 1)) ||
                    world.isLoaded(pos.offset(0, 0, -1))
        } ?: false
    }

    fun getNode(x: Int, y: Int, z: Int, hash: Long): PathNode {
        var n: PathNode? = closedSet.get(hash)
        if (n == null) {
            n = PathNode(x, y, z, goal)
            closedSet.put(hash, n)
        }
        return n
    }

    fun requestStop() {
        if (!calculating) return
        calculating = false
    }
}