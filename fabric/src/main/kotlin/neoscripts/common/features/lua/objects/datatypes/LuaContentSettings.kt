package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

/**
 * Настройки динамически регистрируемого предмета или блока.
 * Создается через creator.createSettings({ name = "...", texture = "...", ... })
 * и передается в creator.registerItem / creator.registerBlock.
 *
 * Поля:
 *  name         - отображаемое имя (локализуемое вручную, без lang-файлов)
 *  texture      - путь к текстуре относительно ассетов, например
 *                 "neoscripts:textures/item/my_item.png". Используется как
 *                 подсказка рендеру/скриптам; ванильная модель не создается,
 *                 предмет отображается стандартной моделью.
 *  maxStackSize - максимальный размер стака (по умолчанию 64)
 *  fireResistant - устойчивость к огню/лаве
 *  rarity       - редкость: "common", "uncommon", "rare", "epic"
 */
class LuaContentSettings(
    var name: String? = null,
    var texture: String? = null,
    var maxStackSize: Int = 64,
    var fireResistant: Boolean = false,
    var rarity: String? = null
) : LuaUserdata(this) {

    override fun typename(): String = "content_settings"
    override fun tojstring(): String =
        "ContentSettings{name=$name, texture=$texture, maxStackSize=$maxStackSize, fireResistant=$fireResistant, rarity=$rarity}"

    override fun get(key: LuaValue): LuaValue = when (key.tojstring()) {
        "name" -> name?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "texture" -> texture?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "maxStackSize" -> LuaValue.valueOf(maxStackSize)
        "fireResistant" -> LuaValue.valueOf(fireResistant)
        "rarity" -> rarity?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        else -> super.get(key)
    }

    override fun set(key: LuaValue, value: LuaValue) {
        when (key.tojstring()) {
            "name" -> name = if (value.isnil()) null else value.tojstring()
            "texture" -> texture = if (value.isnil()) null else value.tojstring()
            "maxStackSize" -> maxStackSize = value.checkint()
            "fireResistant" -> fireResistant = value.toboolean()
            "rarity" -> rarity = if (value.isnil()) null else value.tojstring()
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
        return props
    }

    /**
     * Отображаемое имя как Component (MutableComponent для Block.getName).
     */
    fun displayName(): MutableComponent? = name?.let { Component.literal(it) }

    companion object {
        /**
         * Собирает настройки из Lua-таблицы. Допустимые ключи:
         * name, texture, maxStackSize, fireResistant, rarity.
         */
        fun fromTable(table: LuaValue?): LuaContentSettings {
            val settings = LuaContentSettings()
            if (table == null || !table.istable()) return settings

            if (!table.get("name").isnil()) settings.name = table.get("name").tojstring()
            if (!table.get("texture").isnil()) settings.texture = table.get("texture").tojstring()
            if (table.get("maxStackSize").isnumber()) settings.maxStackSize = table.get("maxStackSize").toint()
            if (table.get("fire_resistant").isboolean() || table.get("fireResistant").isboolean()) {
                val v = if (!table.get("fireResistant").isnil()) table.get("fireResistant") else table.get("fire_resistant")
                settings.fireResistant = v.toboolean()
            }
            if (!table.get("rarity").isnil()) settings.rarity = table.get("rarity").tojstring()
            return settings
        }
    }
}
