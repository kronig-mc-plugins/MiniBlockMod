package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.SprintDust;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scale the initialized dust, including vanilla's randomized velocity and gravity. */
@Mixin(value = TerrainParticle.class, remap = false)
public abstract class MiniTerrainParticleMixin extends SingleQuadParticle {
    protected MiniTerrainParticleMixin(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite) {
        super(level, x, y, z, sprite);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDDLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At("TAIL"))
    private void miniblock$scaleInitializedSprintDust(ClientLevel level, double x, double y, double z,
                                                      double velocityX, double velocityY, double velocityZ,
                                                      BlockState state, BlockPos pos, CallbackInfo callback) {
        float scale = SprintDust.currentScale();
        if (scale == 1.0F) return;
        scale(scale);
        xd *= scale;
        yd *= scale;
        zd *= scale;
        gravity *= scale;
    }
}
