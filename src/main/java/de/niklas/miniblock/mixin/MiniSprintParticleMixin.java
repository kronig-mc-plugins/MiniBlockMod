package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.PlayerScale;
import de.niklas.miniblock.player.SprintDust;
import de.niklas.miniblock.player.MiniSurfaceEffects;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla sprint dust otherwise spawns at 0.1 blocks, almost exactly at miniature eye height. */
@Mixin(value = Entity.class, remap = false)
public abstract class MiniSprintParticleMixin {
    @Inject(method = "spawnSprintParticle", at = @At("HEAD"), cancellable = true)
    private void miniblock$useActualMicroSurface(CallbackInfo callback) {
        if (!((Object) this instanceof Player player) || !player.level().isClientSide() || !player.onGround()) return;
        var surface = MiniSurfaceEffects.microSurface(player);
        if (surface != null) {
            MiniSurfaceEffects.emitWalkingDust(player, surface, true);
            callback.cancel();
        }
    }

    @Redirect(method = "spawnSprintParticle", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void miniblock$scaleSprintDust(Level level, ParticleOptions options, double x, double y, double z,
                                         double velocityX, double velocityY, double velocityZ) {
        if (!level.isClientSide() || !((Object) this instanceof Player player) || !PlayerScale.isMini(player)) {
            level.addParticle(options, x, y, z, velocityX, velocityY, velocityZ);
            return;
        }
        float scale = player.getScale();
        double scaledY = SprintDust.spawnHeight(player.getY(), y, scale);
        // Horizontal input already follows miniature movement. Restore its relative magnitude before
        // vanilla combines it with random launch noise; the initialized particle is then scaled once.
        // Use the original entry point: Forge running effects and vanilla visibility/particle limits stay in force.
        SprintDust.duringEmission(scale,
                () -> level.addParticle(options, x, scaledY, z, velocityX / scale, velocityY, velocityZ / scale));
    }
}
