package de.niklas.miniblock.player;

/** Only the synchronously created particle of this emission inherits the runner's size. */
public final class SprintDust {
    private static final ThreadLocal<Float> EMISSION_SCALE = new ThreadLocal<>();

    private SprintDust() {}

    public static float currentScale() {
        Float scale = EMISSION_SCALE.get();
        return scale == null ? 1.0F : scale;
    }

    public static double spawnHeight(double feetY, double vanillaHeight, float scale) {
        return feetY + (vanillaHeight - feetY) * scale;
    }

    public static void duringEmission(float scale, Runnable emission) {
        Float previousScale = EMISSION_SCALE.get();
        EMISSION_SCALE.set(scale);
        try {
            emission.run();
        } finally {
            // Limits, absent particle providers, and exceptions must never resize later particles.
            if (previousScale == null) EMISSION_SCALE.remove();
            else EMISSION_SCALE.set(previousScale);
        }
    }
}
