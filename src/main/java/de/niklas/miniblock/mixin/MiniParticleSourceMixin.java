package de.niklas.miniblock.mixin;

import de.niklas.miniblock.client.ParticleScaleAccess;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Apply an effect source's physical scale once, after its provider has initialized it. */
@Mixin(value = Particle.class, remap = false)
public abstract class MiniParticleSourceMixin implements ParticleScaleAccess {
    @Shadow protected double xd;
    @Shadow protected double yd;
    @Shadow protected double zd;
    @Shadow protected float gravity;
    @Shadow protected float bbWidth;
    @Shadow protected float bbHeight;
    @Shadow protected abstract void setSize(float width, float height);
    @Unique private float miniblock$intrinsicScale = 1.0F;
    @Unique private boolean miniblock$sourceScaleApplied;

    @Override
    public float miniblock$sourceScale() {
        return miniblock$intrinsicScale;
    }

    @Override
    public void miniblock$applySourceScale(float scale) {
        if (miniblock$sourceScaleApplied || scale == 1.0F || !Float.isFinite(scale) || scale <= 0.0F) return;
        miniblock$sourceScaleApplied = true;
        miniblock$intrinsicScale = scale;
        // Preserve provider-specific boxes. Particle.scale instead resets them to a 0.2-block base box.
        // Visual geometry is scaled at extraction, including getQuadSize overrides independent of quadSize.
        setSize(bbWidth * scale, bbHeight * scale);
        xd *= scale;
        yd *= scale;
        zd *= scale;
        gravity *= scale;
    }
}
