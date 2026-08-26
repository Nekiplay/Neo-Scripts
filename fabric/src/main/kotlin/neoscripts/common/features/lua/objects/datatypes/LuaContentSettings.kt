package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.block.state.BlockBehaviour
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

/**
 * Настройки динамически регистрируемого предмета или блока.
 * Создается через creator.createSettings({ ... }) и передается в
 * creator.registerItem / creator.registerBlock / creator.registerBlockItem.
 * Все поля опциональны.
 *
 * Поля предмета:
 *  name           - отображаемое имя
 *  texture        - ПУТЬ К ФАЙЛУ текстуры на диске (как в 2d renderer
 *                   renderImage), например "config/neoscripts/textures/my_item.png".
 *                   Загружается в DynamicTexture, получить можно через
 *                   creator.getItemTexture("neoscripts:my_item").
 *  maxStackSize   - максимальный размер стака (по умолчанию 64)
 *  fireResistant  - устойчивость к огню/лаве
 *  rarity         - редкость: "common", "uncommon", "rare", "epic"
 *  durability     - прочность (инструменты, броня)
 *  craftRemainder - id предмета-остатка при крафте ("minecraft:bucket")
 *  enchantable    - базовая стоимость зачарования (int)
 *  useCooldown    - кулдаун использования в секундах (float)
 *
 * Поля блока (применяются только в registerBlock):
 *  hardness       - твердость (время разрушения); задает и сопротивление взрыву,
 *                   если resistance не указано отдельно
 *  resistance     - сопротивление взрыву
 *  luminance      - уровень света 0..15
 *  friction       - трение (скольжение), 0.6 по умолчанию у ванильных блоков
 */
