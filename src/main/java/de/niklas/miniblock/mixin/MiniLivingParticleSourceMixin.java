package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.MiniParticleBurst;
import de.niklas.miniblock.player.ParticleSourceScale;
import de.niklas.miniblock.player.PlayerNetwork;
import de.niklas.miniblock.player.PlayerScale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Entity-bound effects inherit the source player's physical size, independently of the observer. */
@Mixin(value = LivingEntity.class, remap = false)
public abstract class MiniLivingParticleSourceMixin {
    @Unique private float miniblock$sourceScale() {
        return (Object) this instanceof Player player && PlayerScale.isMini(player) ? player.getScale() : 1.0F;
    }

    @Redirect(method = {"tickEffects", "handleEntityEvent"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void miniblock$bodyEffects(Level level, ParticleOptions options, double x, double y, double z,
                                      double vx, double vy, double vz) {
        ParticleSourceScale.duringEmission(miniblock$sourceScale(), () -> level.addParticle(options, x, y, z, vx, vy, vz));
    }

    @Redirect(method = "makePoofParticles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void miniblock$deathPoof(Level level, ParticleOptions options, double x, double y, double z,
                                    double vx, double vy, double vz) {
        float scale = miniblock$sourceScale();
        ParticleSourceScale.duringEmission(scale, () -> level.addParticle(options,
                x + vx * 10.0 * (1.0 - scale), y + vy * 10.0 * (1.0 - scale), z + vz * 10.0 * (1.0 - scale), vx, vy, vz));
    }

    @Redirect(method = "makeDrownParticles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void miniblock$drowning(Level level, ParticleOptions options, double x, double y, double z,
                                    double vx, double vy, double vz) {
        var entity = (LivingEntity) (Object) this;
        float scale = miniblock$sourceScale();
        ParticleSourceScale.duringEmission(scale, () -> level.addParticle(options,
                entity.getX() + (x - entity.getX()) * scale,
                entity.getY() + (y - entity.getY()) * scale,
                entity.getZ() + (z - entity.getZ()) * scale, vx / scale, vy / scale, vz / scale));
    }

    @Unique private Vec3 miniblock$itemOrigin(double x, double y, double z) {
        var entity = (LivingEntity) (Object) this;
        float scale = miniblock$sourceScale();
        return new Vec3(entity.getX() + (x - entity.getX()) * scale,
                entity.getEyeY() + (y - entity.getEyeY()) * scale,
                entity.getZ() + (z - entity.getZ()) * scale);
    }

    @Redirect(method = "spawnItemParticles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void miniblock$eatingClient(Level level, ParticleOptions options, double x, double y, double z,
                                       double vx, double vy, double vz) {
        Vec3 origin = miniblock$itemOrigin(x, y, z);
        ParticleSourceScale.duringEmission(miniblock$sourceScale(),
                () -> level.addParticle(options, origin.x, origin.y, origin.z, vx, vy, vz));
    }

    @Redirect(method = "spawnItemParticles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int miniblock$eatingServer(ServerLevel level, ParticleOptions options, double x, double y, double z,
                                      int count, double vx, double vy, double vz, double speed) {
        float scale = miniblock$sourceScale();
        if (scale == 1.0F) return level.sendParticles(options, x, y, z, count, vx, vy, vz, speed);
        Vec3 origin = miniblock$itemOrigin(x, y, z);
        PlayerNetwork.sendParticles(level, new MiniParticleBurst(options, BlockPos.containing(origin), origin,
                Vec3.ZERO, new Vec3(vx, vy, vz), Math.clamp(count, 1, 512), scale, 0.0));
        return 0;
    }
}
