package com.nekiplay.neoscripts.common.features.lua.objects.datatypes

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import com.nekiplay.neoscripts.client.sugar.toComponent
import net.minecraft.world.item.Item
import net.minecraft.world.item.Rarity
import net.minecraft.world.level.block.state.BlockBehaviour
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue

/**
 * Настройки динамически регистрируемого предмета или блока.
 * Создается через creator.createSettings({ ... }) и передается в
 * creator.registerItem / creator.registerBlock / creator.registerBlockItem.
 * Все поля опциональны. См. https://docs.fabricmc.net/develop/blocks/first-block
 *
 * Поля предмета:
 *  name           - отображаемое имя
 *  texture        - ПУТЬ К ФАЙЛУ текстуры на диске (как в 2d renderer
 *                   renderImage), например "config/neoscripts/textures/my_item.png".
 *                   Загружается в DynamicTexture, получить можно через
 *                   creator.getItemTexture("neoscripts:my_item").
 *  model          - родительская модель (Identifier) или путь к JSON-файлу:
 *                   "minecraft:item/generated" (дефолт для предметов),
 *                   "minecraft:item/handheld",
 *                   "minecraft:block/cube_all" (дефолт для блоков),
 *                   "minecraft:block/cross" (растение), "minecraft:block/cube" и т.д.
 *                   Поддерживается короткая запись "minecraft:diamond_block" ->
 *                   "minecraft:block/diamond_block" (блок) / "minecraft:item/diamond" (предмет)
 *                   и любой неймспейс: "tinker_construct:block/cast"
 *                   Если указан путь к файлу "*.json" (например "config/neoscripts/models/foo.json"),
 *                   файл читается и отдаётся как модель.
 *  maxStackSize   - максимальный размер стака (по умолчанию 64)
 *  fireResistant  - устойчивость к огню/лаве
 *  rarity         - редкость: "common", "uncommon", "rare", "epic"
 *  durability     - прочность (инструменты, броня)
 *  craftRemainder - id предмета-остатка при крафте ("minecraft:bucket")
 *  enchantable    - базовая стоимость зачарования (int)
 *  useCooldown    - кулдаун использования в секундах (float)
 *
 * Поля блока (применяются только в registerBlock / registerStairs / registerSlab / registerDoor / registerTrapdoor / registerFence):
 *  hardness / destroy_time / destroyTime / strength — твердость (время разрушения); задает и сопротивление взрыву,
 *                   если resistance не указано отдельно
 *  resistance / explosion_resistance - сопротивление взрыву
 *  luminance / light_level / lightLevel — уровень света 0..15
 *  friction       - трение (скольжение), 0.6 по умолчанию у ванильных блоков
 *  sound / soundType — тип звука: "stone","wood","gravel","grass","metal","glass","wool","sand","snow","ladder","anvil","slime","honey","wet_grass","coral","bamboo","soul_sand","soul_soil","basalt","wart","nether_wood","stem","shroomlight","nylium","fungus","roots","chain","copper","cave_vines","glow_lichen","powder_snow","sculk","sculk_vein","sculk_sensor","bone_block","dripstone","pointed_dripstone","amethyst","amethyst_cluster","small_amethyst_bud","medium_amethyst_bud","large_amethyst_bud","tuff","calcite","nether_sprouts","nether_wart","soul_sand" и т.д. (мапится на SoundType)
 *  mapColor / map_color — цвет на карте: "stone","dirt","wood","metal","plant","sand","wool","fire","snow","clay","dirt","grass","water","lava" и т.д. (MapColor)
 *  instabreak / insta_break / instabreakable — мгновенно ломается (как трава)
 *  requiresTool / requires_correct_tool — требует правильного инструмента
 *  offsetType / offset_type — "none","xz","xyz" (смещение модели, как у травы)
 *  copyFrom / copy_from / fullCopy — id блока для копирования свойств (например "minecraft:stone" -> ofFullCopy)
 *  noCollision / no_collision / collidable=false — отключить коллизию (проходимый блок, как трава)
 *  noOcclusion / no_occlusion / transparent — отключить окклюзию (не заслоняет свет, как стекло)
 *  collision / collidable — true/false, алиас для noCollision
 *  ignitedByLava / ignited_by_lava — воспламеняется от лавы
 *  tool / mineableTool / harvestTool — требуемый инструмент: "pickaxe","axe","shovel","hoe" (data/.../mineable/<tool>.json)
 *  tier / miningTier / level — уровень добычи: "wood","stone","iron","diamond","netherite" (data/.../needs_<tier>_tool.json), требует requiresTool=true
 *  shape / boxes / collisionShape — кастомная коллизия: {from={0,0,0},to={16,32,16}} или {0,0,0,16,32,16}, 32=2 блока высоты
 *  drops / drop / loot — лут блока (Fabric loot_table): nil=дроп себя, false/{}=ничего, "minecraft:diamond"=1 предмет,
 *                   {"minecraft:diamond","minecraft:stick"} — несколько предметов (каждый свой pool, все выпадут),
 *                   {{id="minecraft:diamond", count=3}, {id="minecraft:emerald", min=1, max=3}} — с количеством,
 *                   {{id="minecraft:diamond", weight=10}, {id="minecraft:emerald", weight=1}} — взвешенный случайный выбор (один pool, weight как на minecraft.wiki/w/Loot_table),
 *                   {{id="minecraft:diamond", conditions={{condition="minecraft:random_chance", chance=0.1}}}, {id="minecraft:stick", condition={condition="minecraft:survives_explosion"}}} — кастомные conditions/predicate (см. https://minecraft.wiki/w/Predicate),
 *                   "minecraft:apple" и т.д. Генерируется data/<ns>/loot_table/blocks/<path>.json (см. https://docs.fabricmc.net/develop/blocks/first-block#adding-block-drops)
 *  ore / oreGen / generation — генерация руды (https://wiki.fabricmc.net/tutorial:ores):
 *                   true/false или таблица { size=9, count=20, minY=-24, maxY=64, height="trapezoid", replace="stone_ore_replaceables", biomes="overworld" }
 *                   Генерирует data/<ns>/worldgen/configured_feature и placed_feature + BiomeModifications
 *  tags / tag / itemTags — теги предмета для рецептов (https://docs.fabricmc.net/develop/data-generation/tags):
 *                   "c:tin_ingots" или {"c:tin_ingots","c:ingots/tin"} или {{tag="c:tin_ingots"}}
 *                   Генерирует data/<tagNs>/tags/item/<tagPath>.json (и block/tags если блок)
 *  blockTags / block_tags — теги блока (аналогично, data/.../tags/block)
 */
