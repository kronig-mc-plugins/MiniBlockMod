package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.ParticleSourceScale;
import de.niklas.miniblock.player.PlayerScale;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Entity.class, remap = false)
public abstract class MiniSplashParticleSourceMixin {
    @Redirect(method = "doWaterSplashEffect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void miniblock$scaleSplash(Level level, ParticleOptions options, double x, double y, double z,
                                      double vx, double vy, double vz) {
        if (!((Object) this instanceof Player player) || !PlayerScale.isMini(player)) {
            level.addParticle(options, x, y, z, vx, vy, vz);
            return;
        }
        float scale = player.getScale();
        double bodyMotionY = player.getDeltaMovement().y;
        // Keep the water-surface position and scaled body motion; scale only the native launch noise.
        ParticleSourceScale.duringEmission(scale,
                () -> level.addParticle(options, x, y, z, vx / scale,
                        bodyMotionY / scale + (vy - bodyMotionY), vz / scale));
    }
}
