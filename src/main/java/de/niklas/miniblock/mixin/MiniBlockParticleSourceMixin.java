package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.ParticleSourceScale;
import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.world.MiniBlockInteractions;
import de.niklas.miniblock.world.MiniCell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mining hits must use the selected microcell, because the storage block has no native visible model. */
@Mixin(value = ClientLevel.class, remap = false)
public abstract class MiniBlockParticleSourceMixin {
    @Inject(method = "addBreakingBlockEffects", at = @At("HEAD"), cancellable = true)
    private void miniblock$microMiningHit(BlockPos pos, Direction direction, boolean sound, CallbackInfo callback) {
        ClientLevel level = (ClientLevel) (Object) this;
        if (!(level.getBlockEntity(pos) instanceof MiniBlockEntity mini)) return;
        callback.cancel();
        var minecraft = Minecraft.getInstance();
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !hit.getBlockPos().equals(pos)) return;
        MiniCell cell = MiniCell.atHit(level, hit, false);
        var state = mini.stateAt(cell.x(), cell.y(), cell.z());
        if (state.isAir() || !state.shouldSpawnTerrainParticles()) return;
        double size = MiniBlockInteractions.CELL_SIZE;
        var origin = hit.getLocation().add(direction.getStepX() * 0.1 * size,
                direction.getStepY() * 0.1 * size, direction.getStepZ() * 0.1 * size);
        ParticleSourceScale.duringEmission((float) size, () -> level.addParticle(
                new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(pos), origin.x, origin.y, origin.z, 0.0, 0.0, 0.0));
        if (sound && minecraft.player != null) {
            var type = state.getSoundType(level, pos, minecraft.player);
            level.playLocalSound(origin.x, origin.y, origin.z, type.getHitSound(), SoundSource.BLOCKS,
                    type.getVolume() * 0.25F, type.getPitch(), false);
        }
    }
}
