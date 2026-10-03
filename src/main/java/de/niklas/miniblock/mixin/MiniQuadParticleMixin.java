package de.niklas.miniblock.mixin;

import de.niklas.miniblock.client.ParticleViewScale;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Every native quad effect ends here, including subclass animation and multi-quad effects. */
@Mixin(value = SingleQuadParticle.class, remap = false)
public abstract class MiniQuadParticleMixin {
    @Redirect(method = "extractRotatedQuad(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lorg/joml/Quaternionf;FFFF)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/SingleQuadParticle;getQuadSize(F)F"))
    private float miniblock$sizeQuadForObserver(SingleQuadParticle particle, float partialTicks) {
        return particle.getQuadSize(partialTicks) * ParticleViewScale.modelScale(particle);
    }
}
