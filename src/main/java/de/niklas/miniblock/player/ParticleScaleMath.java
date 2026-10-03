package de.niklas.miniblock.player;

/** Observer scaling affects geometry; source scaling also affects the emitted particle's physics. */
public final class ParticleScaleMath {
    private ParticleScaleMath() {}

    public static float displayScale(float sourceScale, float observerScale) {
        return Math.min(sourceScale, observerScale);
    }
}
