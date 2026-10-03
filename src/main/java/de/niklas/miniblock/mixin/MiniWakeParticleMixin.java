package de.niklas.miniblock.mixin;

import de.niklas.miniblock.client.ParticleScaleAccess;
import net.minecraft.client.particle.WakeParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Wake animation rebuilds its collision box every tick, after provider initialization. */
@Mixin(value = WakeParticle.class, remap = false)
public abstract class MiniWakeParticleMixin {
    @ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.001F))
    float miniblock$scaleWakeExpansion(float nativeExpansion) {
        return nativeExpansion * ((ParticleScaleAccess) this).miniblock$sourceScale();
    }
}
