package de.niklas.miniblock.core;

import java.util.Objects;

/** World-to-microcell mapping, including block boundaries and negative positions. */
public final class MicroCoordinates {
    private static final double HIT_EPSILON = 1.0e-7;
    // Minecraft's face clipping tolerates 1e-7 normal block units at tangent edges.
    private static final double EDGE_EPSILON = 1.0e-7 * VoxelGrid.SIZE;

    private MicroCoordinates() {}

    public static CellAddress at(double worldX, double worldY, double worldZ) {
        return fromGlobalCells(floorCell(worldX * VoxelGrid.SIZE),
                floorCell(worldY * VoxelGrid.SIZE), floorCell(worldZ * VoxelGrid.SIZE));
    }

    public static CellAddress fromGlobalCells(int x, int y, int z) {
        return new CellAddress(Math.floorDiv(x, VoxelGrid.SIZE), Math.floorDiv(y, VoxelGrid.SIZE),
                Math.floorDiv(z, VoxelGrid.SIZE), Math.floorMod(x, VoxelGrid.SIZE),
                Math.floorMod(y, VoxelGrid.SIZE), Math.floorMod(z, VoxelGrid.SIZE));
    }

    /** The cell just inside a hit face; use selected.offset(face) to place beside it. */
    public static CellAddress hitCell(double worldX, double worldY, double worldZ, Face face) {
        Objects.requireNonNull(face, "face");
        return fromGlobalCells(floorCell(worldX * VoxelGrid.SIZE - face.dx * HIT_EPSILON),
                floorCell(worldY * VoxelGrid.SIZE - face.dy * HIT_EPSILON),
                floorCell(worldZ * VoxelGrid.SIZE - face.dz * HIT_EPSILON));
    }

    /**
     * Resolves a hit inside its known carrier. At an exact tangent edge, the usual
     * floor may select an empty cell; prefer an occupied cell sharing that face.
     * A null grid represents a normal, completely occupied carrier block.
     */
    public static CellAddress hitCell(double worldX, double worldY, double worldZ, Face face,
                                      int carrierX, int carrierY, int carrierZ, VoxelGrid grid) {
        CellAddress mapped = hitCell(worldX, worldY, worldZ, face);
        int originX = Math.multiplyExact(carrierX, VoxelGrid.SIZE);
        int originY = Math.multiplyExact(carrierY, VoxelGrid.SIZE);
        int originZ = Math.multiplyExact(carrierZ, VoxelGrid.SIZE);
        int x = clampCell((long) mapped.globalX() - originX);
        int y = clampCell((long) mapped.globalY() - originY);
        int z = clampCell((long) mapped.globalZ() - originZ);
        CellAddress primary = new CellAddress(carrierX, carrierY, carrierZ, x, y, z);
        if (grid == null || grid.get(x, y, z) != 0) return primary;

        double localX = worldX * VoxelGrid.SIZE - originX;
        double localY = worldY * VoxelGrid.SIZE - originY;
        double localZ = worldZ * VoxelGrid.SIZE - originZ;
        int alternateX = face.dx == 0 ? tangentAlternative(localX, x) : -1;
        int alternateY = face.dy == 0 ? tangentAlternative(localY, y) : -1;
        int alternateZ = face.dz == 0 ? tangentAlternative(localZ, z) : -1;

        // Stable ordering: X, Y, XY, Z, XZ, YZ, XYZ; unavailable axes are skipped.
        for (int mask = 1; mask < 8; mask++) {
            if (((mask & 1) != 0 && alternateX < 0)
                    || ((mask & 2) != 0 && alternateY < 0)
                    || ((mask & 4) != 0 && alternateZ < 0)) continue;
            int candidateX = (mask & 1) != 0 ? alternateX : x;
            int candidateY = (mask & 2) != 0 ? alternateY : y;
            int candidateZ = (mask & 4) != 0 ? alternateZ : z;
            if (grid.get(candidateX, candidateY, candidateZ) != 0
                    && liesOnFace(localX, localY, localZ, candidateX, candidateY, candidateZ, face)) {
                return new CellAddress(carrierX, carrierY, carrierZ, candidateX, candidateY, candidateZ);
            }
        }
        return primary;
    }

    private static int clampCell(long coordinate) {
        return (int) Math.clamp(coordinate, 0, VoxelGrid.SIZE - 1);
    }

    private static int tangentAlternative(double coordinate, int primary) {
        long boundary = Math.round(coordinate);
        if (Math.abs(coordinate - boundary) > EDGE_EPSILON) return -1;
        int alternative;
        if (boundary == primary) alternative = primary - 1;
        else if (boundary == primary + 1) alternative = primary + 1;
        else return -1;
        return alternative >= 0 && alternative < VoxelGrid.SIZE ? alternative : -1;
    }

    private static boolean liesOnFace(double hitX, double hitY, double hitZ,
                                      int x, int y, int z, Face face) {
        if (face.dx != 0 && Math.abs(hitX - (x + (face.dx > 0 ? 1 : 0))) > EDGE_EPSILON) return false;
        if (face.dy != 0 && Math.abs(hitY - (y + (face.dy > 0 ? 1 : 0))) > EDGE_EPSILON) return false;
        if (face.dz != 0 && Math.abs(hitZ - (z + (face.dz > 0 ? 1 : 0))) > EDGE_EPSILON) return false;
        return hitX >= x - EDGE_EPSILON && hitX <= x + 1 + EDGE_EPSILON
                && hitY >= y - EDGE_EPSILON && hitY <= y + 1 + EDGE_EPSILON
                && hitZ >= z - EDGE_EPSILON && hitZ <= z + 1 + EDGE_EPSILON;
    }

    private static int floorCell(double scaledCoordinate) {
        double floor = Math.floor(scaledCoordinate);
        if (!Double.isFinite(floor) || floor < Integer.MIN_VALUE || floor > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("World position is outside the supported microcell range");
        }
        return (int) floor;
    }

    public enum Face {
        DOWN(0, -1, 0), UP(0, 1, 0), NORTH(0, 0, -1), SOUTH(0, 0, 1), WEST(-1, 0, 0), EAST(1, 0, 0);

        public final int dx;
        public final int dy;
        public final int dz;

        Face(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }

    public record CellAddress(int blockX, int blockY, int blockZ, int cellX, int cellY, int cellZ) {
        public CellAddress {
            VoxelGrid.index(cellX, cellY, cellZ);
        }

        public int index() {
            return VoxelGrid.index(cellX, cellY, cellZ);
        }

        public int globalX() { return Math.addExact(Math.multiplyExact(blockX, VoxelGrid.SIZE), cellX); }
        public int globalY() { return Math.addExact(Math.multiplyExact(blockY, VoxelGrid.SIZE), cellY); }
        public int globalZ() { return Math.addExact(Math.multiplyExact(blockZ, VoxelGrid.SIZE), cellZ); }

        public CellAddress offset(Face face) {
            Objects.requireNonNull(face, "face");
            return fromGlobalCells(Math.addExact(globalX(), face.dx), Math.addExact(globalY(), face.dy),
                    Math.addExact(globalZ(), face.dz));
        }
    }
}
