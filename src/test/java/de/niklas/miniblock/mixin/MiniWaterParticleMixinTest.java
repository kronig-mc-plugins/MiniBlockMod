package de.niklas.miniblock.mixin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.niklas.miniblock.client.ParticleScaleAccess;
import org.junit.jupiter.api.Test;

class MiniWaterParticleMixinTest {
    @Test
    void miniatureBubbleKeepsTheNativeMotionProportionThroughoutItsLife() {
        float sourceScale = 1.0F / 16.0F;
        var miniature = new SourceBubble(sourceScale);
        var ordinary = new SourceBubble(1.0F);
        double nativeVelocity = 0.04;
        double miniatureVelocity = nativeVelocity * sourceScale;
        double nativeHeight = 0.0;
        double miniatureHeight = 0.0;
        for (int tick = 0; tick < 40; tick++) {
            nativeVelocity += ordinary.miniblock$scaleBuoyancy(0.002);
            miniatureVelocity += miniature.miniblock$scaleBuoyancy(0.002);
            nativeHeight += nativeVelocity;
            miniatureHeight += miniatureVelocity;
            assertEquals(nativeHeight * sourceScale, miniatureHeight, 1.0E-12);
            nativeVelocity *= 0.85F;
            miniatureVelocity *= 0.85F;
        }
        assertEquals(0.002, ordinary.miniblock$scaleBuoyancy(0.002));
    }

    @Test
    void animatedWakeBoxRetainsSourceScaleAfterProviderInitialization() {
        float sourceScale = 1.0F / 16.0F;
        var miniature = new SourceWake(sourceScale);
        var ordinary = new SourceWake(1.0F);
        for (int life = 20; life <= 60; life++) {
            float nativeBoxWidth = life * ordinary.miniblock$scaleWakeExpansion(0.001F);
            float miniatureBoxWidth = life * miniature.miniblock$scaleWakeExpansion(0.001F);
            assertEquals(nativeBoxWidth * sourceScale, miniatureBoxWidth);
        }
        assertEquals(0.001F, ordinary.miniblock$scaleWakeExpansion(0.001F));
    }

    private static final class SourceBubble extends MiniBubbleParticleMixin implements ParticleScaleAccess {
        private final float sourceScale;

        private SourceBubble(float sourceScale) { this.sourceScale = sourceScale; }
        @Override public float miniblock$sourceScale() { return sourceScale; }
        @Override public void miniblock$applySourceScale(float scale) { throw new UnsupportedOperationException(); }
    }

    private static final class SourceWake extends MiniWakeParticleMixin implements ParticleScaleAccess {
        private final float sourceScale;

        private SourceWake(float sourceScale) { this.sourceScale = sourceScale; }
        @Override public float miniblock$sourceScale() { return sourceScale; }
        @Override public void miniblock$applySourceScale(float scale) { throw new UnsupportedOperationException(); }
    }
}
