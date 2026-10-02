package de.niklas.miniblock.core;

import org.junit.jupiter.api.Test;

import static de.niklas.miniblock.core.MicroCoordinates.Face.*;
import static org.junit.jupiter.api.Assertions.*;

class MicroCoordinatesTest {
    @Test
    void negativeGlobalCellsUseFloorDivision() {
        assertEquals(new MicroCoordinates.CellAddress(-1, -2, -2, 15, 15, 0),
                MicroCoordinates.fromGlobalCells(-1, -17, -32));
        assertEquals(new MicroCoordinates.CellAddress(0, -1, 1, 0, 15, 0),
                MicroCoordinates.at(0, -0.001, 1));
        assertEquals(new MicroCoordinates.CellAddress(-1, -1, -1, 0, 0, 0),
                MicroCoordinates.at(-1, -1, -1));
    }

    @Test
    void globalCellRoundTripsIncludeIntegerExtremes() {
        for (int coordinate : new int[] {Integer.MIN_VALUE, -513, -8, -1, 0, 7, 8, 513, Integer.MAX_VALUE}) {
            MicroCoordinates.CellAddress address = MicroCoordinates.fromGlobalCells(coordinate, coordinate, coordinate);
            assertEquals(coordinate, address.globalX());
            assertEquals(coordinate, address.globalY());
            assertEquals(coordinate, address.globalZ());
            assertTrue(address.index() >= 0 && address.index() < VoxelGrid.CELL_COUNT);
        }
    }

    @Test
    void hitSelectionChoosesTheInsideCellForEveryFace() {
        assertEquals(2, MicroCoordinates.hitCell(0.1875, 0.4, 0.4, EAST).cellX());
        assertEquals(3, MicroCoordinates.hitCell(0.1875, 0.4, 0.4, WEST).cellX());
        assertEquals(2, MicroCoordinates.hitCell(0.4, 0.1875, 0.4, UP).cellY());
        assertEquals(3, MicroCoordinates.hitCell(0.4, 0.1875, 0.4, DOWN).cellY());
        assertEquals(2, MicroCoordinates.hitCell(0.4, 0.4, 0.1875, SOUTH).cellZ());
        assertEquals(3, MicroCoordinates.hitCell(0.4, 0.4, 0.1875, NORTH).cellZ());
    }

    @Test
    void adjacentPlacementCrossesPositiveAndNegativeBlockBoundaries() {
        MicroCoordinates.CellAddress east = MicroCoordinates.hitCell(1, 0.5, 0.5, EAST);
        assertEquals(new MicroCoordinates.CellAddress(0, 0, 0, 15, 8, 8), east);
        assertEquals(new MicroCoordinates.CellAddress(1, 0, 0, 0, 8, 8), east.offset(EAST));
        MicroCoordinates.CellAddress west = MicroCoordinates.hitCell(-1, 0.5, 0.5, WEST);
        assertEquals(new MicroCoordinates.CellAddress(-1, 0, 0, 0, 8, 8), west);
        assertEquals(new MicroCoordinates.CellAddress(-2, 0, 0, 15, 8, 8), west.offset(WEST));
        MicroCoordinates.CellAddress origin = MicroCoordinates.hitCell(0, 0.5, 0.5, EAST);
        assertEquals(-1, origin.blockX());
        assertEquals(15, origin.cellX());
        assertEquals(0, origin.offset(EAST).blockX());
        assertEquals(0, origin.offset(EAST).cellX());
        assertEquals(new MicroCoordinates.CellAddress(0, -1, 0, 0, 15, 0),
                MicroCoordinates.fromGlobalCells(0, 0, 0).offset(DOWN));
    }

