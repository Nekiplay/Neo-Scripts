package com.nekiplay.neoscripts.features.lua.objects.player

import com.nekiplay.neoscripts.Main.mc
import com.nekiplay.neoscripts.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.features.lua.objects.datatypes.text.LuaComponent
import com.nekiplay.neoscripts.features.lua.objects.datatypes.text.LuaComponentBuilder
import com.nekiplay.neoscripts.mixins.minecraft.GamemodeAccessor
import com.nekiplay.neoscripts.sugar.getFormattedString
import com.nekiplay.neoscripts.sugar.getScorebordLines
import com.nekiplay.neoscripts.sugar.getTab
import com.nekiplay.neoscripts.utils.PlayerUtils
import com.nekiplay.neoscripts.utils.RaycastUtils
import com.nekiplay.neoscripts.utils.Rotations
import com.nekiplay.neoscripts.utils.StatusBarTracker
import com.nekiplay.neoscripts.utils.Utils
import com.nekiplay.neoscripts.utils.trackers.ColdTracker
import com.nekiplay.neoscripts.utils.trackers.PetCache
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

class PlayerObject : LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            // Objects
            "input" -> InputObject()
            "inventory" -> InventoryObject()
            "network" -> NetworkObject()

            // Variables
            "entity" -> {
                if (mc.player != null) {
                    LuaEntity(mc.player!!)
                }
                else {
                    NIL
                }
            }
            "fishHook" -> {
                if (mc.player != null && mc.player?.fishing != null) {
                    LuaEntity(mc.player!!.fishing!!)
                }
                else {
                    NIL
                }
            }

            // Functions
            "addMessage" -> AddChatMessageFunction()
            "sendMessage" -> SendChatMessageFunction()
            "sendCommand" -> SendCommandFunction()
            "getPos", "getPosition" -> GetPlayerPosFunction()
            "getRotation" -> GetPlayerRotationFunction()
            "setRotation" -> SetPlayerRotationFunction()
            "getName" -> GetPlayerNameFunction()
            "getArea" -> GetPlayerAreaFunction()
            "getRawLocation" -> GetPlayerRawLocationFunction()
            "getLocation" -> GetPlayerLocationFunction()
            "getProfile" -> GetPlayerProfileFunction()
            "getProfileId" -> GetPlayerProfileIdFunction()
            "getBits" -> GetPlayerBitsFunction()
            "getPurse" -> GetPlayerPurseFunction()
            "getHealth" -> GetPlayerHealthFunction()
            "getMaxHealth" -> GetPlayerMaxHealthFunction()
            "getMana" -> GetPlayerManaFunction()
            "getMaxMana" -> GetPlayerMaxManaFunction()
            "getDefence" -> GetPlayerDefenceFunction()
            "getSpeed" -> GetPlayerSpeedFunction()
            "getCold" -> GetPlayerColdFunction()
            "getAir" -> GetPlayerAirFunction()
            "getPet" -> GetPlayerPetFunction()
            "getMaxAir" -> GetPlayerMaxAirFunction()
            "getRank" -> GetPlayerRankFunction()
            "isSneaking" -> IsPlayerSneakingFunction()
            "isSprinting" -> IsPlayerSprintingFunction()
            "isOnGround" -> IsPlayerOnGroundFunction()
            "isOnSkyBlock" -> IsPlayerOnSkyBlockFunction()
            "isHasLineOfSight" -> IsHasLineOfSight()

            "swingHand" -> SwingHandFunction()

            "getEyePosition" -> GetEyePositionFunction()
            "getLookEndPos" -> GetLookEndPosFunction()
            "getDirectionFromYawPitch" -> GetDirectionFromYawPitch()

            "getScoreBoardLines" -> GetScoreboardLinesFunction()
            "getTab" -> GetTabFunction()

            "addToast" -> AddToastFunction()

            "raycast" -> RayCastFunction()
            else -> NIL
        } as LuaValue
    }

    private inner class IsHasLineOfSight : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue? {
            if (arg is LuaEntity) {
                if (mc.player?.hasLineOfSight(arg.entity) == true) {
                    return TRUE
                }
            }
            else if (arg.touserdata() is LuaEntity) {
                if (mc.player?.hasLineOfSight((arg.touserdata() as LuaEntity).entity) == true) {
                    return TRUE
                }
            }
            return FALSE
        }
    }

    private inner class AddToastFunction : ThreeArgFunction() {
        override fun call(arg: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue? {
            if (arg?.isstring() == true && arg2?.isstring() == true && arg3?.isnumber() == true) {
                val type = SystemToast.SystemToastId(arg3.tonumber().tolong())
                SystemToast.add(
                    mc.toastManager,
                    type,
                    Component.literal(arg.tojstring()),
                    Component.literal(arg2.tojstring())
                );
                return TRUE
            }
            return FALSE
        }
    }

    private inner class GetPlayerPetFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                val pet = PetCache.getCurrentPet()
                if (pet != null) {
                    val table = tableOf()
                    if (pet.name.isPresent) {
                        table.set("name", valueOf(pet.name.get()))
                    }
                    table.set("type", valueOf(pet.type))
                    table.set("exp", valueOf(pet.exp))
                    if (pet.item.isPresent) {
                        table.set("item", valueOf(pet.item.get()))
                    }
                    if (pet.skin.isPresent) {
                        table.set("skin", valueOf(pet.skin.get()))
                    }
                    table.set("tier", valueOf(pet.tier.name))
                    if (pet.uuid.isPresent) {
                        table.set("uuid", valueOf(pet.uuid.get()))
                    }
                    table
                }
                else {
                    NIL
                }
            } else {
                NIL
            }
        }
    }

    private inner class GetPlayerAirFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(StatusBarTracker.getAir().value)
            } else {
                valueOf(0)
            }
        }
    }

    private inner class GetPlayerMaxAirFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(StatusBarTracker.getAir().max)
            } else {
                valueOf(0)
            }
        }
    }

    private inner class GetPlayerRankFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(Utils.getRank().toString())
            } else {
                valueOf(0)
            }
        }
    }

    private inner class SwingHandFunction : OneArgFunction() {
        override fun call(arg: LuaValue?): LuaValue? {
            if (arg?.isboolean() == true) {
                if (arg.toboolean()) {
                    mc.player?.swing(InteractionHand.OFF_HAND)
                }
                else {
                    mc.player?.swing(InteractionHand.MAIN_HAND)
                }
            }
            else {
                mc.player?.swing(InteractionHand.MAIN_HAND)
            }
            return TRUE
        }
    }

    private inner class GetTabFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val tab = mc.player?.getTab()
            val table = tableOf()

            table.set("header", if (tab?.header?.isPresent == true)
                valueOf(tab.header.get().getFormattedString()) else NIL)

            table.set("footer", if (tab?.footer?.isPresent == true)
                valueOf(tab.footer.get().getFormattedString()) else NIL)

            val lines = tableOf()
            if (tab?.body != null && tab.body.isNotEmpty()) {
                tab.body.forEachIndexed { index, line ->
                    lines.set(index + 1, valueOf(line.getFormattedString()))
                }
            }
            table.set("body", if (lines.length() == 0) NIL else lines)

            return table
        }
    }

    private inner class GetScoreboardLinesFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val table = tableOf()
            mc.player?.getScorebordLines()?.forEachIndexed { index, line ->
                table.set(index + 1, valueOf(line.getFormattedString()))
            }
            return table
        }
    }

    private inner class GetDirectionFromYawPitch : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue? {
            if (arg1.isnumber() && arg2.isnumber()) {
                val table = tableOf()
                val rotations = Rotations.getDirectionFromYawPitch(arg1.tofloat(), arg2.tofloat())
                table.set("direction", LuaVector3d(rotations))
                return table
            }
            return NIL
        }
    }

    private inner class RayCastFunction : OneArgFunction() {
        override fun call(
            arg1: LuaValue?
        ): LuaValue? {
            if (arg1?.isnumber() == true) {
                val hitResult = RaycastUtils.findCrosshairTarget(mc.cameraEntity, arg1.todouble(), arg1.todouble(), 1f)
                return if (hitResult != null) {
                    processHitResult(hitResult)
                } else {
                    NIL
                }
            }
            return NIL
        }
    }

    private fun processHitResult(hitResult: HitResult): LuaValue {
        when (hitResult.type)
        {
            HitResult.Type.ENTITY -> {
                val table = tableOf()
                table.set("type", "entity")
                table.set("data", LuaEntity((hitResult as EntityHitResult).entity))
                return table
            }
            HitResult.Type.BLOCK -> {
                val result = hitResult as BlockHitResult
                val table = tableOf()
                table.set("type", "block")
                table.set("location", LuaVector3d(result.location))
                table.set("side", LuaDirection(result.direction))

                table.set("blockPos", LuaBlockPos(result.blockPos))

                return table
            }
            else -> {
                val table = tableOf()
                table.set("type", "miss")
                return table
            }
        }
    }

    private inner class AddChatMessageFunction : OneArgFunction() {
        override fun call(message: LuaValue): LuaValue {
            if (message.isstring()) {
                mc.execute {
                    mc.player?.displayClientMessage(Component.literal(message.tojstring()), false)
                }
                return TRUE
            }
            else if (message is LuaComponent) {
                mc.execute {
                    mc.player?.displayClientMessage(message.component, false)
                }
                return TRUE
            }
            else if (message is LuaComponentBuilder) {
                mc.execute {
                    mc.player?.displayClientMessage(message.buildComponent(), false)
                }
                return TRUE
            }
            return FALSE
        }
    }

    private inner class SendCommandFunction : OneArgFunction() {
        override fun call(message: LuaValue): LuaValue {
            val msg = message.tojstring()
            if (msg.isEmpty()) return FALSE

            val mc = Minecraft.getInstance()

            mc.execute {
                val player = mc.player ?: return@execute
                val connection = player.connection ?: return@execute

                if (msg.startsWith("/")) {
                    val commandLine = msg.substring(1) // Текст команды без "/"
                    connection.sendCommand(commandLine)
                } else {
                    // Обычное сообщение в чат
                    connection.sendChat(msg)
                }
            }

            return TRUE
        }
    }

    private inner class SendChatMessageFunction : OneArgFunction() {
        override fun call(message: LuaValue): LuaValue {
            if (message.isstring()) {
                mc.connection?.sendChat(message.tojstring())
                return TRUE
            }
            return FALSE
        }
    }

    private inner class SetPlayerRotationFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            if (arg1.isnumber() && arg2.isnumber()) {
                val player = mc.player
                if (player != null) {
                    // Ограничиваем yaw в диапазоне -180° до 180°
                    var yaw = arg1.tofloat()
                    yaw %= 360f
                    if (yaw > 180f) yaw -= 360f
                    if (yaw < -180f) yaw += 360f

                    // Ограничиваем pitch в диапазоне -90° до 90° (стандартные ограничения Minecraft)
                    var pitch = arg2.tofloat()
                    pitch = pitch.coerceIn(-90f, 90f)

                    player.yRot = yaw
                    player.xRot = pitch
                    return TRUE
                }
                return FALSE
            }
            return NIL
        }
    }

    private inner class GetPlayerRotationFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val player = mc.player;
            return if (player != null) {
                val table = tableOf()
                table.set("yaw", valueOf(player.yRot.toDouble()))
                table.set("pitch", valueOf(player.xRot.toDouble()))
                table
            } else {
                NIL
            }
        }
    }

    private inner class GetPlayerPosFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val player = mc.player
            return if (player != null) {
                LuaVector3d(player.getPosition(1f))
            } else {
                NIL
            }
        }
    }

    private inner class GetPlayerNameFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.player?.name?.string ?: "Unknown")
        }
    }

    private inner class GetPlayerAreaFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(Utils.getArea())
        }
    }

    private inner class GetPlayerRawLocationFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(Utils.getRawLocation())
        }
    }

    private inner class GetPlayerLocationFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(Utils.getLocation().name)
        }
    }

    private inner class GetPlayerProfileFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(Utils.getProfile())
            } else {
                NIL
            }
        }
    }

    private inner class GetPlayerProfileIdFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(Utils.getProfileId())
            } else {
                NIL
            }
        }
    }

    private inner class GetPlayerBitsFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(Utils.getBits())
            } else {
                valueOf(0.0)
            }
        }
    }

    private inner class GetPlayerPurseFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(Utils.getPurse())
            } else {
                valueOf(0.0)
            }
        }
    }

    private inner class GetPlayerMaxHealthFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(StatusBarTracker.getHealth().max())
            } else {
                valueOf((mc.player?.health)?.toDouble() ?: 0.0)
            }
        }
    }

    private inner class GetPlayerHealthFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(StatusBarTracker.getHealth().value())
            } else {
                valueOf((mc.player?.health)?.toDouble() ?: 0.0)
            }
        }
    }

    private inner class GetPlayerDefenceFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(StatusBarTracker.getDefense())
            } else {
                valueOf(0)
            }
        }
    }

    private inner class GetPlayerSpeedFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(StatusBarTracker.getSpeed().value())
            } else {
                valueOf(0)
            }
        }
    }

    private inner class GetPlayerMaxManaFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(StatusBarTracker.getMana().max())
            } else {
                valueOf(0)
            }
        }
    }

    private inner class GetPlayerManaFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(StatusBarTracker.getMana().value())
            } else {
                valueOf(0)
            }
        }
    }

    private inner class GetPlayerColdFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return if (Utils.isOnSkyblock()) {
                valueOf(ColdTracker.getCold())
            } else {
                valueOf(0)
            }
        }
    }

    private inner class IsPlayerSneakingFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.player?.isShiftKeyDown ?: false)
        }
    }

    private inner class IsPlayerSprintingFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.player?.isSprinting ?: false)
        }
    }

    private inner class IsPlayerOnGroundFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.player?.onGround() ?: false)
        }
    }

    private inner class IsPlayerOnSkyBlockFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(Utils.isOnSkyblock())
        }
    }

    private inner class GetEyePositionFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val eyePos = PlayerUtils.getEyePosition()
            val table = tableOf()
            table.set("x", valueOf(eyePos.x))
            table.set("y", valueOf(eyePos.y))
            table.set("z", valueOf(eyePos.z))
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

                val target = Vec3(targetX, targetY, targetZ)
                val endPos = PlayerUtils.getLookEndPos(target, distance.toFloat())

                val table = tableOf()
                table.set("x", valueOf(endPos.x))
                table.set("y", valueOf(endPos.y))
                table.set("z", valueOf(endPos.z))
                table
            } else if (arg1?.isnumber() == true) {
                // Если передан только distance (от текущего взгляда)
                val distance = arg1.todouble()
                val endPos = PlayerUtils.getLookEndPos(distance.toFloat())

                val table = tableOf()
                table.set("x", valueOf(endPos.x))
                table.set("y", valueOf(endPos.y))
                table.set("z", valueOf(endPos.z))
                table
            } else {
                NIL
            }
        }
    }

    // Переопределяем необходимые методы LuaValue
    override fun typename(): String = "player"
    override fun tojstring(): String = "PlayerObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }
}