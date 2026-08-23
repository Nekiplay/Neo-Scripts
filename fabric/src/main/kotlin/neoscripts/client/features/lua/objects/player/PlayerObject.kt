package com.nekiplay.neoscripts.client.features.lua.objects.player

import com.nekiplay.neoscripts.ClientMain.mc
import com.nekiplay.neoscripts.common.mixins.gui.HudAccessor
import com.nekiplay.neoscripts.client.sugar.getFormattedString
import com.nekiplay.neoscripts.client.sugar.getScorebordLines
import com.nekiplay.neoscripts.client.sugar.getTab
import com.nekiplay.neoscripts.client.utils.MathUtil
import com.nekiplay.neoscripts.client.utils.PlayerUtils
import com.nekiplay.neoscripts.client.utils.RaycastUtils
import com.nekiplay.neoscripts.client.utils.RotationUtils
import com.nekiplay.neoscripts.client.utils.aiming.RotationManager
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaRaycast
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponent
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.text.LuaComponentBuilder
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.LerpingBossEvent
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.ZeroArgFunction
import org.luaj.vm2.lib.jse.JavaInstance

class PlayerObject : LuaValue() {
    override fun call(): LuaValue {
        return this
    }

    // Precomputed dynamic keys
    private val javaClassKey = LuaValue.valueOf("javaClass")
    private val classKey = LuaValue.valueOf("class")
    private val entityKey = LuaValue.valueOf("entity")
    private val fishHookKey = LuaValue.valueOf("fishHook")

    override fun get(key: LuaValue): LuaValue {
        return when {
            key == javaClassKey || key == classKey -> JavaInstance(mc.player)
            key == entityKey -> {
                if (mc.player != null) {
                    LuaEntity(mc.player!!)
                } else {
                    NIL
                }
            }
            key == fishHookKey -> {
                if (mc.player != null && mc.player?.fishing != null) {
                    LuaEntity(mc.player!!.fishing!!)
                } else {
                    NIL
                }
            }
            key.type() == LuaValue.TSTRING -> functions[key] ?: NIL
            else -> NIL
        }
    }

    private val functions: Map<LuaValue, LuaValue> by lazy {
        buildMap {
            put(LuaValue.valueOf("input"), InputObject())
            put(LuaValue.valueOf("inventory"), InventoryObject())
            put(LuaValue.valueOf("network"), NetworkObject())
            put(LuaValue.valueOf("addMessage"), AddChatMessageFunction())
            put(LuaValue.valueOf("sendMessage"), SendChatMessageFunction())
            put(LuaValue.valueOf("sendCommand"), SendCommandFunction())
            put(LuaValue.valueOf("getPos"), GetPlayerPosFunction())
            put(LuaValue.valueOf("getPosition"), GetPlayerPosFunction())
            put(LuaValue.valueOf("getRotation"), GetPlayerRotationFunction())
            put(LuaValue.valueOf("getSilentRotation"), GetPlayerSilentRotationFunction())
            put(LuaValue.valueOf("getServerRotation"), GetPlayerSilentRotationFunction())
            put(LuaValue.valueOf("setRotation"), SetPlayerRotationFunction())
            put(LuaValue.valueOf("setSilentRotation"), SetPlayerSilentRotationFunction())
            put(LuaValue.valueOf("setServerRotation"), SetPlayerSilentRotationFunction())
            put(LuaValue.valueOf("getName"), GetPlayerNameFunction())
            put(LuaValue.valueOf("isSneaking"), IsPlayerSneakingFunction())
            put(LuaValue.valueOf("isSprinting"), IsPlayerSprintingFunction())
            put(LuaValue.valueOf("isOnGround"), IsPlayerOnGroundFunction())
            put(LuaValue.valueOf("isHasLineOfSight"), IsHasLineOfSight())
            put(LuaValue.valueOf("swingHand"), SwingHandFunction())
            put(LuaValue.valueOf("getEyePosition"), GetEyePositionFunction())
            put(LuaValue.valueOf("getLookEndPos"), GetLookEndPosFunction())
            put(LuaValue.valueOf("getDirectionFromYawPitch"), GetDirectionFromYawPitch())
            put(LuaValue.valueOf("getScoreBoardLines"), GetScoreboardLinesFunction())
            put(LuaValue.valueOf("getTab"), GetTabFunction())
            put(LuaValue.valueOf("addToast"), AddToastFunction())
            put(LuaValue.valueOf("getBossBar"), GetBossBarFunction())
            put(LuaValue.valueOf("raycast"), RayCastFunction())
            put(LuaValue.valueOf("raycastToEntity"), RayCastToEntityFunction())
            put(LuaValue.valueOf("raycastToBlocksFromId"), RayCastToBlocksFunction())
            put(LuaValue.valueOf("raycastToBlocksFromIdentifier"), RayCastToBlocksFromIdentifierFunction())
            put(LuaValue.valueOf("getTitle"), GetTitleFunction())
            put(LuaValue.valueOf("getSubTitle"), GetSubTitleFunction())
            put(LuaValue.valueOf("getActionBar"), GetActionBarFunction())

        }
    }

