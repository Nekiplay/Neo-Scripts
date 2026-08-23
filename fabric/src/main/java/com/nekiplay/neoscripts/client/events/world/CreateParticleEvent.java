package com.nekiplay.neoscripts.client.events.world;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.Vec3;

public class CreateParticleEvent {
    public static final Event<CreateParticleCallback> EVENT = EventFactory.createArrayBacked(
            CreateParticleCallback.class,
            (listeners) -> (event) -> {
                for (CreateParticleCallback listener : listeners) {
                    InteractionResult result = listener.create(event);

                    if(result != InteractionResult.PASS) {
                        return result;
                    }
                }

                return InteractionResult.PASS;
            }
    );

    private final Vec3 pos;
    private final ParticleType<?> particleType;

    public CreateParticleEvent(ParticleType<?> particleType, Vec3 pos) {
        this.particleType = particleType;
        this.pos = pos;
    }

    public Vec3 getPos() {
        return pos;
    }
    public ParticleType<?> getParticleType() {
        return particleType;
    }

    public interface CreateParticleCallback {
        InteractionResult create(CreateParticleEvent event);
    }
}
