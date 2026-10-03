package de.niklas.miniblock.mixin;

import de.niklas.miniblock.client.ParticleScaleAccess;
import net.minecraft.client.particle.BubbleParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Buoyancy is a fixed tick constant rather than Particle.gravity. */
@Mixin(value = BubbleParticle.class, remap = false)
public abstract class MiniBubbleParticleMixin {
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.002))
    double miniblock$scaleBuoyancy(double nativeRise) {
        return nativeRise * ((ParticleScaleAccess) this).miniblock$sourceScale();
    }
}