    private class GetTitleFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val accessed = mc.gui.hud as HudAccessor
            return valueOf(accessed.title?.getFormattedString())
        }
    }

    private class GetSubTitleFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val accessed = mc.gui.hud as HudAccessor
            return valueOf(accessed.subtitle?.getFormattedString())
        }
    }

    private class GetActionBarFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val accessed = mc.gui.hud as HudAccessor
            return valueOf(accessed.actionBar?.getFormattedString())
        }
    }

    private class IsHasLineOfSight : OneArgFunction() {
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

    private class GetBossBarFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val bossOverlay = mc.gui.hud.bossOverlay ?: return NIL
            val eventsMap = bossOverlay.events
            val table = tableOf()
            if (eventsMap == null || eventsMap.isEmpty()) {
                return table
            }
            eventsMap.forEach { (uuid, bossEvent) ->
                if (bossEvent is LerpingBossEvent) {
                    val bossBarTable = tableOf()
                    bossBarTable.set("uuid", valueOf(uuid.toString()))
                    bossBarTable.set("name", valueOf(bossEvent.name.getFormattedString()))
                    bossBarTable.set("percent", valueOf(bossEvent.getProgress().toDouble()))
                    bossBarTable.set("color", valueOf(bossEvent.color.name))
                    bossBarTable.set("overlay", valueOf(bossEvent.overlay.name))
                    bossBarTable.set("shouldCreateFog", valueOf(bossEvent.shouldCreateWorldFog()))
                    bossBarTable.set("shouldDarkenScreen", valueOf(bossEvent.shouldDarkenScreen()))
                    bossBarTable.set("shouldPlayBossMusic", valueOf(bossEvent.shouldPlayBossMusic()))
                    table.set(table.length() + 1, bossBarTable)
                }
            }
            return table
        }
    }

    private class AddToastFunction : ThreeArgFunction() {
        override fun call(arg: LuaValue?, arg2: LuaValue?, arg3: LuaValue?): LuaValue? {
            if (arg?.isstring() == true && arg2?.isstring() == true && arg3?.isnumber() == true) {
                val type = SystemToast.SystemToastId(arg3.tonumber().tolong())
                SystemToast.add(
                    mc.gui.toastManager(),
                    type,
                    Component.literal(arg.tojstring()),
                    Component.literal(arg2.tojstring())
                );
                return TRUE
            }
            return FALSE
        }
    }

    private class SwingHandFunction : OneArgFunction() {
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

    private class GetTabFunction : ZeroArgFunction() {
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

    private class GetScoreboardLinesFunction : ZeroArgFunction() {
        override fun call(): LuaValue? {
            val table = tableOf()
            mc.player?.getScorebordLines()?.forEachIndexed { index, line ->
                table.set(index + 1, valueOf(line.getFormattedString()))
            }
            return table
        }
    }

    private class GetDirectionFromYawPitch : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue? {
            if (arg1.isnumber() && arg2.isnumber()) {
                val table = tableOf()
                val rotations = RotationUtils.getDirectionFromYawPitch(arg1.tofloat(), arg2.tofloat())
                table.set("direction", LuaVector3d(rotations))
                return table
            }
            return NIL
        }
    }

    private class RayCastToBlocksFunction : TwoArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue
        ): LuaValue? {
            if (arg1?.isnumber() == true) {
                val player = mc.player ?: return NIL
                val targetBlocks = mutableListOf<Block>()
                if (arg2.istable()) {
                    val table = arg2.checktable()
                    val len = table.length()

                    for (i in 1..len) {
                        val value = table.get(i)
                        if (value.isint()) {
                            val id = value.toint()
                            val state = Block.stateById(id)

                            if (state != null) {
                                targetBlocks.add(state.block)
                            }
                        }
                    }
                } else {
                    return NIL
                }

                val hitResult = if (!RotationManager.getCurrentLastYaw().isNaN()) {
                    RaycastUtils.rayTrace(
                        mc.cameraEntity,
                        4.5,
                        RotationManager.getCurrentLastYaw(),
                        RotationManager.getCurrentPitch(),
                        targetBlocks
                    )
                }
                else {
                    RaycastUtils.rayTrace(
                        mc.cameraEntity,
                        4.5,
                        player.yRot,
                        player.xRot,
                        targetBlocks
                    )
                }
                return if (hitResult != null) {
                    LuaRaycast(hitResult)
                } else {
                    NIL
                }
            }
            return NIL
        }
    }

    private class RayCastToBlocksFromIdentifierFunction : TwoArgFunction() {
        override fun call(
            arg1: LuaValue?,
            arg2: LuaValue
        ): LuaValue? {
            if (arg1?.isnumber() == true) {
                val player = mc?.player ?: return NIL
                val targetBlocks = mutableListOf<Block>()
                if (arg2.istable()) {
                    val table = arg2.checktable()
                    val len = table.length()

                    for (i in 1..len) {
                        val value = table.get(i)
                        if (value.isstring()) {
                            val id = value.tojstring()
                            val state = BuiltInRegistries.BLOCK.get(Identifier.parse(id))

                            if (state.isPresent) {
                                targetBlocks.add(state.get().value())
                            }
                        }
                    }
                } else {
                    return NIL
                }

                val hitResult = if (!RotationManager.getCurrentLastYaw().isNaN()) {
                    RaycastUtils.rayTrace(
                        mc?.cameraEntity!!,
                        4.5,
                        RotationManager.getCurrentLastYaw(),
                        RotationManager.getCurrentPitch(),
                        targetBlocks
                    )
                }
                else {
                    RaycastUtils.rayTrace(
                        mc?.cameraEntity!!,
                        4.5,
                        player.yRot,
                        player.xRot,
                        targetBlocks
                    )
                }
                return if (hitResult != null) {
                    LuaRaycast(hitResult)
                } else {
                    NIL
                }
            }
            return NIL
        }
    }

    private class RayCastToEntityFunction : OneArgFunction() {
        override fun call(
            arg1: LuaValue?
        ): LuaValue? {
            if (arg1?.isnumber() == true) {
                val player = mc.player ?: return NIL
                val hitResult = if (!RotationManager.getCurrentYaw().isNaN()) {
                    RaycastUtils.findCrosshairEntity(
                        mc.cameraEntity,
                        player.eyePosition,
                        RotationManager.getCurrentYaw(),
                        RotationManager.getCurrentPitch(),
                        arg1.todouble()
                    )
                } else {
                    RaycastUtils.findCrosshairEntity(
                        mc.cameraEntity,
                        mc.player?.eyePosition,
                        player.yRot,
                        player.xRotO,
                        arg1.todouble()
                    )
                }
                return if (hitResult != null) {
                    LuaRaycast(hitResult)
                } else {
                    NIL
                }
            }
            return NIL
        }
    }

    private class RayCastFunction : OneArgFunction() {
        override fun call(
            arg1: LuaValue?
        ): LuaValue? {
            if (arg1?.isnumber() == true) {
                val player = mc.player ?: return NIL
                val hitResult = if (!RotationManager.getCurrentYaw().isNaN()) {
                    RaycastUtils.findCrosshairTarget(
                        mc.cameraEntity,
                        player.eyePosition,
                        RotationManager.getCurrentYaw(),
                        RotationManager.getCurrentPitch(),
                        arg1.todouble(),
                        arg1.todouble()
                    )
                } else {
                    RaycastUtils.findCrosshairTarget(
                        mc.cameraEntity,
                        mc.player?.eyePosition,
                        player.yRot,
                        player.xRotO,
                        arg1.todouble(),
                        arg1.todouble()
                    )
                }
                return if (hitResult != null) {
                    LuaRaycast(hitResult)
                } else {
                    NIL
                }
            }
            return NIL
        }
    }

    private class AddChatMessageFunction : OneArgFunction() {
        override fun call(message: LuaValue): LuaValue {
            if (message.isstring()) {
                mc.execute {
                    mc.player?.sendSystemMessage(Component.literal(message.tojstring()))
                }
                return TRUE
            }
            else if (message is LuaComponent) {
                mc.execute {
                    mc.player?.sendSystemMessage(message.component)
                }
                return TRUE
            }
            else if (message is LuaComponentBuilder) {
                mc.execute {
                    mc.player?.sendSystemMessage(message.buildComponent())
                }
                return TRUE
            }
            return FALSE
        }
    }

    private class SendCommandFunction : OneArgFunction() {
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

    private class SendChatMessageFunction : OneArgFunction() {
        override fun call(message: LuaValue): LuaValue {
            if (message.isstring()) {
                mc.execute {
                    mc.connection?.sendChat(message.tojstring())
                }
                return TRUE
            }
            return FALSE
        }
    }

    private class SetPlayerSilentRotationFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (args.isnumber(1) && args.isnumber(2)) {
                val player = mc.player
                if (player != null) {
                    // Ограничиваем yaw в диапазоне -180° до 180°
                    var yaw = args.optdouble(1, 0.0)
                    yaw %= 360f
                    if (yaw > 180f) yaw -= 360f
                    if (yaw < -180f) yaw += 360f

                    // Ограничиваем pitch в диапазоне -90° до 90° (стандартные ограничения Minecraft)
                    var pitch = args.optdouble(2, 0.0)
                    pitch = pitch.coerceIn(-90.0, 90.0)


                    val movementCorrection = args.optboolean(3, true) ?: true
                    var silentMovementCorrection = false
                    if (movementCorrection) {
                        silentMovementCorrection = args.optboolean(4, false) ?: false
                    }

                    var priority = args.optint(5, 1)

                    RotationManager.rotateTo(yaw.toFloat(), pitch.toFloat(), 1,  priority, movementCorrection, silentMovementCorrection)
                    return TRUE
                }
                return FALSE
            }
            return NIL
        }
    }

    private class SetPlayerRotationFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            if (arg1.isnumber() && arg2.isnumber()) {
                val player = mc.player
                if (player != null && mc.gui.screen() == null) {
                    // Ограничиваем yaw в диапазоне -180° до 180°
                    var yaw = arg1.tofloat()
                    yaw %= 360f
                    if (yaw > 180f) yaw -= 360f
                    if (yaw < -180f) yaw += 360f

                    // Ограничиваем pitch в диапазоне -90° до 90° (стандартные ограничения Minecraft)
                    var pitch = arg2.tofloat()
                    pitch = pitch.coerceIn(-90f, 90f)

                    player.yRot = MathUtil.applyGCD(yaw, yaw)
                    player.xRot = MathUtil.applyGCD(pitch, pitch)
                    return TRUE
                }
                return FALSE
            }
            return NIL
        }
    }

    private class GetPlayerSilentRotationFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val player = mc.player;
            return if (player != null) {
                val table = tableOf()
                if (!RotationManager.getCurrentYaw().isNaN()) {
                    table.set("yaw", valueOf(RotationManager.getCurrentYaw().toDouble()))
                    table.set("pitch", valueOf(RotationManager.getCurrentPitch().toDouble()))
                }
                else {
                    table.set("yaw", valueOf(player.yRot.toDouble()))
                    table.set("pitch", valueOf(player.xRot.toDouble()))
                }
                table
            } else {
                NIL
            }
        }
    }


    private class GetPlayerRotationFunction : ZeroArgFunction() {
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

    private class GetPlayerPosFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val player = mc.player
            return if (player != null) {
                LuaVector3d(player.getPosition(1f))
            } else {
                NIL
            }
        }
    }

    private class GetPlayerNameFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.player?.name?.string ?: "Unknown")
        }
    }
    private class IsPlayerSneakingFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.player?.isShiftKeyDown ?: false)
        }
    }

    private class IsPlayerSprintingFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.player?.isSprinting ?: false)
        }
    }

    private class IsPlayerOnGroundFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            return valueOf(mc.player?.onGround() ?: false)
        }
    }

    private class GetEyePositionFunction : ZeroArgFunction() {
        override fun call(): LuaValue {
            val eyePos = PlayerUtils.getEyePosition()
            val table = tableOf()
            table.set("x", valueOf(eyePos.x))
            table.set("y", valueOf(eyePos.y))
            table.set("z", valueOf(eyePos.z))
            return table
        }
    }

    private class GetLookEndPosFunction : TwoArgFunction() {
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
