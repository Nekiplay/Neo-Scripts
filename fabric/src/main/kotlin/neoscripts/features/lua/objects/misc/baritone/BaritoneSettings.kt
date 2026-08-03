package com.nekiplay.neoscripts.features.lua.objects.misc.baritone

import baritone.api.BaritoneAPI
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import xaero.common.minimap.waypoints.Waypoint
import xaero.hud.minimap.BuiltInHudModules
import xaero.hud.minimap.world.MinimapWorld


class BaritoneSettings : LuaValue() {
    override fun typename(): String = "baritone-settings"
    override fun tojstring(): String = "Baritone-Settings"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun set(key: LuaValue?, value: LuaValue?) {
        when (key?.tojstring()) {
            "allowBreak" -> {
                if (value?.isboolean() == true) {
                    BaritoneAPI.getSettings().allowBreak.value = value.toboolean();
                }
            }
            "allowPlace" -> {
                if (value?.isboolean() == true) {
                    BaritoneAPI.getSettings().allowPlace.value = value.toboolean();
                }
            }
            "maxFallHeightNoWater" -> {
                if (value?.isnumber() == true) {
                    BaritoneAPI.getSettings().maxFallHeightNoWater.value = value.toint();
                }
            }
            "maxFallHeightBucket" -> {
                if (value?.isnumber() == true) {
                    BaritoneAPI.getSettings().maxFallHeightBucket.value = value.toint();
                }
            }
            "allowWaterBucketFall" -> {
                if (value?.isboolean() == true) {
                    BaritoneAPI.getSettings().allowWaterBucketFall.value = value.toboolean();
                }
            }
            "pauseMiningForFallingBlocks" -> {
                if (value?.isboolean() == true) {
                    BaritoneAPI.getSettings().pauseMiningForFallingBlocks.value = value.toboolean();
                }
            }
            "autoTool" -> {
                if (value?.isboolean() == true) {
                    BaritoneAPI.getSettings().autoTool.value = value.toboolean();
                }
            }
            "assumeExternalAutoTool" -> {
                if (value?.isboolean() == true) {
                    BaritoneAPI.getSettings().assumeExternalAutoTool.value = value.toboolean();
                }
            }
            "freeLook" -> {
                if (value?.isboolean() == true) {
                    BaritoneAPI.getSettings().freeLook.value = value.toboolean();
                }
            }
            "blockFreeLook" -> {
                if (value?.isboolean() == true) {
                    BaritoneAPI.getSettings().blockFreeLook.value = value.toboolean();
                }
            }
            "allowParkour" -> {
                if (value?.isboolean() == true) {
                    BaritoneAPI.getSettings().allowParkour.value = value.toboolean();
                }
            }
            "allowParkourPlace" -> {
                if (value?.isboolean() == true) {
                    BaritoneAPI.getSettings().allowParkourPlace.value = value.toboolean();
                }
            }
            else -> super.get(key)
        }
        super.set(key, value)
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "allowBreak" -> valueOf(BaritoneAPI.getSettings().allowBreak.value)
            "allowPlace" -> valueOf(BaritoneAPI.getSettings().allowPlace.value)
            "allowWaterBucketFall" -> valueOf(BaritoneAPI.getSettings().allowWaterBucketFall.value)
            "maxFallHeightNoWater" -> valueOf(BaritoneAPI.getSettings().maxFallHeightNoWater.value)
            "maxFallHeightBucket" -> valueOf(BaritoneAPI.getSettings().maxFallHeightBucket.value)
            "pauseMiningForFallingBlocks" -> valueOf(BaritoneAPI.getSettings().pauseMiningForFallingBlocks.value)
            "autoTool" -> valueOf(BaritoneAPI.getSettings().autoTool.value)
            "assumeExternalAutoTool" -> valueOf(BaritoneAPI.getSettings().assumeExternalAutoTool.value)
            "freeLook" -> valueOf(BaritoneAPI.getSettings().freeLook.value)
            "blockFreeLook" -> valueOf(BaritoneAPI.getSettings().blockFreeLook.value)
            "allowParkour" -> valueOf(BaritoneAPI.getSettings().allowParkour.value)
            "allowParkourPlace" -> valueOf(BaritoneAPI.getSettings().allowParkourPlace.value)
            else -> super.get(key)
        }
    }
}