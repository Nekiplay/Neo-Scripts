package com.nekiplay.hypixelcry.features.lua.objects.player

import com.nekiplay.hypixelcry.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.text.LuaComponent
import com.nekiplay.hypixelcry.features.lua.objects.datatypes.text.LuaComponentBuilder
import com.nekiplay.hypixelcry.pathfinder.utils.mc
import com.nekiplay.hypixelcry.sugar.getFormattedString
import com.nekiplay.hypixelcry.sugar.getScorebordLines
import com.nekiplay.hypixelcry.sugar.getTab
import com.nekiplay.hypixelcry.utils.PlayerUtils
import com.nekiplay.hypixelcry.utils.RaycastUtils
import com.nekiplay.hypixelcry.utils.Rotations
import com.nekiplay.hypixelcry.utils.StatusBarTracker
import com.nekiplay.hypixelcry.utils.Utils
import com.nekiplay.hypixelcry.utils.trackers.ColdTracker
import com.nekiplay.hypixelcry.utils.trackers.PetCache
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua

class PlayerObject(private val L: Lua) {
    fun register() {
        // 1. Создаем основную таблицу модуля player
        L.newTable()
        val playerTableIdx = L.getTop()

        // --- Объекты (Sub-modules) ---
        InputObject(L).push()
        L.setField(playerTableIdx, "input")

        InventoryObject(L).push()
        L.setField(playerTableIdx, "inventory")

        NetworkObject(L).push()
        L.setField(playerTableIdx, "network")

        // --- Функции ---
        val functions = mapOf(
            "addMessage" to JFunction { l -> addChatMessage(l) },
            "sendMessage" to JFunction { l -> sendChatMessage(l) },
            "sendCommand" to JFunction { l -> sendCommand(l) },
            "getPos" to JFunction { l -> getPlayerPos(l) },
            "getPosition" to JFunction { l -> getPlayerPos(l) },
            "getRotation" to JFunction { l -> getPlayerRotation(l) },
            "setRotation" to JFunction { l -> setPlayerRotation(l) },
            "getName" to JFunction { l -> getPlayerName(l) },
            "getArea" to JFunction { l -> getPlayerArea(l) },
            "getRawLocation" to JFunction { l -> getPlayerRawLocation(l) },
            "getLocation" to JFunction { l -> getPlayerLocation(l) },
            "getProfile" to JFunction { l -> getPlayerProfile(l) },
            "getProfileId" to JFunction { l -> getPlayerProfileId(l) },
            "getBits" to JFunction { l -> getPlayerBits(l) },
            "getPurse" to JFunction { l -> getPlayerPurse(l) },
            "getHealth" to JFunction { l -> getPlayerHealth(l) },
            "getMaxHealth" to JFunction { l -> getPlayerMaxHealth(l) },
            "getMana" to JFunction { l -> getPlayerMana(l) },
            "getMaxMana" to JFunction { l -> getPlayerMaxMana(l) },
            "getDefence" to JFunction { l -> getPlayerDefence(l) },
            "getSpeed" to JFunction { l -> getPlayerSpeed(l) },
            "getCold" to JFunction { l -> getPlayerCold(l) },
            "getAir" to JFunction { l -> getPlayerAir(l) },
            "getPet" to JFunction { l -> getPlayerPet(l) },
            "getMaxAir" to JFunction { l -> getPlayerMaxAir(l) },
            "getRank" to JFunction { l -> getPlayerRank(l) },
            "isSneaking" to JFunction { l -> isPlayerSneaking(l) },
            "isSprinting" to JFunction { l -> isPlayerSprinting(l) },
            "isOnGround" to JFunction { l -> isPlayerOnGround(l) },
            "isOnSkyBlock" to JFunction { l -> isPlayerOnSkyBlock(l) },
            "isHasLineOfSight" to JFunction { l -> hasLineOfSight(l) },
            "swingHand" to JFunction { l -> swingHand(l) },
            "getEyePosition" to JFunction { l -> getEyePosition(l) },
            "getLookEndPos" to JFunction { l -> getLookEndPos(l) },
            "getDirectionFromYawPitch" to JFunction { l -> getDirectionFromYawPitch(l) },
            "getScoreBoardLines" to JFunction { l -> getScoreboardLines(l) },
            "getTab" to JFunction { l -> getTab(l) },
            "addToast" to JFunction { l -> addToast(l) },
            "raycast" to JFunction { l -> rayCast(l) }
        )

        functions.forEach { (name, func) ->
            L.push(func)
            L.setField(playerTableIdx, name)
        }

        // 2. Создаем метатаблицу для динамических полей (entity, fishHook)
        L.newTable()
        L.push(JFunction { l ->
            val key = l.toString(2)
            when (key) {
                "entity" -> {
                    val p = mc.player
                    if (p != null) l.push(LuaEntity(l, p).push()) else l.pushNil()
                    1
                }
                "fishHook" -> {
                    val hook = mc.player?.fishing
                    if (hook != null) l.push(LuaEntity(l, hook).push()) else l.pushNil()
                    1
                }
                else -> {
                    // Если это не динамическое поле, ищем в основной таблице функций
                    l.pushValue(1) // Кладём саму таблицу player
                    l.pushValue(2) // Кладём ключ
                    l.rawGet(-3)   // Ищем значение в таблице (без вызова метаметодов)
                    1
                }
            }
        })
        L.setField(-2, "__index")

        // Привязываем метатаблицу к таблице player
        L.setMetatable(playerTableIdx)

        // 3. Делаем таблицу глобальной
        L.setGlobal("player")
    }

