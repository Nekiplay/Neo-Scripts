package com.nekiplay.hypixelcry.pathfinder.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3


fun BlockPos.toVec3() = Vec3(x.toDouble() + 0.5, y.toDouble() + 0.5, z.toDouble() + 0.5)