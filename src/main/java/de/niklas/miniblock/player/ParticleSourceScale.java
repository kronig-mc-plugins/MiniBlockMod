package de.niklas.miniblock.player;

/** World-space size of the source effect, scoped to its synchronous vanilla emission. */
public final class ParticleSourceScale {
    private static final ThreadLocal<Float> EMISSION_SCALE = new ThreadLocal<>();

    private ParticleSourceScale() {}

    public static float currentScale() {
        Float scale = EMISSION_SCALE.get();
        return scale == null ? 1.0F : scale;
    }

    public static void duringEmission(float scale, Runnable emission) {
        Float previousScale = EMISSION_SCALE.get();
        EMISSION_SCALE.set(scale);
        try {
            emission.run();
        } finally {
            if (previousScale == null) EMISSION_SCALE.remove();
            else EMISSION_SCALE.set(previousScale);
        }
    }
}