    private fun hasLineOfSight(l: Lua): Int {
        // Извлекаем переданный объект из первого аргумента
        val obj = l.toJavaObject(1)

        // Пытаемся привести его к нашей обертке LuaEntity
        val luaEntity = obj as? LuaEntity
        val targetEntity = luaEntity?.entity

        if (targetEntity != null) {
            val result = mc.player?.hasLineOfSight(targetEntity) == true
            l.push(result)
            return 1
        }

        l.push(false)
        return 1
    }

    /**
     * Добавление тоста (уведомления): player.addToast(title, message, typeId)
     */
    private fun addToast(l: Lua): Int {
        // Проверяем типы аргументов: String, String, Number
        if (l.isString(1) && l.isString(2) && l.isNumber(3)) {
            val title = l.toString(1) ?: ""
            val message = l.toString(2) ?: ""
            val typeId = l.toNumber(3).toLong()

            // Создаем ID тоста (в новых версиях MC конструктор может отличаться)
            val type = SystemToast.SystemToastId(typeId)

            SystemToast.add(
                mc.toastManager,
                type,
                Component.literal(title),
                Component.literal(message)
            )
            l.push(true)
            return 1
        }

        l.push(false)
        return 1
    }

    private fun getPlayerPet(l: Lua): Int {
        // 1. Проверяем, находится ли игрок на Skyblock
        if (!Utils.isOnSkyblock()) {
            return 0 // Возвращаем nil (0 значений на стеке)
        }

        // 2. Получаем текущего питомца из кэша
        val pet = PetCache.getCurrentPet() ?: return 0

        // 3. Создаем таблицу для данных питомца
        l.newTable() // Таблица теперь на индексе -1

        // 4. Наполняем таблицу данными (учитывая Optional поля)

        // Имя
        pet.name.ifPresent { name ->
            l.push(name)
            l.setField(-2, "name")
        }

        // Тип (например, "ENDERMAN")
        l.push(pet.type)
        l.setField(-2, "type")

        // Опыт
        l.push(pet.exp)
        l.setField(-2, "exp")

        // Предмет питомца (Optional)
        pet.item.ifPresent { item ->
            l.push(item)
            l.setField(-2, "item")
        }

        // Скин (Optional)
        pet.skin.ifPresent { skin ->
            l.push(skin)
            l.setField(-2, "skin")
        }

        // Тир/Редкость (LEGENDARY, EPIC и т.д.)
        l.push(pet.tier.name)
        l.setField(-2, "tier")

        // UUID (Optional)
        pet.uuid.ifPresent { uuid ->
            l.push(uuid)
            l.setField(-2, "uuid")
        }

        // Возвращаем 1, так как на вершине стека лежит наша заполненная таблица
        return 1
    }

