package com.nekiplay.neoscripts.events

import com.nekiplay.neoscripts.events.main.DoubleEvent
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.world.entity.LivingEntity

class ExtractRenderStateEvent(pre: Boolean, val entity : LivingEntity, val renderEntityState : LivingEntityRenderState, val f : Float) : DoubleEvent(pre)