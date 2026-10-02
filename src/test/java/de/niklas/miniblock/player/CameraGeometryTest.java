package de.niklas.miniblock.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CameraGeometryTest {
    @Test
    void miniatureNearPlaneAndViewBobStayInsideHeadAtAllSupportedFovs() {
        float scale = 1.0F / 16.0F;
        for (float fov : new float[] {30.0F, 70.0F, 110.0F}) {
            for (float aspect : new float[] {4.0F / 3.0F, 16.0F / 9.0F, 32.0F / 9.0F, 4.0F}) {
                float near = CameraGeometry.nearPlane(0.05F, scale, fov, aspect);
                double tangent = Math.tan(Math.toRadians(fov) * 0.5);
                double cornerRadius = near * Math.sqrt(1.0 + tangent * tangent * (1.0 + aspect * aspect));
                double bobRadius = CameraGeometry.maximumViewBob(scale) * Math.sqrt(1.25);
                assertTrue(cornerRadius + bobRadius < 0.18 * scale,
                        "The complete near-plane corner and camera bob must fit below a standing ceiling");
                assertTrue(cornerRadius + bobRadius < 0.3 * scale,
                        "A colliding player's near plane must stay on the visible side of the wall");
                assertTrue(near > 0.0F);
            }
        }
    }

    @Test
    void ordinaryCameraProjectionKeepsItsNativeClipDistance() {
        assertEquals(0.05F, CameraGeometry.nearPlane(0.05F, 1.0F, 110.0F, 4.0F));
    }
}