class LuaContentSettings(
    var name: String? = null,
    var texture: String? = null,
    var model: String? = null,
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
    var friction: Float? = null,
    var noCollision: Boolean = false,
    var noOcclusion: Boolean = false,
    var sound: String? = null,
    var mapColor: String? = null,
    var instabreak: Boolean = false,
    var requiresCorrectTool: Boolean = false,
    var offsetType: String? = null,
    var copyFrom: String? = null,
    var ignitedByLava: Boolean? = null,
    // Требуемый инструмент и уровень добычи (для requiresCorrectToolForDrops)
    // tool: "pickaxe"/"axe"/"shovel"/"hoe" -> data/minecraft/tags/block/mineable/<tool>.json
    // tier: "wood"/"stone"/"iron"/"diamond"/"netherite"/"gold" -> data/minecraft/tags/block/needs_<tier>_tool.json
    var mineableTool: String? = null,
    var miningTier: String? = null,
    // Кастомная коллизия: список боксов [x1,y1,z1,x2,y2,z2] в координатах 0..32 (16=1 блок, 32=2 блока высоты)
    // Задается через shape / collisionShape / boxes : { {from={0,0,0},to={16,32,16}}, {0,0,0,16,16,16}, ... }
    var shapeBoxes: MutableList<DoubleArray>? = null,
    var textures: MutableMap<String, String>? = null,
    // Лут блока: nil = дроп себя (дефолт), пустой список = ничего не дропает, иначе список DropEntry
    var drops: MutableList<DropEntry>? = null,
    // Генерация руды: nil = без генерации, иначе настройки
    var ore: OreConfig? = null,
    // Теги для рецептов (см. https://docs.fabricmc.net/develop/data-generation/tags)
    var tags: MutableList<String>? = null, // item tags: data/<tagNs>/tags/item/<path>.json
    var blockTags: MutableList<String>? = null, // block tags: data/<ns>/tags/block/<path>.json
    // Контейнер для блока (https://docs.fabricmc.net/develop/blocks/block-containers + container-menus)
    // containerSize: 1..54, 9 = 1 ряд, 27 = сундук, 54 = двойной сундук; nil = обычный блок
    var containerSize: Int? = null,
    var containerTitle: String? = null,
    // Кастомные слоты: список {x,y} на каждый слот (1..size). Если nil — сетка 9xN с 8,18
    var containerSlots: MutableList<IntArray>? = null,
    // Текстура GUI: Identifier "minecraft:textures/gui/container/generic_54.png" или путь к файлу
    var containerTexture: String? = null
) : LuaUserdata(this) {

    data class OreConfig(
        var size: Int = 9,
        var discardChance: Float = 0.0f,
        var count: Int = 10,
        var minY: Int = -64,
        var maxY: Int = 64,
        var heightType: String = "trapezoid", // trapezoid | uniform
        var replace: String? = null, // "stone_ore_replaceables" | "deepslate_ore_replaceables" | "netherrack" | "end_stone" | tag/block id
        var biomes: String = "overworld", // overworld | nether | end | tag like "#minecraft:is_overworld"
        var featureId: String? = null // если nil — используется id блока
    )

    data class DropEntry(
        val id: String,
        val count: Int = 1,
        val countMin: Int? = null,
        val countMax: Int? = null,
        val weight: Int? = null, // null=гарантированный дроп (отдельный pool), число=взвешенный выбор в одном pool (см. minecraft.wiki/w/Loot_table#weight)
        val conditions: List<String>? = null // raw JSON объектов условий/predicate (см. https://minecraft.wiki/w/Predicate), добавляются в entry.conditions
    )

    override fun typename(): String = "content_settings"

    override fun tojstring(): String =
        "ContentSettings{name=$name, texture=$texture, model=$model, maxStackSize=$maxStackSize, rarity=$rarity" +
            (durability?.let { ", durability=$it" } ?: "") +
            (craftRemainder?.let { ", craftRemainder=$it" } ?: "") +
            (hardness?.let { ", hardness=$it" } ?: "") +
            (sound?.let { ", sound=$it" } ?: "") +
            (if (instabreak) ", instabreak" else "") +
            (if (requiresCorrectTool) ", requiresTool" else "") +
            (if (noCollision) ", noCollision" else "") +
            (if (noOcclusion) ", noOcclusion" else "") +
            (shapeBoxes?.let { ", shapeBoxes=${it.size}" } ?: "") +
            (drops?.let { ", drops=${it.size}" } ?: "") +
            (tags?.let { ", tags=${it.size}" } ?: "") +
            (blockTags?.let { ", blockTags=${it.size}" } ?: "") +
            (offsetType?.let { ", offset=$it" } ?: "") + "}"

    override fun get(key: LuaValue): LuaValue = when (key.tojstring()) {
        "name" -> name?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "texture" -> texture?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "model", "parent", "parentModel", "parent_model", "modelFile", "model_file", "modelJson", "model_json" -> model?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "maxStackSize" -> LuaValue.valueOf(maxStackSize)
        "fireResistant", "fire_resistant" -> LuaValue.valueOf(fireResistant)
        "rarity" -> rarity?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "durability" -> durability?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "craftRemainder", "craft_remainder" -> craftRemainder?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "enchantable" -> enchantable?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "useCooldown", "use_cooldown" -> useCooldown?.let { LuaValue.valueOf(it.toDouble()) } ?: LuaValue.NIL
        "hardness", "destroyTime", "destroy_time", "strength" -> hardness?.let { LuaValue.valueOf(it.toDouble()) } ?: LuaValue.NIL
        "resistance", "explosionResistance", "explosion_resistance" -> resistance?.let { LuaValue.valueOf(it.toDouble()) } ?: LuaValue.NIL
        "luminance", "lightLevel", "light_level" -> luminance?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "friction" -> friction?.let { LuaValue.valueOf(it.toDouble()) } ?: LuaValue.NIL
        "noCollision", "no_collision", "noCollission" -> LuaValue.valueOf(noCollision)
        "noOcclusion", "no_occlusion", "transparent", "translucent" -> LuaValue.valueOf(noOcclusion)
        "collision", "collidable", "hasCollision", "has_collision" -> LuaValue.valueOf(!noCollision)
        "sound", "soundType", "sound_type" -> sound?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "mapColor", "map_color", "color" -> mapColor?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "instabreak", "insta_break", "instabreakable" -> LuaValue.valueOf(instabreak)
        "requiresTool", "requires_tool", "requiresCorrectTool", "requires_correct_tool" -> LuaValue.valueOf(requiresCorrectTool)
        "offsetType", "offset_type", "offset" -> offsetType?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "copyFrom", "copy_from", "fullCopy", "full_copy", "copy" -> copyFrom?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "ignitedByLava", "ignited_by_lava" -> ignitedByLava?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "shape", "collisionShape", "collision_shape", "boxes", "collisionBoxes", "collision_boxes", "hitbox", "hitBox" -> shapeBoxes?.let { boxesToLua(it) } ?: LuaValue.NIL
        "tool", "mineableTool", "mineable_tool", "harvestTool", "harvest_tool" -> mineableTool?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "tier", "miningTier", "mining_tier", "miningLevel", "mining_level", "needsTier", "needs_tier" -> miningTier?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "textures", "textureMap", "texture_map" -> textures?.let { mapToLua(it) } ?: LuaValue.NIL
        "drops", "drop", "loot", "lootTable", "loot_table", "loot_table_blocks", "dropsList", "drops_list" -> drops?.let { dropsToLua(it) } ?: LuaValue.NIL
        "ore", "oreGen", "ore_gen", "generation", "worldgen", "worldGen", "vein" -> ore?.let { oreToLua(it) } ?: LuaValue.NIL
        "tags", "tag", "itemTags", "item_tags", "item_tag" -> tags?.let { tagsToLua(it) } ?: LuaValue.NIL
        "blockTags", "block_tags", "block_tag", "blockTag" -> blockTags?.let { tagsToLua(it) } ?: LuaValue.NIL
        "containerSize", "container_size", "size", "inventorySize", "inventory_size", "slotCount", "slot_count" -> containerSize?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "containerTitle", "container_title", "title", "menuTitle", "menu_title", "containerName" -> containerTitle?.let { LuaValue.valueOf(it) } ?: name?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        "containerSlots", "container_slots", "slots", "slotPositions", "slot_positions" -> containerSlots?.let { slotsToLua(it) } ?: LuaValue.NIL
        "containerTexture", "container_texture", "guiTexture", "gui_texture", "menuTexture", "menu_texture" -> containerTexture?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
        else -> super.get(key)
    }

    override fun set(key: LuaValue, value: LuaValue) {
        when (key.tojstring()) {
            "name" -> name = if (value.isnil()) null else Companion.readableString(value)
            "texture" -> texture = if (value.isnil()) null else value.tojstring()
            "model", "parent", "parentModel", "parent_model", "modelFile", "model_file", "modelJson", "model_json" -> model = if (value.isnil()) null else value.tojstring()
            "maxStackSize" -> maxStackSize = value.checkint()
            "fireResistant", "fire_resistant" -> fireResistant = value.toboolean()
            "rarity" -> rarity = if (value.isnil()) null else value.tojstring()
            "durability" -> durability = if (value.isnil()) null else value.checkint()
            "craftRemainder", "craft_remainder" -> craftRemainder = if (value.isnil()) null else value.tojstring()
            "enchantable" -> enchantable = if (value.isnil()) null else value.checkint()
            "useCooldown", "use_cooldown" -> useCooldown = if (value.isnil()) null else value.checkdouble().toFloat()
            "hardness", "destroyTime", "destroy_time", "strength" -> hardness = if (value.isnil()) null else value.checkdouble().toFloat()
            "resistance", "explosionResistance", "explosion_resistance" -> resistance = if (value.isnil()) null else value.checkdouble().toFloat()
            "luminance", "lightLevel", "light_level" -> luminance = if (value.isnil()) null else value.checkint().coerceIn(0, 15)
            "friction" -> friction = if (value.isnil()) null else value.checkdouble().toFloat()
            "noCollision", "no_collision", "noCollission" -> noCollision = value.toboolean()
            "noOcclusion", "no_occlusion", "transparent", "translucent" -> noOcclusion = value.toboolean()
            "collision", "collidable", "hasCollision", "has_collision" -> noCollision = !value.toboolean()
            "sound", "soundType", "sound_type" -> sound = if (value.isnil()) null else value.tojstring()
            "mapColor", "map_color", "color" -> mapColor = if (value.isnil()) null else value.tojstring()
            "instabreak", "insta_break", "instabreakable" -> instabreak = value.toboolean()
            "requiresTool", "requires_tool", "requiresCorrectTool", "requires_correct_tool" -> {
                if (value.isstring()) {
                    val s = value.tojstring().lowercase()
                    if (s in listOf("true","false")) requiresCorrectTool = s=="true"
                    else { miningTier = s; requiresCorrectTool = true }
                } else requiresCorrectTool = value.toboolean()
            }
            "offsetType", "offset_type", "offset" -> offsetType = if (value.isnil()) null else value.tojstring()
            "copyFrom", "copy_from", "fullCopy", "full_copy", "copy" -> copyFrom = if (value.isnil()) null else value.tojstring()
            "ignitedByLava", "ignited_by_lava" -> ignitedByLava = if (value.isnil()) null else value.toboolean()
            "shape", "collisionShape", "collision_shape", "boxes", "collisionBoxes", "collision_boxes", "hitbox", "hitBox" -> {
                if (value.isnil()) shapeBoxes = null else shapeBoxes = parseShapeValue(value)
            }
            "tool", "mineableTool", "mineable_tool", "harvestTool", "harvest_tool" -> mineableTool = if (value.isnil()) null else value.tojstring().lowercase()
            "tier", "miningTier", "mining_tier", "miningLevel", "mining_level", "needsTier", "needs_tier", "level" -> miningTier = if (value.isnil()) null else value.tojstring().lowercase()
            "textures", "textureMap", "texture_map" -> {
                if (value.isnil()) textures = null
                else if (value.istable()) {
                    val map = mutableMapOf<String, String>()
                    val table = value.checktable()
                    val keys = table.keys()
                    for (kObj in keys) {
                        val k = kObj as? LuaValue ?: LuaValue.valueOf(kObj.toString())
                        val v = table.get(k)
                        if (!v.isnil()) {
                            map[k.tojstring()] = v.tojstring()
                        }
                    }
                    textures = map
                }
            }
            "drops", "drop", "loot", "lootTable", "loot_table", "loot_table_blocks", "dropsList", "drops_list" -> {
                if (value.isnil()) drops = null
                else if (value.isboolean() && !value.toboolean()) drops = mutableListOf()
                else drops = parseDropsValue(value)
            }
            "ore", "oreGen", "ore_gen", "generation", "worldgen", "worldGen", "vein" -> {
                if (value.isnil()) ore = null
                else if (value.isboolean()) ore = if (value.toboolean()) OreConfig() else null
                else if (value.istable()) ore = parseOreValue(value)
                else if (value.isstring() && value.tojstring().equals("true", true)) ore = OreConfig()
            }
            "tags", "tag", "itemTags", "item_tags", "item_tag" -> {
                if (value.isnil()) tags = null
                else tags = parseTagsValue(value)
            }
            "blockTags", "block_tags", "block_tag", "blockTag" -> {
                if (value.isnil()) blockTags = null
                else blockTags = parseTagsValue(value)
            }
            "containerSize", "container_size", "size", "inventorySize", "inventory_size", "slotCount", "slot_count" -> {
                if (value.isnil()) containerSize = null else containerSize = value.checkint().coerceIn(1, 54)
            }
            "containerTitle", "container_title", "title", "menuTitle", "menu_title", "containerName", "container_name" -> {
                if (value.isnil()) containerTitle = null else containerTitle = readableString(value)
            }
            "containerSlots", "container_slots", "slots", "slotPositions", "slot_positions" -> {
                if (value.isnil()) containerSlots = null else containerSlots = parseSlotsValue(value)
            }
            "containerTexture", "container_texture", "guiTexture", "gui_texture", "menuTexture", "menu_texture" -> {
                if (value.isnil()) containerTexture = null else containerTexture = value.tojstring()
            }
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
        // copyFrom — полная копия свойств существующего блока (ofFullCopy)
        var props = if (copyFrom != null) {
            try {
                val id = Identifier.parse(copyFrom!!)
                val holder = BuiltInRegistries.BLOCK.get(id)
                if (holder.isPresent) BlockBehaviour.Properties.ofFullCopy(holder.get().value())
                else properties
            } catch (_: Exception) { properties }
        } else properties

        if (hardness != null || resistance != null) {
            val h = hardness ?: resistance ?: 1.0f
            val r = resistance ?: hardness ?: 1.0f
            props = props.strength(h, r)
        }
        luminance?.let { lvl -> props = props.lightLevel { lvl } }
        friction?.let { props = props.friction(it) }
        if (noCollision) props = props.noCollision()
        if (noOcclusion) props = props.noOcclusion()
        if (instabreak) props = props.instabreak()
        if (requiresCorrectTool) props = props.requiresCorrectToolForDrops()
        ignitedByLava?.let { if (it) props = props.ignitedByLava() }
        sound?.let { s ->
            try {
                val st = net.minecraft.world.level.block.SoundType::class.java.getField(s.uppercase()).get(null) as net.minecraft.world.level.block.SoundType
                props = props.sound(st)
            } catch (_: Exception) {
                // пробуем по имени с _ как в SoundType.* и по алиасам
                val aliasMap = mapOf(
                    "grass" to "GRASS", "dirt" to "GRASS", "wood" to "WOOD", "stone" to "STONE",
                    "metal" to "METAL", "glass" to "GLASS", "wool" to "WOOL", "sand" to "SAND",
                    "snow" to "SNOW", "powder_snow" to "POWDER_SNOW", "ladder" to "LADDER",
                    "anvil" to "ANVIL", "slime" to "SLIME_BLOCK", "honey" to "HONEY_BLOCK",
                    "wet_grass" to "WET_GRASS", "coral" to "CORAL_BLOCK", "bamboo" to "BAMBOO",
                    "soul_sand" to "SOUL_SAND", "soul_soil" to "SOUL_SOIL", "basalt" to "BASALT",
                    "wart" to "WART_BLOCK", "nether_wood" to "NETHER_WOOD", "stem" to "STEM",
                    "nylium" to "NYLIUM", "fungus" to "FUNGUS", "roots" to "ROOTS",
                    "chain" to "CHAIN", "copper" to "COPPER", "amethyst" to "AMETHYST",
                    "tuff" to "TUFF", "calcite" to "CALCITE", "dripstone" to "DRIPSTONE_BLOCK",
                    "sculk" to "SCULK", "bone" to "BONE_BLOCK", "lantern" to "LANTERN"
                )
                val key = aliasMap[s.lowercase()] ?: s.uppercase()
                try {
                    val st2 = net.minecraft.world.level.block.SoundType::class.java.getField(key).get(null) as net.minecraft.world.level.block.SoundType
                    props = props.sound(st2)
                } catch (_: Exception) {}
            }
        }
        mapColor?.let { c ->
            try {
                // MapColor — ищем поле по имени (например STONE, COLOR_STONE и т.д.)
                var mc: Any? = null
                for (field in net.minecraft.world.level.material.MapColor::class.java.fields) {
                    if (field.name.equals(c, ignoreCase = true) || field.name.equals("COLOR_${c.uppercase()}", ignoreCase = true)) {
                        mc = field.get(null)
                        break
                    }
                }
                if (mc != null) {
                    val m = mc as net.minecraft.world.level.material.MapColor
                    props = props.mapColor(m)
                }
            } catch (_: Exception) {}
        }
        offsetType?.let { o ->
            try {
                val ot = BlockBehaviour.OffsetType.valueOf(o.uppercase())
                props = props.offsetType(ot)
            } catch (_: Exception) {
                when (o.lowercase()) {
                    "xz", "x_z" -> props = props.offsetType(BlockBehaviour.OffsetType.XZ)
                    "xyz", "x_y_z" -> props = props.offsetType(BlockBehaviour.OffsetType.XYZ)
                    else -> props = props.offsetType(BlockBehaviour.OffsetType.NONE)
                }
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
          * Строка из Lua-значения: component/builder/строка -> текст компонента
          * (иначе получилось бы "literal{Pizza}").
          */
        fun readableString(v: LuaValue): String =
            v.toComponent()?.string ?: v.tojstring()

        // ── shape helpers ──
        fun parseShapeValue(v: LuaValue?): MutableList<DoubleArray>? {
            if (v == null || v.isnil()) return null
            // Ожидаем таблицу. Поддерживаем:
            //  shape = {from={0,0,0}, to={16,32,16}}  — один бокс
            //  shape = { {from={0,0,0}, to={16,16,16}}, {from={0,16,0}, to={16,32,16}} }
            //  shape = { {0,0,0, 16,32,16}, {0,32,0, 16,48,16} }
            //  shape = {0,0,0, 16,32,16} — один бокс плоско
            if (!v.istable()) return null
            // одиночный бокс вида {from=..., to=...}
            if (!v.get("from").isnil() || !v.get("to").isnil()) {
                parseSingleBox(v)?.let { return mutableListOf(it) }
            }
            val out = mutableListOf<DoubleArray>()
            val n = v.length()
            if (n > 0) {
                for (i in 1..n) {
                    val e = v.get(i)
                    if (e.isnil()) continue
                    if (e.istable()) {
                        // либо 6 чисел подряд, либо from/to
                        if (!e.get("from").isnil() || !e.get("to").isnil()) {
                            parseSingleBox(e)?.let { out.add(it) }
                        } else {
                            // пробуем как массив 6 чисел
                            val arr = mutableListOf<Double>()
                            for (j in 1..e.length()) {
                                val num = e.get(j)
                                if (num.isnumber()) arr.add(num.todouble())
                            }
                            if (arr.size == 6) out.add(arr.toDoubleArray())
                            else if (e.length() == 0 && e.get(1).isnil()) {
                                // возможно пусто
                            }
                        }
                    } else if (e.isnumber()) {
                        // плоский массив на верхнем уровне {0,0,0,16,32,16}
                        val arr = mutableListOf<Double>()
                        for (j in 1..n) arr.add(v.get(j).todouble())
                        if (arr.size == 6) return mutableListOf(arr.toDoubleArray())
                        break
                    }
                }
                // fallback: если передали {0,0,0,16,32,16} как v с length 6 и без вложенности
                if (out.isEmpty() && n == 6) {
                    val arr = (1..6).map { v.get(it).todouble() }.toDoubleArray()
                    if (arr.all { !it.isNaN() }) out.add(arr)
                }
            } else {
                // возможно {from={...}, to={...}} уже обработали, иначе пробуем как 6 чисел по ключам 1..6
                parseSingleBox(v)?.let { out.add(it) }
            }
            return if (out.isEmpty()) null else out
        }

        private fun parseSingleBox(tbl: LuaValue): DoubleArray? {
            // from/to могут быть таблицами {x,y,z} или {1=x,2=y,3=z}
            fun readVec(key: String): DoubleArray? {
                val vec = tbl.get(key)
                if (vec.isnil()) return null
                if (vec.istable()) {
                    val x = vec.get(1).takeIf { it.isnumber() }?.todouble() ?: vec.get("x").takeIf { it.isnumber() }?.todouble() ?: vec.get("1").takeIf { it.isnumber() }?.todouble()
                    val y = vec.get(2).takeIf { it.isnumber() }?.todouble() ?: vec.get("y").takeIf { it.isnumber() }?.todouble() ?: vec.get("2").takeIf { it.isnumber() }?.todouble()
                    val z = vec.get(3).takeIf { it.isnumber() }?.todouble() ?: vec.get("z").takeIf { it.isnumber() }?.todouble() ?: vec.get("3").takeIf { it.isnumber() }?.todouble()
                    if (x != null && y != null && z != null) return doubleArrayOf(x, y, z)
                } else if (vec.isnumber()) {
                    // неожиданно
                }
                return null
            }
            val from = readVec("from") ?: readVec("min") ?: readVec("start")
            val to = readVec("to") ?: readVec("max") ?: readVec("end")
            if (from != null && to != null) {
                return doubleArrayOf(from[0], from[1], from[2], to[0], to[1], to[2])
            }
            // плоский {x1,y1,z1,x2,y2,z2}
            if (tbl.length() >= 6) {
                val arr = (1..6).mapNotNull { idx -> tbl.get(idx).takeIf { it.isnumber() }?.todouble() }
                if (arr.size == 6) return arr.toDoubleArray()
            }
            return null
        }

        fun boxesToLua(boxes: List<DoubleArray>): LuaValue {
            val tbl = LuaValue.tableOf()
            for ((idx, b) in boxes.withIndex()) {
                val boxTbl = LuaValue.tableOf()
                // храним как {from={x1,y1,z1}, to={x2,y2,z2}}
                val from = LuaValue.tableOf()
                from.set(1, LuaValue.valueOf(b[0])); from.set(2, LuaValue.valueOf(b[1])); from.set(3, LuaValue.valueOf(b[2]))
                val to = LuaValue.tableOf()
                to.set(1, LuaValue.valueOf(b[3])); to.set(2, LuaValue.valueOf(b[4])); to.set(3, LuaValue.valueOf(b[5]))
                boxTbl.set("from", from); boxTbl.set("to", to)
                tbl.set(idx + 1, boxTbl)
            }
            return tbl
        }

        fun mapToLua(map: Map<String, String>): LuaValue {
            val tbl = LuaValue.tableOf()
            for ((k, v) in map) {
                tbl.set(k, LuaValue.valueOf(v))
            }
            return tbl
        }

        // ── json helpers для conditions/predicate ──
        private fun escapeJson(s: String): String = s
            .replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")

        fun luaValueToJson(v: LuaValue): String {
            if (v.isnil()) return "null"
            if (v.isboolean()) return if (v.toboolean()) "true" else "false"
            if (v.isnumber()) {
                val d = v.todouble()
                return if (d % 1.0 == 0.0) v.toint().toString() else d.toString()
            }
            if (v.isstring()) return "\"${escapeJson(v.tojstring())}\""
            if (v.istable()) {
                val n = v.length()
                val tbl = v.checktable()
                // если есть числовые последовательные ключи и нет именованных — массив
                val hasNamed = run {
                    for (kObj in tbl.keys()) {
                        val k = kObj as? LuaValue ?: LuaValue.valueOf(kObj.toString())
                        val ks = k.tojstring()
                        if (ks.toIntOrNull() == null) return@run true
                        val iv = ks.toIntOrNull() ?: -1
                        if (iv < 1 || iv > n) return@run true
                    }
                    false
                }
                if (n > 0 && !hasNamed) {
                    val arr = (1..n).joinToString(",") { luaValueToJson(v.get(it)) }
                    return "[$arr]"
                }
                // объект
                val parts = mutableListOf<String>()
                for (kObj in tbl.keys()) {
                    val k = kObj as? LuaValue ?: LuaValue.valueOf(kObj.toString())
                    val ks = k.tojstring()
                    if (ks.toIntOrNull() != null && n > 0) continue
                    val vv = tbl.get(k)
                    if (vv.isnil()) continue
                    parts.add("\"${escapeJson(ks)}\":${luaValueToJson(vv)}")
                }
                return "{${parts.joinToString(",")}}"
            }
            return "\"${escapeJson(v.tojstring())}\""
        }

        fun parseConditions(v: LuaValue, rawTable: LuaValue): List<String>? {
            // ищет condition(s)/predicate(s)/when в таблице дропа
            var src: LuaValue? = null
            for (k in arrayOf("conditions", "condition", "predicates", "predicate", "when", "requires", "if")) {
                val cv = rawTable.get(k)
                if (!cv.isnil()) { src = cv; break }
            }
            if (src == null) return null
            if (src.isstring()) {
                val s = src.tojstring().trim()
                if (s.isEmpty()) return null
                // если строка уже JSON — вернем как есть, иначе считаем id условия
                return if (s.startsWith("{")) listOf(s) else listOf("\"${escapeJson(s)}\"")
            }
            if (!src.istable()) return null
            // один объект с полем condition?
            if (!src.get("condition").isnil() || !src.get("predicate").isnil()) {
                return listOf(luaValueToJson(src))
            }
            val n = src.length()
            if (n > 0) {
                val out = mutableListOf<String>()
                for (i in 1..n) {
                    val e = src.get(i)
                    if (e.isnil()) continue
                    if (e.isstring()) {
                        val s = e.tojstring().trim()
                        out.add(if (s.startsWith("{")) s else "\"${escapeJson(s)}\"")
                    } else {
                        out.add(luaValueToJson(e))
                    }
                }
                return if (out.isEmpty()) null else out
            }
            // таблица без length но с ключами — один объект
            return listOf(luaValueToJson(src))
        }

        // ── drops helpers ──
        fun parseDropEntry(v: LuaValue): DropEntry? {
            if (v.isstring()) {
                val id = v.tojstring().trim()
                if (id.isEmpty() || id.equals("none", true) || id.equals("empty", true)) return null
                return try { Identifier.parse(id); DropEntry(id) } catch (_: Exception) { null }
            }
            if (!v.istable()) return null
            // id может быть в "id"/"name"/"item"/"key"
            var id: String? = null
            for (k in arrayOf("id", "name", "item", "key", "value")) {
                val vv = v.get(k)
                if (!vv.isnil() && vv.isstring()) { id = vv.tojstring().trim(); break }
            }
            if (id == null) {
                // поддержка короткой записи { "minecraft:diamond", count=2 } где id хранится в [1]
                val first = v.get(1)
                if (first.isstring()) id = first.tojstring().trim()
            }
            if (id == null || id.isEmpty()) return null
            try { Identifier.parse(id) } catch (_: Exception) { return null }
            // count / amount
            var count = 1
            var countMin: Int? = null
            var countMax: Int? = null
            for (k in arrayOf("count", "amount", "quantity", "number")) {
                val cv = v.get(k)
                if (cv.isnumber()) { count = cv.toint().coerceIn(1, 64); break }
            }
            // min / max для диапазона
            for (k in arrayOf("min", "minCount", "min_count", "minimum")) {
                val mv = v.get(k)
                if (mv.isnumber()) { countMin = mv.toint().coerceIn(1, 64); break }
            }
            for (k in arrayOf("max", "maxCount", "max_count", "maximum")) {
                val mv = v.get(k)
                if (mv.isnumber()) { countMax = mv.toint().coerceIn(1, 64); break }
            }
            if (countMin != null && countMax != null && countMin > countMax) {
                val t = countMin; countMin = countMax; countMax = t
            }
            var weight: Int? = null
            for (k in arrayOf("weight", "w", "chance_weight")) {
                val wv = v.get(k)
                if (wv.isnumber()) { weight = wv.toint().coerceIn(1, 1_000_000); break }
            }
            val conditions = parseConditions(v, v)
            return DropEntry(id, count, countMin, countMax, weight, conditions)
        }

        fun parseDropsValue(v: LuaValue?): MutableList<DropEntry>? {
            if (v == null || v.isnil()) return null
            if (v.isboolean() && !v.toboolean()) return mutableListOf()
            if (v.isstring()) {
                val s = v.tojstring().trim()
                if (s.isEmpty() || s.equals("none", true) || s.equals("empty", true)) return mutableListOf()
                return try { Identifier.parse(s); mutableListOf(DropEntry(s)) } catch (_: Exception) { null }
            }
            if (!v.istable()) return null
            // если таблица — одиночный объект с id
            val hasId = !v.get("id").isnil() || !v.get("name").isnil() || !v.get("item").isnil()
            if (hasId && v.length() == 0) {
                val e = parseDropEntry(v) ?: return mutableListOf()
                return mutableListOf(e)
            }
            val out = mutableListOf<DropEntry>()
            val n = v.length()
            if (n > 0) {
                for (i in 1..n) {
                    val e = v.get(i)
                    if (e.isnil()) continue
                    if (e.isstring()) {
                        val id = e.tojstring().trim()
                        if (id.isEmpty()) continue
                        try { Identifier.parse(id); out.add(DropEntry(id)) } catch (_: Exception) {}
                    } else if (e.istable()) {
                        parseDropEntry(e)?.let { out.add(it) }
                    }
                }
                // если передали {id="...", count=2} но с length>0 — уже обработано выше как одиночный? fallback
                if (out.isEmpty() && hasId) parseDropEntry(v)?.let { out.add(it) }
                return out
            } else {
                // таблица без числовых ключей, но возможно пустая -> считаем пустым дропом
                if (v.get(1).isnil() && hasId) {
                    parseDropEntry(v)?.let { return mutableListOf(it) }
                }
                return mutableListOf()
            }
        }

        fun dropsToLua(drops: List<DropEntry>): LuaValue {
            val tbl = LuaValue.tableOf()
            for ((idx, d) in drops.withIndex()) {
                val e = LuaValue.tableOf()
                e.set("id", LuaValue.valueOf(d.id))
                if (d.countMin != null && d.countMax != null) {
                    e.set("min", LuaValue.valueOf(d.countMin))
                    e.set("max", LuaValue.valueOf(d.countMax))
                } else if (d.count != 1) {
                    e.set("count", LuaValue.valueOf(d.count))
                }
                d.weight?.let { e.set("weight", LuaValue.valueOf(it)) }
                d.conditions?.let { conds ->
                    val arr = LuaValue.tableOf()
                    for ((ci, cjson) in conds.withIndex()) {
                        // храним как строку JSON — при желании можно распарсить в Lua через json.decode
                        arr.set(ci + 1, LuaValue.valueOf(cjson))
                    }
                    e.set("conditions", arr)
                    if (conds.size == 1) e.set("condition", LuaValue.valueOf(conds[0]))
                }
                tbl.set(idx + 1, e)
            }
            return tbl
        }

        // ── ore helpers ──
        fun parseOreValue(v: LuaValue): OreConfig? {
            if (v.isboolean()) return if (v.toboolean()) OreConfig() else null
            if (!v.istable()) return null
            val cfg = OreConfig()
            fun num(vararg keys: String): Double? {
                for (k in keys) { val vv = v.get(k); if (vv.isnumber()) return vv.todouble() }
                return null
            }
            fun str(vararg keys: String): String? {
                for (k in keys) { val vv = v.get(k); if (!vv.isnil() && vv.isstring()) return vv.tojstring() }
                return null
            }
            num("size", "veinSize", "vein_size")?.let { cfg.size = it.toInt().coerceIn(1, 64) }
            num("count", "veinsPerChunk", "veins_per_chunk", "veins")?.let { cfg.count = it.toInt().coerceIn(1, 128) }
            num("minY", "min_y", "min", "yMin")?.let { cfg.minY = it.toInt().coerceIn(-64, 320) }
            num("maxY", "max_y", "max", "yMax")?.let { cfg.maxY = it.toInt().coerceIn(-64, 320) }
            str("height", "heightType", "height_type")?.let { cfg.heightType = it.lowercase() }
            str("replace", "target", "replaceable", "replaceTag", "replace_tag")?.let { cfg.replace = it }
            str("biomes", "biome", "dimension", "dim")?.let { cfg.biomes = it.lowercase() }
            str("feature", "featureId", "feature_id", "id")?.let { cfg.featureId = it }
            num("discardChance", "discard_chance", "discardChanceOnAirExposure", "discard_chance_on_air_exposure")?.let { cfg.discardChance = it.toFloat().coerceIn(0f, 1f) }
            if (cfg.minY > cfg.maxY) { val t = cfg.minY; cfg.minY = cfg.maxY; cfg.maxY = t }
            return cfg
        }

        fun oreToLua(cfg: OreConfig): LuaValue {
            val t = LuaValue.tableOf()
            t.set("size", LuaValue.valueOf(cfg.size))
            t.set("count", LuaValue.valueOf(cfg.count))
            t.set("minY", LuaValue.valueOf(cfg.minY))
            t.set("maxY", LuaValue.valueOf(cfg.maxY))
            t.set("height", LuaValue.valueOf(cfg.heightType))
            t.set("biomes", LuaValue.valueOf(cfg.biomes))
            t.set("discardChance", LuaValue.valueOf(cfg.discardChance.toDouble()))
            cfg.replace?.let { t.set("replace", LuaValue.valueOf(it)) }
            cfg.featureId?.let { t.set("featureId", LuaValue.valueOf(it)) }
            return t
        }

        // ── tags helpers ──
        fun parseTagsValue(v: LuaValue?): MutableList<String>? {
            if (v == null || v.isnil()) return null
            if (v.isstring()) {
                val s = v.tojstring().trim()
                if (s.isEmpty()) return mutableListOf()
                return try { Identifier.parse(s); mutableListOf(s) } catch (_: Exception) { null }
            }
            if (v.isboolean()) return if (v.toboolean()) mutableListOf() else null
            if (!v.istable()) return null
            // одиночный объект {tag="c:tin_ingots"} ?
            for (k in arrayOf("tag", "id", "name", "value")) {
                val vv = v.get(k)
                if (!vv.isnil() && vv.isstring()) {
                    val s = vv.tojstring().trim()
                    return try { Identifier.parse(s); mutableListOf(s) } catch (_: Exception) { null }
                }
            }
            val out = mutableListOf<String>()
            val n = v.length()
            if (n > 0) {
                for (i in 1..n) {
                    val e = v.get(i)
                    if (e.isnil()) continue
                    if (e.isstring()) {
                        val s = e.tojstring().trim()
                        try { Identifier.parse(s); out.add(s) } catch (_: Exception) {}
                    } else if (e.istable()) {
                        for (k in arrayOf("tag", "id", "name", "value")) {
                            val vv = e.get(k)
                            if (!vv.isnil() && vv.isstring()) {
                                val s = vv.tojstring().trim()
                                try { Identifier.parse(s); out.add(s); break } catch (_: Exception) {}
                            }
                        }
                        if (e.get(1).isstring() && out.size < i) {
                            val s = e.get(1).tojstring().trim()
                            try { Identifier.parse(s); out.add(s) } catch (_: Exception) {}
                        }
                    }
                }
                return out
            }
            // пустая таблица -> пустой список
            return mutableListOf()
        }

        fun tagsToLua(list: List<String>): LuaValue {
            val t = LuaValue.tableOf()
            for ((i, s) in list.withIndex()) t.set(i + 1, LuaValue.valueOf(s))
            return t
        }

        // ── container helpers ──
        fun parseSlotsValue(v: LuaValue?): MutableList<IntArray>? {
            if (v == null || v.isnil()) return null
            if (!v.istable()) return null
            val out = mutableListOf<IntArray>()
            val n = v.length()
            if (n > 0) {
                for (i in 1..n) {
                    val e = v.get(i)
                    if (e.isnil()) continue
                    if (e.istable()) {
                        // supports {x,y} or {x=.., y=..} or {1=x,2=y}
                        val x = e.get(1).takeIf { it.isnumber() }?.toint()
                            ?: e.get("x").takeIf { it.isnumber() }?.toint()
                            ?: e.get("1").takeIf { it.isnumber() }?.toint()
                        val y = e.get(2).takeIf { it.isnumber() }?.toint()
                            ?: e.get("y").takeIf { it.isnumber() }?.toint()
                            ?: e.get("2").takeIf { it.isnumber() }?.toint()
                        if (x != null && y != null) out.add(intArrayOf(x, y))
                    } else if (e.isnumber()) {
                        // flat array {x1,y1,x2,y2,...} pairs?
                        // handled outside, skip
                    }
                }
                // also handle flat {x1,y1,x2,y2} as pairs if no nested tables succeeded
                if (out.isEmpty() && n % 2 == 0) {
                    var ok = true
                    for (j in 1..n step 2) { if (!v.get(j).isnumber() || !v.get(j+1).isnumber()) { ok=false; break } }
                    if (ok) {
                        for (j in 1..n step 2) out.add(intArrayOf(v.get(j).toint(), v.get(j+1).toint()))
                    }
                }
            } else {
                // single {x,y}
                val x = v.get(1).takeIf { it.isnumber() }?.toint() ?: v.get("x").takeIf { it.isnumber() }?.toint()
                val y = v.get(2).takeIf { it.isnumber() }?.toint() ?: v.get("y").takeIf { it.isnumber() }?.toint()
                if (x != null && y != null) out.add(intArrayOf(x, y))
            }
            return if (out.isEmpty()) null else out
        }

        fun slotsToLua(list: List<IntArray>): LuaValue {
            val t = LuaValue.tableOf()
            for ((i, arr) in list.withIndex()) {
                val e = LuaValue.tableOf()
                e.set(1, LuaValue.valueOf(arr[0]))
                e.set(2, LuaValue.valueOf(arr[1]))
                e.set("x", LuaValue.valueOf(arr[0]))
                e.set("y", LuaValue.valueOf(arr[1]))
                t.set(i+1, e)
            }
            return t
        }

        /**
          * Собирает настройки из Lua-таблицы. Допустимые ключи (snake_case или camelCase):
          * name, texture, model/parent, maxStackSize / max_stack_size, fireResistant / fire_resistant,
          * rarity, durability, craftRemainder / craft_remainder, enchantable,
          * useCooldown / use_cooldown, hardness/destroyTime, resistance, luminance, friction,
          * sound, mapColor, instabreak, requiresTool, offsetType, copyFrom, noCollision, noOcclusion,
          * shape/boxes/collisionShape (см. документацию выше: {from={0,0,0},to={16,32,16}}).
          */
        fun fromTable(table: LuaValue?): LuaContentSettings {
            val settings = LuaContentSettings()
            if (table == null || !table.istable()) return settings

            fun str(vararg keys: String): String? {
                for (k in keys) {
                    val v = table.get(k)
                    if (!v.isnil()) return readableString(v)
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

            fun bool(vararg keys: String): Boolean? {
                for (k in keys) {
                    val v = table.get(k)
                    if (!v.isnil()) return v.toboolean()
                }
                return null
            }

            settings.name = str("name")
            settings.texture = str("texture")
            settings.model = str("model", "parent", "parentModel", "parent_model", "modelFile", "model_file", "modelJson", "model_json")
            num("maxStackSize", "max_stack_size")?.let { settings.maxStackSize = it.toInt() }
            table.get("fireResistant").takeIf { !it.isnil() }?.let { settings.fireResistant = it.toboolean() }
                ?: table.get("fire_resistant").takeIf { !it.isnil() }?.let { settings.fireResistant = it.toboolean() }
            settings.rarity = str("rarity")
            num("durability")?.let { settings.durability = it.toInt() }
            settings.craftRemainder = str("craftRemainder", "craft_remainder")
            num("enchantable")?.let { settings.enchantable = it.toInt() }
            num("useCooldown", "use_cooldown")?.let { settings.useCooldown = it.toFloat() }
            num("hardness", "destroyTime", "destroy_time", "strength")?.let { settings.hardness = it.toFloat() }
            num("resistance", "explosionResistance", "explosion_resistance")?.let { settings.resistance = it.toFloat() }
            num("luminance", "lightLevel", "light_level")?.let { settings.luminance = it.toInt().coerceIn(0, 15) }
            num("friction")?.let { settings.friction = it.toFloat() }
            bool("noCollision", "no_collision", "noCollission")?.let { settings.noCollision = it }
            bool("collision", "collidable", "hasCollision", "has_collision")?.let { settings.noCollision = !it }
            bool("noOcclusion", "no_occlusion", "transparent", "translucent")?.let { settings.noOcclusion = it }
            settings.sound = str("sound", "soundType", "sound_type")
            settings.mapColor = str("mapColor", "map_color", "color")
            bool("instabreak", "insta_break", "instabreakable")?.let { settings.instabreak = it }
            bool("requiresTool", "requires_tool", "requiresCorrectTool", "requires_correct_tool")?.let { settings.requiresCorrectTool = it }
            settings.offsetType = str("offsetType", "offset_type", "offset")
            settings.copyFrom = str("copyFrom", "copy_from", "fullCopy", "full_copy", "copy")
            bool("ignitedByLava", "ignited_by_lava")?.let { settings.ignitedByLava = it }
            // shape: пробуем каждый ключ
            for (k in arrayOf("shape", "collisionShape", "collision_shape", "boxes", "collisionBoxes", "collision_boxes", "hitbox", "hitBox")) {
                val v = table.get(k)
                if (!v.isnil()) {
                    parseShapeValue(v)?.let { settings.shapeBoxes = it; break }
                }
            }
            // tool / tier: строковые, но requiresTool может быть строкой-tier
            settings.mineableTool = str("tool", "mineableTool", "mineable_tool", "harvestTool", "harvest_tool")
            settings.miningTier = str("tier", "miningTier", "mining_tier", "miningLevel", "mining_level", "needsTier", "needs_tier", "level")
            // textures: table of key -> file path
            val texturesTbl = table.get("textures")
            if (!texturesTbl.isnil() && texturesTbl.istable()) {
                val map = mutableMapOf<String, String>()
                val tbl = texturesTbl.checktable()
                val keys = tbl.keys()
                for (kObj in keys) {
                    val k = kObj as? LuaValue ?: LuaValue.valueOf(kObj.toString())
                    val v = tbl.get(k)
                    if (!v.isnil()) {
                        map[k.tojstring()] = v.tojstring()
                    }
                }
                settings.textures = map
            }
            // алиас: requiresTool = "iron" -> tier
            val reqToolStr = table.get("requiresTool").takeIf { it.isstring() }?.tojstring() ?: table.get("requires_tool").takeIf { it.isstring() }?.tojstring() ?: table.get("requiresCorrectTool").takeIf { it.isstring() }?.tojstring()
            if (reqToolStr != null && settings.miningTier == null) {
                val lower = reqToolStr.lowercase()
                if (lower in listOf("wood","wooden","stone","iron","diamond","netherite","gold","golden","copper")) {
                    settings.miningTier = lower
                    settings.requiresCorrectTool = true
                }
            }
            // drops / loot
            var dropsVal: LuaValue? = null
            for (k in arrayOf("drops", "drop", "loot", "lootTable", "loot_table", "dropsList", "drops_list")) {
                val v = table.get(k)
                if (!v.isnil()) { dropsVal = v; break }
            }
            if (dropsVal != null) {
                if (dropsVal.isboolean() && !dropsVal.toboolean()) settings.drops = mutableListOf()
                else settings.drops = parseDropsValue(dropsVal)
            }
            // ore
            var oreVal: LuaValue? = null
            for (k in arrayOf("ore", "oreGen", "ore_gen", "generation", "worldgen", "worldGen", "vein")) {
                val v = table.get(k)
                if (!v.isnil()) { oreVal = v; break }
            }
            if (oreVal != null) {
                if (oreVal.isboolean()) settings.ore = if (oreVal.toboolean()) OreConfig() else null
                else if (oreVal.istable()) settings.ore = parseOreValue(oreVal)
            }
            // tags
            var tagsVal: LuaValue? = null
            for (k in arrayOf("tags", "tag", "itemTags", "item_tags", "item_tag")) {
                val v = table.get(k)
                if (!v.isnil()) { tagsVal = v; break }
            }
            if (tagsVal != null) settings.tags = parseTagsValue(tagsVal)
            var blockTagsVal: LuaValue? = null
            for (k in arrayOf("blockTags", "block_tags", "block_tag", "blockTag")) {
                val v = table.get(k)
                if (!v.isnil()) { blockTagsVal = v; break }
            }
            if (blockTagsVal != null) settings.blockTags = parseTagsValue(blockTagsVal)
            // container
            for (k in arrayOf("containerSize", "container_size", "size", "inventorySize", "inventory_size", "slotCount", "slot_count")) {
                val v = table.get(k)
                if (!v.isnil() && v.isnumber()) { settings.containerSize = v.toint().coerceIn(1,54); break }
            }
            for (k in arrayOf("containerTitle", "container_title", "title", "menuTitle", "menu_title", "containerName", "container_name")) {
                val v = table.get(k)
                if (!v.isnil() && v.isstring()) { settings.containerTitle = readableString(v); break }
            }
            for (k in arrayOf("containerSlots", "container_slots", "slots", "slotPositions", "slot_positions")) {
                val v = table.get(k)
                if (!v.isnil()) { parseSlotsValue(v)?.let { settings.containerSlots = it; break } }
            }
            for (k in arrayOf("containerTexture", "container_texture", "guiTexture", "gui_texture", "menuTexture", "menu_texture")) {
                val v = table.get(k)
                if (!v.isnil() && v.isstring()) { settings.containerTexture = v.tojstring(); break }
            }
            return settings
        }
    }
}