class LuaContentSettings(
    var name: String? = null,
    var texture: String? = null,
    var maxStackSize: Int = 64,
    var fireResistant: Boolean = false,
    var rarity: String? = null,
    var durability: Int? = null,
    var craftRemainder: String? = null,
    var enchantable: Int? = null,
    var useCooldown: Float? = null,
    var hardness: Float? = null,
    var resistance: Float? = null,
    var luminance: Int? = null,
    var friction: Float? = null
) : LuaUserdata(this) {

    override fun typename(): String = "content_settings"

    override fun tojstring(): String =
        "ContentSettings{name=$name, texture=$texture, maxStackSize=$maxStackSize, rarity=$rarity" +
            (durability?.let { ", durability=$it" } ?: "") +
            (craftRemainder?.let { ", craftRemainder=$it" } ?: "") +
            (hardness?.let { ", hardness=$it" } ?: "") + "}"

    override fun get(key: LuaValue): LuaValue = when (key.tojstring()) {
        "name" -> name?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "texture" -> texture?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "maxStackSize" -> LuaValue.valueOf(maxStackSize)
        "fireResistant", "fire_resistant" -> LuaValue.valueOf(fireResistant)
        "rarity" -> rarity?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "durability" -> durability?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "craftRemainder", "craft_remainder" -> craftRemainder?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "enchantable" -> enchantable?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "useCooldown", "use_cooldown" -> useCooldown?.let { LuaValue.valueOf(it.toDouble()) } ?: LuaValue.NIL
        "hardness" -> hardness?.let { LuaValue.valueOf(it.toDouble()) } ?: LuaValue.NIL
        "resistance" -> resistance?.let { LuaValue.valueOf(it.toDouble()) } ?: LuaValue.NIL
        "luminance" -> luminance?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "friction" -> friction?.let { LuaValue.valueOf(it.toDouble()) } ?: LuaValue.NIL
        else -> super.get(key)
    }

    override fun set(key: LuaValue, value: LuaValue) {
        when (key.tojstring()) {
            "name" -> name = if (value.isnil()) null else value.tojstring()
            "texture" -> texture = if (value.isnil()) null else value.tojstring()
            "maxStackSize" -> maxStackSize = value.checkint()
            "fireResistant", "fire_resistant" -> fireResistant = value.toboolean()
            "rarity" -> rarity = if (value.isnil()) null else value.tojstring()
            "durability" -> durability = if (value.isnil()) null else value.checkint()
            "craftRemainder", "craft_remainder" -> craftRemainder = if (value.isnil()) null else value.tojstring()
            "enchantable" -> enchantable = if (value.isnil()) null else value.checkint()
            "useCooldown", "use_cooldown" -> useCooldown = if (value.isnil()) null else value.checkdouble().toFloat()
            "hardness" -> hardness = if (value.isnil()) null else value.checkdouble().toFloat()
            "resistance" -> resistance = if (value.isnil()) null else value.checkdouble().toFloat()
            "luminance" -> luminance = if (value.isnil()) null else value.checkint().coerceIn(0, 15)
            "friction" -> friction = if (value.isnil()) null else value.checkdouble().toFloat()
            else -> super.set(key, value)
        }
    }

    /**
     * Применяет настройки к Item.Properties.
     */
    fun applyTo(properties: Item.Properties): Item.Properties {
        var props = properties.stacksTo(maxStackSize.coerceIn(1, 99))
        if (fireResistant) props = props.fireResistant()
        if (rarity != null) {
            try {
                props = props.rarity(Rarity.valueOf(rarity!!.uppercase()))
            } catch (_: IllegalArgumentException) {
                // Неизвестная редкость — оставляем common
            }
        }
        durability?.let { if (it > 0) props = props.durability(it) }
        enchantable?.let { if (it >= 0) props = props.enchantable(it) }
        useCooldown?.let { if (it > 0f) props = props.useCooldown(it) }
        craftRemainder?.let { idStr ->
            try {
                val holder = BuiltInRegistries.ITEM.get(Identifier.parse(idStr))
                if (holder.isPresent) props = props.craftRemainder(holder.get().value())
            } catch (_: Exception) {
                // Неизвестный предмет — пропускаем
            }
        }
        return props
    }

    /**
     * Применяет настройки к BlockBehaviour.Properties.
     */
    fun applyTo(properties: BlockBehaviour.Properties): BlockBehaviour.Properties {
        var props = properties
        if (hardness != null || resistance != null) {
            val h = hardness ?: resistance ?: 1.0f
            val r = resistance ?: hardness ?: 1.0f
            props = props.strength(h, r)
        }
        luminance?.let { lvl -> props = props.lightLevel { lvl } }
        friction?.let { props = props.friction(it) }
        return props
    }

    /**
     * Отображаемое имя как Component (MutableComponent для Block.getName).
     */
    fun displayName(): MutableComponent? = name?.let { Component.literal(it) }

    companion object {
        /**
         * Собирает настройки из Lua-таблицы. Допустимые ключи (snake_case или camelCase):
         * name, texture, maxStackSize / max_stack_size, fireResistant / fire_resistant,
         * rarity, durability, craftRemainder / craft_remainder, enchantable,
         * useCooldown / use_cooldown, hardness, resistance, luminance, friction.
         */
        fun fromTable(table: LuaValue?): LuaContentSettings {
            val settings = LuaContentSettings()
            if (table == null || !table.istable()) return settings

            fun str(vararg keys: String): String? {
                for (k in keys) {
                    val v = table.get(k)
                    if (!v.isnil()) return v.tojstring()
                }
                return null
            }

            fun num(vararg keys: String): Double? {
                for (k in keys) {
                    val v = table.get(k)
                    if (v.isnumber()) return v.todouble()
                }
                return null
            }

            settings.name = str("name")
            settings.texture = str("texture")
            num("maxStackSize", "max_stack_size")?.let { settings.maxStackSize = it.toInt() }
            table.get("fireResistant").takeIf { !it.isnil() }?.let { settings.fireResistant = it.toboolean() }
                ?: table.get("fire_resistant").takeIf { !it.isnil() }?.let { settings.fireResistant = it.toboolean() }
            settings.rarity = str("rarity")
            num("durability")?.let { settings.durability = it.toInt() }
            settings.craftRemainder = str("craftRemainder", "craft_remainder")
            num("enchantable")?.let { settings.enchantable = it.toInt() }
            num("useCooldown", "use_cooldown")?.let { settings.useCooldown = it.toFloat() }
            num("hardness")?.let { settings.hardness = it.toFloat() }
            num("resistance")?.let { settings.resistance = it.toFloat() }
            num("luminance")?.let { settings.luminance = it.toInt().coerceIn(0, 15) }
            num("friction")?.let { settings.friction = it.toFloat() }
            return settings
        }
    }
}