    private fun getPlayerAir(l: Lua): Int {
        val value = if (Utils.isOnSkyblock()) {
            StatusBarTracker.getAir().value
        } else {
            0
        }
        l.push(value.toDouble())
        return 1
    }

    private fun getPlayerMaxAir(l: Lua): Int {
        val max = if (Utils.isOnSkyblock()) {
            StatusBarTracker.getAir().max
        } else {
            0
        }
        l.push(max.toDouble())
        return 1
    }

    private fun getPlayerRank(l: Lua): Int {
        val rank = if (Utils.isOnSkyblock()) {
            Utils.getRank().toString()
        } else {
            "0" // Сохраняем логику вашего оригинала
        }
        l.push(rank)
        return 1
    }

    private fun swingHand(l: Lua): Int {
        val offhand = if (l.isBoolean(1)) l.toBoolean(1) else false
        mc.player?.swing(if (offhand) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND)
        l.push(true)
        return 1
    }

    private fun getTab(l: Lua): Int {
        val tab = mc.player?.getTab()
        l.newTable()

        if (tab?.header?.isPresent == true) {
            l.push(tab.header.get().getFormattedString())
            l.setField(-2, "header")
        }

        if (tab?.footer?.isPresent == true) {
            l.push(tab.footer.get().getFormattedString())
            l.setField(-2, "footer")
        }

        if (tab?.body != null && tab.body.isNotEmpty()) {
            l.newTable()
            tab.body.forEachIndexed { index, line ->
                l.push(line.getFormattedString())
                l.rawSetI(-2, index + 1)
            }
            l.setField(-2, "body")
        }

        return 1
    }

    private fun getScoreboardLines(l: Lua): Int {
        val lines = mc.player?.getScorebordLines()
        l.newTable()
        lines?.forEachIndexed { index, line ->
            l.push(line.getFormattedString())
            l.rawSetI(-2, index + 1)
        }
        return 1
    }

    private fun getDirectionFromYawPitch(l: Lua): Int {
        if (l.isNumber(1) && l.isNumber(2)) {
            val rotations = Rotations.getDirectionFromYawPitch(l.toNumber(1).toFloat(), l.toNumber(2).toFloat())
            l.newTable()
            l.push(rotations.x.toDouble()); l.setField(-2, "x")
            l.push(rotations.y.toDouble()); l.setField(-2, "y")
            l.push(rotations.z.toDouble()); l.setField(-2, "z")
            return 1
        }
        return 0
    }

    private fun rayCast(l: Lua): Int {
        if (l.isNumber(1)) {
            val distance = l.toNumber(1)
            val hitResult = RaycastUtils.findCrosshairTarget(mc.cameraEntity, distance, distance, 1f)
            if (hitResult != null) {
                pushHitResult(l, hitResult)
                return 1
            }
        }
        return 0
    }

