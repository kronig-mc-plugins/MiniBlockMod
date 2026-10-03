package de.niklas.miniblock.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SprintDustTest {
    @Test
    void scaledSprintDustSpawnsNearFeetInsteadOfInsideMiniatureEyes() {
        float scale = 1.0F / 16.0F;
        double feetY = 64.0;
        double eyeY = feetY + 1.62 * scale;
        double vanillaSpawnY = feetY + 0.1;
        double largestVanillaQuad = 0.1;
        assertTrue(Math.abs(eyeY - vanillaSpawnY) < largestVanillaQuad,
                "Vanilla's unscaled dust can cover a miniature player's camera");
        double miniatureSpawnY = SprintDust.spawnHeight(feetY, vanillaSpawnY, scale);
        assertEquals(feetY + 0.00625, miniatureSpawnY, 1.0E-10);
        assertTrue(eyeY - miniatureSpawnY > largestVanillaQuad * scale,
                "The miniature dust must have clear space below the miniature eyes");
        assertEquals(vanillaSpawnY, SprintDust.spawnHeight(feetY, vanillaSpawnY, 1.0F));
    }

    @Test
    void evenTheLargestVanillaDustArcStaysBelowMiniatureEyesAfterScaling() {
        float scale = 1.0F / 16.0F;
        double y = SprintDust.spawnHeight(0.0, 0.1, scale);
        // Particle's random launch speed is at most 0.15*3*0.4+0.1, before gravity and friction.
        double velocityY = 0.28 * scale;
        double largestQuad = Math.sqrt(2.0) * 0.1 * scale;
        for (int tick = 0; tick < 20; tick++) {
            assertTrue(y + largestQuad < 1.62 * scale,
                    "No scaled sprint-dust frame may reach the runner's eye height");
            velocityY -= 0.04 * scale;
            y += velocityY;
            velocityY *= 0.98;
        }
    }

    @Test
    void emissionScaleCannotLeakIntoLaterOrNestedParticles() {
        assertEquals(1.0F, SprintDust.currentScale());
        SprintDust.duringEmission(1.0F / 16.0F, () -> {
            assertEquals(1.0F / 16.0F, SprintDust.currentScale());
            SprintDust.duringEmission(1.0F / 8.0F, () -> assertEquals(1.0F / 8.0F, SprintDust.currentScale()));
            assertEquals(1.0F / 16.0F, SprintDust.currentScale());
        });
        assertEquals(1.0F, SprintDust.currentScale());
        assertThrows(IllegalStateException.class, () -> SprintDust.duringEmission(1.0F / 16.0F,
                () -> { throw new IllegalStateException("An absent or broken provider must not retain the size"); }));
        assertEquals(1.0F, SprintDust.currentScale());
    }
}
