package com.nekiplay.neoscripts.client.sugar

import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaBlockState
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaComponent
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaComponentBuilder
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaEntity
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaItemStack
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.LuaTransform
import com.mojang.math.Transformation
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaAxis
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaAxisDirection
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaChunkPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaDirection
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaMutableBlockPos
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.core.LuaVector3d
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaBox
import com.nekiplay.neoscripts.common.features.lua.objects.datatypes.phys.LuaRaycast
import com.nekiplay.neoscripts.common.features.lua.objects.misc.LuaEntityType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.luaj.vm2.LuaValue

// ============================================================================
// Расширения LuaValue для проверки и приведения Lua-дататайпов к Java-типам.
// Каждый toX() возвращает null, если значение не того типа;
// каждый isX() проверяет и обёртку (Lua*), и сырой userdata.
// ============================================================================

// --- ItemStack ---

/** Проверка: обёртка itemstack или сырой ItemStack. */
fun LuaValue.isItem(): Boolean =
    this is LuaItemStack || isuserdata(ItemStack::class.java)

/** Приведение к ItemStack (возвращает копию стака). */
fun LuaValue.toItem(): ItemStack? = when (this) {
    is LuaItemStack -> stack.copy()
    else -> touserdata(ItemStack::class.java)?.copy()
}

// --- BlockState ---

/** Проверка: blockstate или сырой BlockState. */
fun LuaValue.isBlock(): Boolean =
    this is LuaBlockState || isuserdata(BlockState::class.java)

/** Приведение к BlockState. */
fun LuaValue.toBlock(): BlockState? = when (this) {
    is LuaBlockState -> blockState
    else -> touserdata(BlockState::class.java)
}

// --- Vec3 ---

/** Проверка: вектор (vector3). */
fun LuaValue.isVector(): Boolean =
    this is LuaVector3d || isuserdata(Vec3::class.java)

/** Приведение к Vec3. */
fun LuaValue.toVector(): Vec3? = when (this) {
    is LuaVector3d -> location
    else -> touserdata(Vec3::class.java)
}

// --- Entity ---

/** Проверка: сущность или сырой Entity. */
fun LuaValue.isEntity(): Boolean =
    this is LuaEntity || isuserdata(Entity::class.java)

/** Приведение к Entity. */
fun LuaValue.toEntity(): Entity? = when (this) {
    is LuaEntity -> entity
    else -> touserdata(Entity::class.java)
}

/** Приведение к конкретному типу сущности (Player, Display.ItemDisplay, ...). */
inline fun <reified T : Entity> LuaValue.toEntityAs(): T? = toEntity() as? T

/** Проверка: сущность заданного типа. */
inline fun <reified T : Entity> LuaValue.isEntityOf(): Boolean = toEntityAs<T>() != null

// --- Text component ---

/** Проверка: компонент текста (component / componentbuilder). */
fun LuaValue.isComponent(): Boolean =
    this is LuaComponent || this is LuaComponentBuilder || isuserdata(Component::class.java)

/**
 * Приведение к Component.
 * Дополнительно принимает обычную строку -> Component.literal(...).
 */
fun LuaValue.toComponent(): Component? = when (this) {
    is LuaComponent -> component.copy()
    is LuaComponentBuilder -> buildComponent()
    isuserdata(Component::class.java) -> touserdata(Component::class.java)
    isstring() -> Component.literal(tojstring())
    else -> null
}

// --- BlockPos ---

/** Проверка: позиция блока (blockpos, включая mutable). */
fun LuaValue.isBlockPos(): Boolean =
    this is LuaBlockPos || this is LuaMutableBlockPos || isuserdata(BlockPos::class.java)

/** Приведение к BlockPos. */
fun LuaValue.toBlockPos(): BlockPos? = when (this) {
    is LuaMutableBlockPos -> pos
    is LuaBlockPos -> pos
    else -> touserdata(BlockPos::class.java)
}

// --- Direction / Axis ---

/** Проверка: направление (direction). */
fun LuaValue.isDirection(): Boolean =
    this is LuaDirection || isuserdata(Direction::class.java)

/** Приведение к Direction. */
fun LuaValue.toDirection(): Direction? = when (this) {
    is LuaDirection -> direction
    else -> touserdata(Direction::class.java)
}

/** Проверка: ось (axis). */
fun LuaValue.isAxis(): Boolean =
    this is LuaAxis || isuserdata(Direction.Axis::class.java)

/** Приведение к Direction.Axis. */
fun LuaValue.toAxis(): Direction.Axis? = when (this) {
    is LuaAxis -> axis
    else -> touserdata(Direction.Axis::class.java)
}

/** Проверка: направление по оси (axisdirection). */
fun LuaValue.isAxisDirection(): Boolean =
    this is LuaAxisDirection || isuserdata(Direction.AxisDirection::class.java)

/** Приведение к Direction.AxisDirection. */
fun LuaValue.toAxisDirection(): Direction.AxisDirection? = when (this) {
    is LuaAxisDirection -> axis
    else -> touserdata(Direction.AxisDirection::class.java)
}

// --- ChunkPos ---

/** Проверка: позиция чанка (chunkpos). */
fun LuaValue.isChunkPos(): Boolean =
    this is LuaChunkPos || isuserdata(ChunkPos::class.java)

/** Приведение к ChunkPos. */
fun LuaValue.toChunkPos(): ChunkPos? = when (this) {
    is LuaChunkPos -> pos
    else -> touserdata(ChunkPos::class.java)
}

// --- AABB ---

/** Проверка: бокс (box). */
fun LuaValue.isBox(): Boolean =
    this is LuaBox || isuserdata(AABB::class.java)

/** Приведение к AABB. */
fun LuaValue.toBox(): AABB? = when (this) {
    is LuaBox -> box
    else -> touserdata(AABB::class.java)
}

// --- HitResult ---

/** Проверка: результат рейкаста (raycast). */
fun LuaValue.isRaycast(): Boolean =
    this is LuaRaycast || isuserdata(HitResult::class.java)

/** Приведение к HitResult. */
fun LuaValue.toRaycast(): HitResult? = when (this) {
    is LuaRaycast -> hitResult
    else -> touserdata(HitResult::class.java)
}

// --- EntityType ---

/** Проверка: тип сущности (entitytype) или сырой EntityType. */
fun LuaValue.isEntityType(): Boolean =
    this is LuaEntityType || isuserdata(EntityType::class.java)

/** Приведение к EntityType<*>. */
fun LuaValue.toEntityType(): EntityType<*>? = when (this) {
    is LuaEntityType -> entityType
    else -> touserdata(EntityType::class.java)
}

// --- Transformation ---

/** Проверка: трансформация display-сущностей (transform) или сырая Transformation. */
fun LuaValue.isTransformation(): Boolean =
    this is LuaTransform || isuserdata(Transformation::class.java)

/** Приведение к com.mojang.math.Transformation. */
fun LuaValue.toTransformation(): Transformation? = when (this) {
    is LuaTransform -> LuaTransform.toTransformation(translation, scale, rotationDegrees)
    else -> touserdata(Transformation::class.java)
}
