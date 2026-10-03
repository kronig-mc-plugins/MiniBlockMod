package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.ParticleSourceScale;
import de.niklas.miniblock.player.PlayerScale;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TrackingEmitter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Entity emitters also tick during construction and outside the ordinary particle groups. */
@Mixin(value = TrackingEmitter.class, remap = false)
public abstract class MiniTrackingEmitterMixin {
    @Shadow @Final private Entity entity;
    @Unique private float miniblock$sourceScale;

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void miniblock$sizeTrackingEffects(ClientLevel level, ParticleOptions options,
                                               double x, double y, double z, double vx, double vy, double vz) {
        if (miniblock$sourceScale == 0.0F) {
            float entityScale = entity instanceof Player player && PlayerScale.isMini(player) ? player.getScale() : 1.0F;
            miniblock$sourceScale = Math.min(entityScale, ParticleSourceScale.currentScale());
        }
        ParticleSourceScale.duringEmission(miniblock$sourceScale,
                () -> level.addParticle(options, x, y, z, vx, vy, vz));
    }
}
