package de.niklas.miniblock.player;

/** Only the synchronously created particle of this emission inherits the runner's size. */
public final class SprintDust {
    private SprintDust() {}

    public static float currentScale() {
        return ParticleSourceScale.currentScale();
    }

    public static double spawnHeight(double feetY, double vanillaHeight, float scale) {
        return feetY + (vanillaHeight - feetY) * scale;
    }

    public static void duringEmission(float scale, Runnable emission) {
        ParticleSourceScale.duringEmission(scale, emission);
    }
}
