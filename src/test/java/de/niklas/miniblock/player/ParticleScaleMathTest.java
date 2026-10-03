package de.niklas.miniblock.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParticleScaleMathTest {
    @Test
    void ordinaryParticlesFollowObserverSizeIncludingParticlesThatAlreadyExist() {
        float sourceScale = 1.0F;
        float nativeQuad = 0.4F;
        assertEquals(nativeQuad, nativeQuad * ParticleScaleMath.displayScale(sourceScale, 1.0F));
        assertEquals(nativeQuad / 16.0F,
                nativeQuad * ParticleScaleMath.displayScale(sourceScale, 1.0F / 16.0F));
        // Growing affects the next frame, without resizing or recreating the stored particle.
        assertEquals(nativeQuad, nativeQuad * ParticleScaleMath.displayScale(sourceScale, 1.0F));
    }

    @Test
    void intrinsicallyMiniatureSourcesAreNeverShrunkTwiceAndStayMiniatureAfterGrowing() {
        float sourceScale = 1.0F / 16.0F;
        float nativeQuad = 0.4F;
        float miniatureQuad = nativeQuad * sourceScale;
        assertEquals(miniatureQuad,
                nativeQuad * ParticleScaleMath.displayScale(sourceScale, 1.0F / 16.0F));
        assertEquals(miniatureQuad, nativeQuad * ParticleScaleMath.displayScale(sourceScale, 1.0F));
        assertEquals(sourceScale, ParticleScaleMath.displayScale(sourceScale, 1.0F));
    }

    @Test
    void animatedOverridesAlsoRetainSourceSizeWhenTheObserverGrows() {
        // Firework OverlayParticle.getQuadSize returns 7.1*sin(animation), independently of stored quadSize.
        float nativePeak = 7.1F;
        float sourceScale = 1.0F / 16.0F;
        assertEquals(0.44375F, nativePeak * ParticleScaleMath.displayScale(sourceScale, 1.0F));
        assertEquals(0.44375F, nativePeak * ParticleScaleMath.displayScale(sourceScale, sourceScale));
        assertEquals(7.1F, nativePeak * ParticleScaleMath.displayScale(1.0F, 1.0F));
    }
}
