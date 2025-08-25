package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.features.lua.utils.block.BlockUtil
import com.nekiplay.hypixelcry.features.lua.utils.block.EntityUtils
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.utils.PlayerUtils
import com.nekiplay.hypixelcry.utils.StatusBarTracker
import com.nekiplay.hypixelcry.utils.Utils
import com.nekiplay.hypixelcry.utils.trackers.ColdTracker
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.luaj.vm2.LuaString
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class PlayerObject : LuaValue() {
    private val client: MinecraftClient = MinecraftClient.getInstance()

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // Objects
            "input" -> InputObject()

            // Variables
            "entity" -> EntityUtils.ToLua(mc.player)

            // Functions
            "addMessage" -> AddChatMessageFunction()
            "sendMessage" -> SendChatMessageFunction()
            "sendCommand" -> SendChatMessageFunction()
            "getPos" -> GetPlayerPosFunction()
            "getRotation" -> GetPlayerRotationFunction()
            "setRotation" -> SetPlayerRotationFunction()
            "getName" -> GetPlayerNameFunction()
            "getLocation" -> GetPlayerLocationFunction()
            "getPurse" -> GetPlayerPurseFunction()
            "getHealth" -> GetPlayerHealthFunction()
            "getMana" -> GetPlayerManaFunction()
            "getDefence" -> GetPlayerDefenceFunction()
            "getSpeed" -> GetPlayerSpeedFunction()
            "getCold" -> GetPlayerColdFunction()
            "isSneaking" -> IsPlayerSneakingFunction()
            "isSprinting" -> IsPlayerSprintingFunction()
            "isOnGround" -> IsPlayerOnGroundFunction()
            "isOnSkyBlock" -> IsPlayerOnSkyBlockFunction()

            "getEyePosition" -> GetEyePositionFunction()
            "getLookEndPos" -> GetLookEndPosFunction()

            "raycast" -> RayCastFunction()
            else -> LuaValue.NIL
        } as LuaValue
    }

    private inner class RayCastFunction : OneArgFunction() {
        override fun call(
            arg1: LuaValue?
        ): LuaValue? {
            if (arg1?.isnumber() == true) {
                val raycast = mc.player?.raycast(arg1.todouble(), 1f, false)
                if (raycast?.type == HitResult.Type.BLOCK && raycast is BlockHitResult) {
                    val table = LuaValue.tableOf()
                    val blockPos = BlockPos(raycast.blockPos.x, raycast.blockPos.y, raycast.blockPos.z)
                    table.set("type", "block")
                    table.set("x", blockPos.getX())
                    table.set("y", blockPos.getY())
                    table.set("z", blockPos.getZ()))
                    table.set("data", BlockUtil.ToLua(blockPos, state))
                    return table
                }
                else if (raycast?.type == HitResult.Type.ENTITY && raycast is EntityHitResult) {
                    val table = LuaValue.tableOf()
                    table.set("type", "entity")
                    table.set("data", EntityUtils.ToLua(raycast.entity))
                    return table
                }
            }

            return LuaValue.NIL
        }
    }

    private inner class AddChatMessageFunction : OneArgFunction() {
        override fun call(message: LuaValue): LuaValue {
            if (message.isstring()) {
                client.player?.sendMessage(Text.of(message.tojstring()), false)
            }
            return NIL
        }
    }

    private inner class SendChatMessageFunction : OneArgFunction() {
        override fun call(message: LuaValue): LuaValue {
            if (message.isstring()) {
                client.networkHandler?.sendChatMessage(message.tojstring())
            }
            return NIL
        }
    }

    private inner class SendChatCommandFunction : OneArgFunction() {
        override fun call(message: LuaValue): LuaValue {
            if (message.isstring()) {
                client.networkHandler?.sendChatCommand(message.tojstring())
            }
            return NIL
        }
    }

    private inner class SetPlayerRotationFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            if (arg1.isnumber() && arg2.isnumber()) {
                val player = client.player
                if (player != null) {
                    // Ограничиваем yaw в диапазоне -180° до 180°
                    var yaw = arg1.tofloat()
                    yaw = yaw % 360f
                    if (yaw > 180f) yaw -= 360f
                    if (yaw < -180f) yaw += 360f

                    // Ограничиваем pitch в диапазоне -90° до 90° (стандартные ограничения Minecraft)
                    var pitch = arg2.tofloat()
                    pitch = pitch.coerceIn(-90f, 90f)

                    player.yaw = yaw
                    player.pitch = pitch
                }
            }
            return LuaValue.NIL
        }
    }

    private inner class GetPlayerRotationFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val player = client.player;
            return if (player != null) {
                val table = LuaValue.tableOf()
                table.set("yaw", LuaValue.valueOf(player.yaw.toDouble()))
                table.set("pitch", LuaValue.valueOf(player.pitch.toDouble()))
                table
            } else {
                NIL
            }
        }
    }

    private inner class GetPlayerPosFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val player = client.player
            return if (player != null) {
                val table = LuaValue.tableOf()
                table.set("x", LuaValue.valueOf(player.x))
                table.set("y", LuaValue.valueOf(player.y))
                table.set("z", LuaValue.valueOf(player.z))
                table
            } else {
                NIL
            }
        }
    }

    private inner class GetPlayerNameFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaString.valueOf(client.player?.name?.string ?: "Unknown")
        }
    }

    private inner class GetPlayerLocationFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaString.valueOf(Utils.getLocation().name)
        }
    }

    private inner class GetPlayerPurseFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                LuaValue.valueOf(Utils.getPurse())
            } else {
                LuaValue.valueOf(0.0)
            }
        }
    }

    private inner class GetPlayerHealthFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                LuaValue.valueOf(StatusBarTracker.getHealth().value())
            } else {
                LuaValue.valueOf((client.player?.health)?.toDouble() ?: 0.0)
            }
        }
    }

    private inner class GetPlayerDefenceFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                LuaValue.valueOf(StatusBarTracker.getDefense())
            } else {
                LuaValue.valueOf(0)
            }
        }
    }

    private inner class GetPlayerSpeedFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                LuaValue.valueOf(StatusBarTracker.getSpeed().value())
            } else {
                LuaValue.valueOf(0)
            }
        }
    }

    private inner class GetPlayerManaFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                LuaValue.valueOf(StatusBarTracker.getMana().value())
            } else {
                LuaValue.valueOf(0)
            }
        }
    }

    private inner class GetPlayerColdFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                LuaValue.valueOf(ColdTracker.getCold())
            } else {
                LuaValue.valueOf(0)
            }
        }
    }

    private inner class IsPlayerSneakingFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(client.player?.isSneaking ?: false)
        }
    }

    private inner class IsPlayerSprintingFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(client.player?.isSprinting ?: false)
        }
    }

    private inner class IsPlayerOnGroundFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(client.player?.isOnGround ?: false)
        }
    }

    private inner class IsPlayerOnSkyBlockFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return LuaValue.valueOf(Utils.isOnSkyblock())
        }
    }

    private inner class GetEyePositionFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val eyePos = PlayerUtils.getEyePosition()
            val table = LuaValue.tableOf()
            table.set("x", LuaValue.valueOf(eyePos.x))
            table.set("y", LuaValue.valueOf(eyePos.y))
            table.set("z", LuaValue.valueOf(eyePos.z))
            return table
        }
    }

    private inner class GetLookEndPosFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue?, arg2: LuaValue?): LuaValue {
            return if (arg1?.istable() == true && arg2?.isnumber() == true) {
                // Если передан target и distance
                val targetX = arg1.get("x").optdouble(0.0)
                val targetY = arg1.get("y").optdouble(0.0)
                val targetZ = arg1.get("z").optdouble(0.0)
                val distance = arg2.todouble()

                val target = Vec3d(targetX, targetY, targetZ)
                val endPos = PlayerUtils.getLookEndPos(target, distance.toFloat())

                val table = LuaValue.tableOf()
                table.set("x", LuaValue.valueOf(endPos.x))
                table.set("y", LuaValue.valueOf(endPos.y))
                table.set("z", LuaValue.valueOf(endPos.z))
                table
            } else if (arg1?.isnumber() == true) {
                // Если передан только distance (от текущего взгляда)
                val distance = arg1.todouble()
                val endPos = PlayerUtils.getLookEndPos(distance.toFloat())

                val table = LuaValue.tableOf()
                table.set("x", LuaValue.valueOf(endPos.x))
                table.set("y", LuaValue.valueOf(endPos.y))
                table.set("z", LuaValue.valueOf(endPos.z))
                table
            } else {
                LuaValue.NIL
            }
        }
    }

    // Переопределяем необходимые методы LuaValue
    override fun typename(): String = "player"
    override fun tojstring(): String = "PlayerObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return LuaValue.TUSERDATA
    }
}
