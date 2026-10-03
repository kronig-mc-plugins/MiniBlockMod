package de.niklas.miniblock.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MiniParticleSourceMixinTest {
    @Test
    void providerSpecificDimensionsAndInitializedMotionScaleOnce() {
        var particle = new ProviderSizedParticle();
        particle.miniblock$applySourceScale(1.0F / 16.0F);
        assertEquals(0.00125F, particle.bbWidth);
        assertEquals(0.0025F, particle.bbHeight);
        assertEquals(0.0175, particle.yd, 1.0E-10);
        assertEquals(0.025F, particle.gravity);
        assertEquals(1.0F / 16.0F, particle.miniblock$sourceScale());
        // Both the provider-return hook and Engine.add see this instance. Scaling must happen only once.
        particle.miniblock$applySourceScale(1.0F / 16.0F);
        assertEquals(0.00125F, particle.bbWidth);
        assertEquals(0.0175, particle.yd, 1.0E-10);
        assertEquals(1, particle.resizeCalls);
    }

    @Test
    void ordinarySourcesRetainTheirProviderDimensionsAndMotion() {
        var particle = new ProviderSizedParticle();
        particle.miniblock$applySourceScale(1.0F);
        assertEquals(0.02F, particle.bbWidth);
        assertEquals(0.04F, particle.bbHeight);
        assertEquals(0.28, particle.yd);
        assertEquals(0.4F, particle.gravity);
        assertEquals(0, particle.resizeCalls);
    }

    /** Minimal provider output that exercises the actual source-scaling mixin without a running client. */
    private static final class ProviderSizedParticle extends MiniParticleSourceMixin {
        private int resizeCalls;

        private ProviderSizedParticle() {
            bbWidth = 0.02F;
            bbHeight = 0.04F;
            yd = 0.28;
            gravity = 0.4F;
        }

        @Override
        protected void setSize(float width, float height) {
            bbWidth = width;
            bbHeight = height;
            resizeCalls++;
        }
    }
}
