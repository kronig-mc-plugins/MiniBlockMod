package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.MiniSurfaceEffects;
import de.niklas.miniblock.player.PlayerScale;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, remap = false)
public abstract class MiniLandingParticleSourceMixin {
    @Unique private boolean miniblock$handledLandingParticles;

    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void miniblock$microLanding(double ya, boolean onGround, BlockState state, BlockPos pos, CallbackInfo callback) {
        miniblock$handledLandingParticles = false;
        if (!((Object) this instanceof Player player) || !(player.level() instanceof ServerLevel level)
                || !onGround || player.fallDistance <= 0.0) return;
        var surface = MiniSurfaceEffects.microSurface(player);
        boolean small = PlayerScale.isMini(player);
        if (surface == null && !small) return;
        miniblock$handledLandingParticles = true;
        double relativeDistance = player.fallDistance / (small ? player.getScale() : 1.0);
        double power = Math.max(0.0, Math.floor(relativeDistance + 1.0E-6 - player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)));
        if (power > 0.0) {
            int count = (int) (150.0 * Math.min(0.2 + power / 15.0, 2.5));
            MiniSurfaceEffects.emitLanding(level, player, state, pos, count);
        }
    }

    @Redirect(method = "checkFallDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;addLandingEffects(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;I)Z"))
    private boolean miniblock$replaceStorageMaterial(BlockState receiver, ServerLevel level, BlockPos pos,
                                                    BlockState state, LivingEntity entity, int count) {
        return miniblock$handledLandingParticles || receiver.addLandingEffects(level, pos, state, entity, count);
    }
}
