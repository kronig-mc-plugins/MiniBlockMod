package de.niklas.miniblock.player;

/** Projection distances must fit inside the scaled head, including at wide FOVs. */
public final class CameraGeometry {
    private CameraGeometry() {}

    public static float nearPlane(float vanillaNear, float scale, float verticalFov, float aspectRatio) {
        if (scale >= 1.0F) return vanillaNear;
        double tangent = Math.tan(Math.toRadians(Math.clamp(verticalFov, 1.0F, 179.0F)) * 0.5);
        double cornerDistance = Math.sqrt(1.0 + tangent * tangent * (1.0 + aspectRatio * aspectRatio));
        // A standing player's eyes have 0.18 * scale headroom. Reserve room for view bob.
        return (float) Math.min(vanillaNear * scale, 0.1 * scale / cornerDistance);
    }

    public static float maximumViewBob(float scale) {
        return 0.05F * scale;
    }
}
