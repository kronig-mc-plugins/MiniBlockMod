package de.niklas.miniblock.mixin;

import de.niklas.miniblock.client.ParticleScaleAccess;
import de.niklas.miniblock.player.ParticleSourceScale;
import net.minecraft.client.particle.CritParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Crit particles move once in their constructor, before the ordinary provider-return hook. */
@Mixin(value = CritParticle.class, remap = false)
public abstract class MiniCritParticleMixin {
    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/particle/CritParticle;tick()V"))
    private void miniblock$sizeBeforeFirstMovement(CritParticle particle) {
        // The constructor has finished initializing motion, gravity and bounds at this call site.
        ((ParticleScaleAccess) particle).miniblock$applySourceScale(ParticleSourceScale.currentScale());
        particle.tick();
    }
}
