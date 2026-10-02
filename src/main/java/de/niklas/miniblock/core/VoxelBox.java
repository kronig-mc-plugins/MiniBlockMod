package de.niklas.miniblock.core;

/** A nonempty box in local microcell coordinates; maximum bounds are exclusive. */
public record VoxelBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public VoxelBox {
        if (minX < 0 || minY < 0 || minZ < 0
                || maxX > VoxelGrid.SIZE || maxY > VoxelGrid.SIZE || maxZ > VoxelGrid.SIZE
                || minX >= maxX || minY >= maxY || minZ >= maxZ) {
            throw new IllegalArgumentException("Box must be nonempty and within the microcell grid");
        }
    }

    public int volume() {
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x < maxX && y >= minY && y < maxY && z >= minZ && z < maxZ;
    }

    public double minXNormalized() { return minX / (double) VoxelGrid.SIZE; }
    public double minYNormalized() { return minY / (double) VoxelGrid.SIZE; }
    public double minZNormalized() { return minZ / (double) VoxelGrid.SIZE; }
    public double maxXNormalized() { return maxX / (double) VoxelGrid.SIZE; }
    public double maxYNormalized() { return maxY / (double) VoxelGrid.SIZE; }
    public double maxZNormalized() { return maxZ / (double) VoxelGrid.SIZE; }
}
