package de.niklas.miniblock.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class VoxelGridTest {
    @Test
    void indicesCoverEveryCellWithoutAliasing() {
        boolean[] seen = new boolean[VoxelGrid.CELL_COUNT];
        for (int y = 0; y < VoxelGrid.SIZE; y++) {
            for (int z = 0; z < VoxelGrid.SIZE; z++) {
                for (int x = 0; x < VoxelGrid.SIZE; x++) {
                    int index = VoxelGrid.index(x, y, z);
                    assertEquals(x + 16 * (z + 16 * y), index);
                    assertFalse(seen[index]);
                    seen[index] = true;
                }
            }
        }
        assertEquals(4095, VoxelGrid.index(15, 15, 15));
        for (boolean visited : seen) assertTrue(visited);
    }

    @Test
    void boundsAndMaterialsAreValidatedBeforeMutation() {
        VoxelGrid grid = new VoxelGrid();
        assertThrows(IndexOutOfBoundsException.class, () -> grid.set(-1, 0, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.set(0, 16, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.get(0, 0, 16));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.get(4096));
        assertThrows(IndexOutOfBoundsException.class, () -> grid.set(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> grid.set(0, -1));
        assertThrows(IllegalArgumentException.class, () -> grid.fill(-1));
        assertEquals(0, grid.revision());
        assertTrue(grid.isEmpty());
    }

    @Test
    void revisionsAndCountTrackOnlyRealChanges() {
        VoxelGrid grid = new VoxelGrid();
        assertFalse(grid.set(0, 0));
        assertTrue(grid.set(0, 3));
        assertFalse(grid.set(0, 3));
        assertEquals(1, grid.revision());
        assertEquals(1, grid.occupiedCount());
        assertTrue(grid.set(0, 4));
        assertEquals(2, grid.revision());
        assertEquals(1, grid.occupiedCount());
        assertTrue(grid.set(0, 0));
        assertEquals(3, grid.revision());
        assertTrue(grid.isEmpty());
        assertFalse(grid.clear());
        assertTrue(grid.fill(7));
        assertEquals(4096, grid.occupiedCount());
        assertEquals(4, grid.revision());
        assertFalse(grid.fill(7));
        assertTrue(grid.clear());
        assertEquals(5, grid.revision());
        assertTrue(grid.isEmpty());
    }

    @Test
    void snapshotsCannotMutateTheirGridOrTheirSource() {
        VoxelGrid grid = new VoxelGrid();
        grid.set(7, 6, 5, 99);
        VoxelGrid.Snapshot snapshot = grid.snapshot();
        int[] exported = snapshot.materials();
        exported[VoxelGrid.index(7, 6, 5)] = 0;
        assertEquals(99, snapshot.get(7, 6, 5));
        assertEquals(99, grid.get(7, 6, 5));
        grid.set(7, 6, 5, 3);
        assertEquals(99, snapshot.get(7, 6, 5));

        int[] input = snapshot.materials();
        VoxelGrid.Snapshot copied = new VoxelGrid.Snapshot(input);
        input[VoxelGrid.index(7, 6, 5)] = 17;
        assertEquals(snapshot, copied);
        assertEquals(snapshot.hashCode(), copied.hashCode());
        assertThrows(IllegalArgumentException.class, () -> new VoxelGrid.Snapshot(new int[4095]));
        int[] negative = new int[4096];
        negative[4095] = -1;
        assertThrows(IllegalArgumentException.class, () -> new VoxelGrid.Snapshot(negative));
    }

    @Test
    void loadingChangesRevisionOnceAndMaintainsCount() {
        VoxelGrid source = new VoxelGrid();
        source.set(0, 1);
        source.set(4095, 2);
        VoxelGrid target = new VoxelGrid();
        assertTrue(target.load(source.snapshot()));
        assertEquals(1, target.revision());
        assertEquals(2, target.occupiedCount());
        assertEquals(1, target.get(0));
        assertEquals(2, target.get(4095));
        assertFalse(target.load(source.snapshot()));
        assertEquals(1, target.revision());
        source.clear();
        assertTrue(target.load(source.snapshot()));
        assertEquals(2, target.revision());
        assertTrue(target.isEmpty());
    }

    @Test
    void aSolidCubeAndRectangularSlabMergeIntoSingleBoxes() {
        VoxelGrid grid = new VoxelGrid();
        assertTrue(grid.collisionBoxes().isEmpty());
        grid.fill(1);
        assertEquals(List.of(new VoxelBox(0, 0, 0, 16, 16, 16)), grid.collisionBoxes());
        assertEquals(List.of(new MaterialBox(new VoxelBox(0, 0, 0, 16, 16, 16), 1)), grid.materialBoxes());
        assertSame(grid.collisionBoxes(), grid.collisionBoxes());
        grid.clear();
        for (int y = 2; y < 5; y++) {
            for (int z = 1; z < 7; z++) {
                for (int x = 3; x < 6; x++) grid.set(x, y, z, 9);
            }
        }
        assertEquals(List.of(new VoxelBox(3, 2, 1, 6, 5, 7)), grid.collisionBoxes());
        VoxelBox slab = grid.collisionBoxes().getFirst();
        assertEquals(54, slab.volume());
        assertEquals(0.1875, slab.minXNormalized());
        assertEquals(0.4375, slab.maxZNormalized());
    }

    @Test
    void materialBoundariesDoNotCreateCollisionGapsAndChangesInvalidateCaches() {
        VoxelGrid grid = new VoxelGrid();
        grid.set(0, 0, 0, 3);
        grid.set(1, 0, 0, 4);
        List<VoxelBox> collisionBefore = grid.collisionBoxes();
        List<MaterialBox> materialsBefore = grid.materialBoxes();
        assertEquals(List.of(new VoxelBox(0, 0, 0, 2, 1, 1)), collisionBefore);
        assertEquals(2, materialsBefore.size());
        grid.set(1, 0, 0, 3);
        assertNotSame(collisionBefore, grid.collisionBoxes());
        assertNotSame(materialsBefore, grid.materialBoxes());
        assertEquals(collisionBefore, grid.collisionBoxes());
        assertEquals(List.of(new MaterialBox(new VoxelBox(0, 0, 0, 2, 1, 1), 3)), grid.materialBoxes());
        assertThrows(UnsupportedOperationException.class, () -> grid.collisionBoxes().clear());
    }

    @Test
    void enclosedCavityAndOpenTunnelStayEmptyInMergedCollision() {
        VoxelGrid cavity = new VoxelGrid();
        cavity.fill(1);
        cavity.set(3, 3, 3, 0);
        assertExactCoverage(cavity);
        assertFalse(cavity.collisionBoxes().stream().anyMatch(box -> box.contains(3, 3, 3)));

        VoxelGrid tunnel = new VoxelGrid();
        tunnel.fill(2);
        // Passage through the whole block: 3/4 block wide and 5/8 block high.
        // Its geometry stays identical when the player's scale changes.
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 10; y++) {
                for (int z = 2; z < 14; z++) tunnel.set(x, y, z, 0);
            }
        }
        assertExactCoverage(tunnel);
        for (int x = 0; x < 16; x++) {
            final int column = x;
            assertFalse(tunnel.collisionBoxes().stream().anyMatch(box -> box.contains(column, 9, 13)));
        }
        assertEquals(4096 - 16 * 10 * 12, tunnel.occupiedCount());
    }

    @Test
    void passageAllowsNormalCrawlingAndTinyStandingButBlocksNormalStanding() {
        VoxelGrid tunnel = new VoxelGrid();
        tunnel.fill(1);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 10; y++) {
                for (int z = 2; z < 14; z++) tunnel.set(x, y, z, 0);
            }
        }
        // Test the actual body volume all along the passage, in normal block units.
        assertFalse(canTravelThrough(tunnel, 0.6, 1.8));
        assertTrue(canTravelThrough(tunnel, 0.6, 0.6));
        assertTrue(canTravelThrough(tunnel, 0.0375, 0.1125));
        // A body wider than the passage still hits the side walls while crawling.
        assertFalse(canTravelThrough(tunnel, 0.8, 0.6));
    }

    @Test
    void arbitraryMixedGridsHaveExactNonoverlappingCoverage() {
        Random random = new Random(472019);
        for (int trial = 0; trial < 30; trial++) {
            VoxelGrid grid = new VoxelGrid();
            for (int index = 0; index < VoxelGrid.CELL_COUNT; index++) grid.set(index, random.nextInt(4));
            assertExactCoverage(grid);
            int[] rendered = new int[VoxelGrid.CELL_COUNT];
            for (MaterialBox materialBox : grid.materialBoxes()) {
                VoxelBox box = materialBox.box();
                for (int y = box.minY(); y < box.maxY(); y++) {
                    for (int z = box.minZ(); z < box.maxZ(); z++) {
                        for (int x = box.minX(); x < box.maxX(); x++) {
                            int index = VoxelGrid.index(x, y, z);
                            assertEquals(0, rendered[index], "Material boxes must never overlap");
                            rendered[index] = materialBox.materialId();
                        }
                    }
                }
            }
            assertArrayEquals(grid.snapshot().materials(), rendered);
        }
    }

    @Test
    void adjacentSixteenthBlocksRemainIndividuallyRemovable() {
        VoxelGrid grid = new VoxelGrid();
        grid.set(14, 15, 15, 5);
        grid.set(15, 15, 15, 5);
        assertEquals(1, grid.collisionBoxes().size());
        assertTrue(grid.set(14, 15, 15, 0));
        assertEquals(1, grid.occupiedCount());
        assertEquals(5, grid.get(15, 15, 15));
        VoxelBox remaining = grid.collisionBoxes().getFirst();
        assertEquals(1, remaining.volume());
        assertEquals(1.0 / 16, remaining.maxXNormalized() - remaining.minXNormalized());
        assertEquals(1.0 / 16, remaining.maxYNormalized() - remaining.minYNormalized());
        assertEquals(1.0 / 16, remaining.maxZNormalized() - remaining.minZNormalized());
    }

    @Test
    void legacyEighthBlockMigrationPreservesWorldGeometryAndMaterials() {
        int[] legacy = new int[512];
        legacy[3 + 8 * (5 + 8 * 7)] = 9;
        legacy[0] = 2;
        VoxelGrid grid = new VoxelGrid();
        grid.load(VoxelGrid.Snapshot.expandLegacy8(legacy));
        assertEquals(16, grid.occupiedCount());
        for (int y = 14; y < 16; y++) {
            for (int z = 10; z < 12; z++) {
                for (int x = 6; x < 8; x++) assertEquals(9, grid.get(x, y, z));
            }
        }
        var migrated = grid.materialBoxes().stream().filter(box -> box.materialId() == 9).findFirst().orElseThrow().box();
        assertEquals(3.0 / 8, migrated.minXNormalized());
        assertEquals(4.0 / 8, migrated.maxXNormalized());
        assertEquals(7.0 / 8, migrated.minYNormalized());
        assertEquals(1, migrated.maxYNormalized());
        assertEquals(5.0 / 8, migrated.minZNormalized());
        assertEquals(6.0 / 8, migrated.maxZNormalized());
        legacy[0] = 0;
        assertEquals(2, grid.get(0));
        assertExactCoverage(grid);
    }

    @Test
    void legacyMigrationRejectsMalformedSnapshots() {
        assertThrows(IllegalArgumentException.class, () -> VoxelGrid.Snapshot.expandLegacy8(new int[4096]));
        int[] invalid = new int[512];
        invalid[511] = -1;
        assertThrows(IllegalArgumentException.class, () -> VoxelGrid.Snapshot.expandLegacy8(invalid));
    }

    private static void assertExactCoverage(VoxelGrid grid) {
        int[] coverage = new int[VoxelGrid.CELL_COUNT];
        for (VoxelBox box : grid.collisionBoxes()) {
            for (int y = box.minY(); y < box.maxY(); y++) {
                for (int z = box.minZ(); z < box.maxZ(); z++) {
                    for (int x = box.minX(); x < box.maxX(); x++) coverage[VoxelGrid.index(x, y, z)]++;
                }
            }
        }
        for (int index = 0; index < VoxelGrid.CELL_COUNT; index++) {
            assertEquals(grid.get(index) == 0 ? 0 : 1, coverage[index], "Cell " + index);
        }
    }

    private static boolean canTravelThrough(VoxelGrid grid, double width, double height) {
        for (double centerX = -width; centerX <= 1 + width; centerX += 0.0625) {
            double minX = centerX - width / 2;
            double maxX = centerX + width / 2;
            double minZ = 0.5 - width / 2;
            double maxZ = 0.5 + width / 2;
            for (VoxelBox box : grid.collisionBoxes()) {
                if (maxX > box.minXNormalized() && minX < box.maxXNormalized()
                        && height > box.minYNormalized() && 0 < box.maxYNormalized()
                        && maxZ > box.minZNormalized() && minZ < box.maxZNormalized()) {
                    return false;
                }
            }
        }
        return true;
    }
}
