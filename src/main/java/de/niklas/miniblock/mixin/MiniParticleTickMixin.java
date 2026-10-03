package de.niklas.miniblock.mixin;

import de.niklas.miniblock.client.ParticleScaleAccess;
import de.niklas.miniblock.player.ParticleSourceScale;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Delayed children retain their parent's source size, independently of later observer changes. */
@Mixin(value = ParticleGroup.class, remap = false)
public abstract class MiniParticleTickMixin {
    @Redirect(method = "tickParticle", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/Particle;tick()V"))
    private void miniblock$inheritParticleSource(Particle particle) {
        float sourceScale = ((ParticleScaleAccess) particle).miniblock$sourceScale();
        ParticleSourceScale.duringEmission(sourceScale, particle::tick);
    }
}