    @Test
    void mappingsRejectNonfiniteAndOverflowingPositions() {
        assertThrows(IllegalArgumentException.class, () -> MicroCoordinates.at(Double.NaN, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> MicroCoordinates.at(0, Double.POSITIVE_INFINITY, 0));
        assertThrows(IllegalArgumentException.class, () -> MicroCoordinates.at(Double.MAX_VALUE, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> new MicroCoordinates.CellAddress(0, 0, 0, 16, 0, 0));
        assertThrows(ArithmeticException.class, () ->
                MicroCoordinates.fromGlobalCells(Integer.MAX_VALUE, 0, 0).offset(EAST));
    }

    @Test
    void anchoredHitSelectsOccupiedCellAtExactTangentEdge() {
        VoxelGrid grid = new VoxelGrid();
        grid.set(0, 0, 0, 1);
        var selected = MicroCoordinates.hitCell(0.0625, 0.0625, 0.03125, EAST, 0, 0, 0, grid);
        assertEquals(new MicroCoordinates.CellAddress(0, 0, 0, 0, 0, 0), selected);
        assertEquals(new MicroCoordinates.CellAddress(0, 0, 0, 1, 0, 0), selected.offset(EAST));
        assertEquals(selected, MicroCoordinates.hitCell(0.0625, 0.0625 + 5.0e-8, 0.03125,
                EAST, 0, 0, 0, grid));
        assertEquals(new MicroCoordinates.CellAddress(0, 0, 0, 0, 1, 0),
                MicroCoordinates.hitCell(0.0625, 0.063, 0.03125, EAST, 0, 0, 0, grid));
        // A nearby material behind the wrong face plane must not steal the hit.
        assertEquals(new MicroCoordinates.CellAddress(0, 0, 0, 0, 1, 0),
                MicroCoordinates.hitCell(0.05, 0.0625, 0.03125, EAST, 0, 0, 0, grid));
    }

    @Test
    void outerTangentCornersStayAnchoredToTheClickedCarrier() {
        var expected = new MicroCoordinates.CellAddress(0, 0, 0, 15, 15, 15);
        assertEquals(expected, MicroCoordinates.hitCell(1, 1, 1, UP, 0, 0, 0, null));
        VoxelGrid grid = new VoxelGrid();
        grid.set(15, 15, 15, 2);
        assertEquals(expected, MicroCoordinates.hitCell(1, 1, 1, UP, 0, 0, 0, grid));
        assertEquals(new MicroCoordinates.CellAddress(0, 1, 0, 15, 0, 15), expected.offset(UP));
        assertEquals(new MicroCoordinates.CellAddress(0, 0, 0, 0, 0, 0),
                MicroCoordinates.hitCell(0, 0, 0, WEST, 0, 0, 0, null));
    }

    @Test
    void anchoredNegativeCarrierEdgesResolveAndPlaceCorrectly() {
        VoxelGrid grid = new VoxelGrid();
        grid.set(0, 0, 0, 1);
        var selected = MicroCoordinates.hitCell(-0.9375, -0.9375, -0.96875, EAST, -1, -1, -1, grid);
        assertEquals(new MicroCoordinates.CellAddress(-1, -1, -1, 0, 0, 0), selected);
        assertEquals(new MicroCoordinates.CellAddress(-1, -1, -1, 1, 0, 0), selected.offset(EAST));
        assertEquals(new MicroCoordinates.CellAddress(-1, -1, -1, 15, 15, 15),
                MicroCoordinates.hitCell(0, 0, 0, EAST, -1, -1, -1, null));
    }

    @Test
    void ambiguousEdgesPreferThePrimaryThenAStableTangentCandidate() {
        VoxelGrid grid = new VoxelGrid();
        grid.set(0, 0, 1, 1);
        grid.set(0, 1, 0, 2);
        var fallback = new MicroCoordinates.CellAddress(0, 0, 0, 0, 0, 1);
        for (int repeat = 0; repeat < 5; repeat++) {
            assertEquals(fallback, MicroCoordinates.hitCell(0.0625, 0.0625, 0.0625, EAST, 0, 0, 0, grid));
        }
        grid.set(0, 1, 1, 3);
        assertEquals(new MicroCoordinates.CellAddress(0, 0, 0, 0, 1, 1),
                MicroCoordinates.hitCell(0.0625, 0.0625, 0.0625, EAST, 0, 0, 0, grid));
        grid.clear();
        grid.set(0, 0, 0, 1);
        assertEquals(new MicroCoordinates.CellAddress(0, 0, 0, 0, 0, 0),
                MicroCoordinates.hitCell(0.0625, 0.0625, 0.0625, EAST, 0, 0, 0, grid));
    }

    @Test
    void ordinaryFaceInteriorsKeepTheirOriginalMapping() {
        double[][] hitPositions = {{0.375, 0.41, 0.43}, {0.41, 0.375, 0.43}, {0.41, 0.43, 0.375}};
        for (var face : MicroCoordinates.Face.values()) {
            double[] hit = face.dx != 0 ? hitPositions[0] : face.dy != 0 ? hitPositions[1] : hitPositions[2];
            var original = MicroCoordinates.hitCell(hit[0], hit[1], hit[2], face);
            VoxelGrid grid = new VoxelGrid();
            grid.set(original.cellX(), original.cellY(), original.cellZ(), 1);
            assertEquals(original, MicroCoordinates.hitCell(hit[0], hit[1], hit[2], face, 0, 0, 0, grid));
            assertEquals(original, MicroCoordinates.hitCell(hit[0], hit[1], hit[2], face, 0, 0, 0, null));
        }
    }
}