    private fun pushHitResult(l: Lua, hitResult: HitResult) {
        l.newTable()
        when (hitResult.type) {
            HitResult.Type.ENTITY -> {
                val entityHit = hitResult as EntityHitResult
                l.push("entity"); l.setField(-2, "type")
                l.push(LuaEntity(l, entityHit.entity).push())
                l.setField(-2, "data")
            }
            HitResult.Type.BLOCK -> {
                val blockHit = hitResult as BlockHitResult
                l.push("block"); l.setField(-2, "type")
                l.push(blockHit.blockPos.x.toDouble()); l.setField(-2, "x")
                l.push(blockHit.blockPos.y.toDouble()); l.setField(-2, "y")
                l.push(blockHit.blockPos.z.toDouble()); l.setField(-2, "z")
                l.push(blockHit.direction.toString()); l.setField(-2, "side")

                l.newTable()
                l.push(blockHit.blockPos.x.toDouble()); l.setField(-2, "x")
                l.push(blockHit.blockPos.y.toDouble()); l.setField(-2, "y")
                l.push(blockHit.blockPos.z.toDouble()); l.setField(-2, "z")
                l.setField(-2, "blockPos")
            }
            else -> {
                l.push("miss"); l.setField(-2, "type")
            }
        }
    }

    private fun addChatMessage(l: Lua): Int {
        val raw = l.toJavaObject(1)
        val component = when {
            l.isString(1) -> Component.literal(l.toString(1) ?: "")
            raw is LuaComponent -> raw.component
            raw is LuaComponentBuilder -> raw.buildComponent()
            else -> null
        }

        if (component != null) {
            mc.execute {
                mc.player?.displayClientMessage(component, false)
            }
            l.push(true)
        } else {
            l.push(false)
        }
        return 1
    }

    private fun sendCommand(l: Lua): Int {
        val msg = l.toString(1) ?: ""
        if (msg.isEmpty()) {
            l.push(false)
            return 1
        }

        mc.execute {
            val player = mc.player ?: return@execute
            val connection = player.connection ?: return@execute

            if (msg.startsWith("/")) {
                connection.sendCommand(msg.substring(1))
            } else {
                connection.sendChat(msg)
            }
        }
        l.push(true)
        return 1
    }

    private fun sendChatMessage(l: Lua): Int {
        val msg = l.toString(1)
        return if (msg != null) {
            mc.connection?.sendChat(msg)
            l.push(true)
            1
        } else {
            l.push(false)
            1
        }
    }

    private fun setPlayerRotation(l: Lua): Int {
        if (l.isNumber(1) && l.isNumber(2)) {
            val player = mc.player
            if (player != null) {
                var yaw = l.toNumber(1).toFloat()
                yaw %= 360f
                if (yaw > 180f) yaw -= 360f
                if (yaw < -180f) yaw += 360f

                var pitch = l.toNumber(2).toFloat()
                pitch = pitch.coerceIn(-90f, 90f)

                player.yRot = yaw
                player.xRot = pitch
                l.push(true)
                return 1
            }
        }
        l.push(false)
        return 1
    }

    private fun getPlayerRotation(l: Lua): Int {
        val player = mc.player
        if (player != null) {
            l.newTable()
            l.push(player.yRot.toDouble()); l.setField(-2, "yaw")
            l.push(player.xRot.toDouble()); l.setField(-2, "pitch")
            return 1
        }
        return 0
    }

    private fun getPlayerPos(l: Lua): Int {
        val player = mc.player
        if (player != null) {
            l.newTable()
            l.push(player.x); l.setField(-2, "x")
            l.push(player.y); l.setField(-2, "y")
            l.push(player.z); l.setField(-2, "z")
            return 1
        }
        return 0
    }

    private fun getPlayerName(l: Lua): Int {
        l.push(mc.player?.name?.string ?: "Unknown")
        return 1
    }

    private fun getPlayerArea(l: Lua): Int {
        l.push(Utils.getArea())
        return 1
    }

    private fun getPlayerRawLocation(l: Lua): Int {
        l.push(Utils.getRawLocation())
        return 1
    }

    private fun getPlayerLocation(l: Lua): Int {
        l.push(Utils.getLocation().name)
        return 1
    }

    private fun getPlayerProfile(l: Lua): Int {
        return if (Utils.isOnSkyblock()) {
            l.push(Utils.getProfile())
            1
        } else 0
    }

    private fun getPlayerProfileId(l: Lua): Int {
        return if (Utils.isOnSkyblock()) {
            l.push(Utils.getProfileId())
            1
        } else 0
    }

