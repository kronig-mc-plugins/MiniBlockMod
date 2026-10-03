package de.niklas.miniblock.mixin;

import de.niklas.miniblock.client.ParticleScaleAccess;
import de.niklas.miniblock.player.ParticleSourceScale;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The common provider endpoint also covers potion, smoke, item, and model effects. */
@Mixin(value = ParticleEngine.class, remap = false)
public abstract class MiniParticleFactoryMixin {
    @Inject(method = "makeParticle", at = @At("RETURN"))
    private void miniblock$applyEffectSourceSize(ParticleOptions options, double x, double y, double z,
                                               double velocityX, double velocityY, double velocityZ,
                                               CallbackInfoReturnable<Particle> callback) {
        Particle particle = callback.getReturnValue();
        if (particle == null) return;
        ((ParticleScaleAccess) particle).miniblock$applySourceScale(ParticleSourceScale.currentScale());
    }

    @Inject(method = "add", at = @At("HEAD"))
    private void miniblock$tagDirectlyConstructedChildren(Particle particle, CallbackInfo callback) {
        ((ParticleScaleAccess) particle).miniblock$applySourceScale(ParticleSourceScale.currentScale());
    }
}