    private fun getPlayerBits(l: Lua): Int {
        val bits = if (Utils.isOnSkyblock()) Utils.getBits() else 0.0
        l.push(bits)
        return 1
    }

    private fun getPlayerPurse(l: Lua): Int {
        val purse = if (Utils.isOnSkyblock()) Utils.getPurse() else 0.0
        l.push(purse)
        return 1
    }

    private fun getPlayerMaxHealth(l: Lua): Int {
        val value = if (Utils.isOnSkyblock()) {
            StatusBarTracker.getHealth().max().toDouble()
        } else {
            mc.player?.maxHealth?.toDouble() ?: 0.0
        }
        l.push(value)
        return 1
    }

    private fun getPlayerHealth(l: Lua): Int {
        val value = if (Utils.isOnSkyblock()) {
            StatusBarTracker.getHealth().value().toDouble()
        } else {
            mc.player?.health?.toDouble() ?: 0.0
        }
        l.push(value)
        return 1
    }

    private fun getPlayerDefence(l: Lua): Int {
        val value = if (Utils.isOnSkyblock()) StatusBarTracker.getDefense().toDouble() else 0.0
        l.push(value)
        return 1
    }

    private fun getPlayerSpeed(l: Lua): Int {
        val value = if (Utils.isOnSkyblock()) StatusBarTracker.getSpeed().value().toDouble() else 0.0
        l.push(value)
        return 1
    }

    private fun getPlayerMaxMana(l: Lua): Int {
        val value = if (Utils.isOnSkyblock()) StatusBarTracker.getMana().max().toDouble() else 0.0
        l.push(value)
        return 1
    }

    private fun getPlayerMana(l: Lua): Int {
        val value = if (Utils.isOnSkyblock()) StatusBarTracker.getMana().value().toDouble() else 0.0
        l.push(value)
        return 1
    }

    private fun getPlayerCold(l: Lua): Int {
        val value = if (Utils.isOnSkyblock()) ColdTracker.getCold().toDouble() else 0.0
        l.push(value)
        return 1
    }

    private fun isPlayerSneaking(l: Lua): Int {
        l.push(mc.player?.isShiftKeyDown ?: false)
        return 1
    }

    private fun isPlayerSprinting(l: Lua): Int {
        l.push(mc.player?.isSprinting ?: false)
        return 1
    }

    private fun isPlayerOnGround(l: Lua): Int {
        l.push(mc.player?.onGround() ?: false)
        return 1
    }

    private fun isPlayerOnSkyBlock(l: Lua): Int {
        l.push(Utils.isOnSkyblock())
        return 1
    }


    private fun getEyePosition(l: Lua): Int {
        val eyePos = PlayerUtils.getEyePosition()
        l.newTable()
        l.push(eyePos.x); l.setField(-2, "x")
        l.push(eyePos.y); l.setField(-2, "y")
        l.push(eyePos.z); l.setField(-2, "z")
        return 1
    }

    private fun getLookEndPos(l: Lua): Int {
        val endPos: Vec3 = if (l.isTable(1) && l.isNumber(2)) {
            l.getField(1, "x"); val tx = l.toNumber(-1); l.pop(1)
            l.getField(1, "y"); val ty = l.toNumber(-1); l.pop(1)
            l.getField(1, "z"); val tz = l.toNumber(-1); l.pop(1)
            val distance = l.toNumber(2)
            PlayerUtils.getLookEndPos(Vec3(tx, ty, tz), distance.toFloat())
        } else if (l.isNumber(1)) {
            val distance = l.toNumber(1)
            PlayerUtils.getLookEndPos(distance.toFloat())
        } else {
            return 0
        }

        l.newTable()
        l.push(endPos.x); l.setField(-2, "x")
        l.push(endPos.y); l.setField(-2, "y")
        l.push(endPos.z); l.setField(-2, "z")
        return 1
    }
}