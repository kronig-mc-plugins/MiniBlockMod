package de.niklas.miniblock.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** A 16 by 16 by 16 block-local grid. Palette ID zero means empty. */
public final class VoxelGrid {
    public static final int SIZE = 16;
    public static final int CELL_COUNT = SIZE * SIZE * SIZE;

    private final int[] materials = new int[CELL_COUNT];
    private int occupiedCount;
    private long revision;
    private List<VoxelBox> collisionBoxes;
    private List<MaterialBox> materialBoxes;

    /** Storage order is x + SIZE * (z + SIZE * y). */
    public static int index(int x, int y, int z) {
        checkCoordinate(x);
        checkCoordinate(y);
        checkCoordinate(z);
        return x + SIZE * (z + SIZE * y);
    }

    public int get(int x, int y, int z) {
        return get(index(x, y, z));
    }

    public int get(int index) {
        checkIndex(index);
        return materials[index];
    }

    /** Returns whether the stored material changed. */
    public boolean set(int x, int y, int z, int materialId) {
        return set(index(x, y, z), materialId);
    }

    public boolean set(int index, int materialId) {
        checkIndex(index);
        checkMaterial(materialId);
        int previous = materials[index];
        if (previous == materialId) {
            return false;
        }
        materials[index] = materialId;
        if (previous == 0) occupiedCount++;
        if (materialId == 0) occupiedCount--;
        changed();
        return true;
    }

    public int occupiedCount() {
        return occupiedCount;
    }

    public boolean isEmpty() {
        return occupiedCount == 0;
    }

    public long revision() {
        return revision;
    }

    /** Sets every cell in one mutation, including when materialId is zero. */
    public boolean fill(int materialId) {
        checkMaterial(materialId);
        boolean different = false;
        for (int material : materials) {
            if (material != materialId) {
                different = true;
                break;
            }
        }
        if (!different) return false;
        Arrays.fill(materials, materialId);
        occupiedCount = materialId == 0 ? 0 : CELL_COUNT;
        changed();
        return true;
    }

    public boolean clear() {
        return fill(0);
    }

    public Snapshot snapshot() {
        return new Snapshot(materials);
    }

    /** Restores values, keeping the revision local and monotonic. */
    public boolean load(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (Arrays.equals(materials, snapshot.materials)) return false;
        System.arraycopy(snapshot.materials, 0, materials, 0, CELL_COUNT);
        occupiedCount = 0;
        for (int material : materials) {
            if (material != 0) occupiedCount++;
        }
        changed();
        return true;
    }

    /** Exact occupied volume, merged across material boundaries for collision. */
    public List<VoxelBox> collisionBoxes() {
        if (collisionBoxes == null) {
            List<VoxelBox> result = new ArrayList<>();
            for (MaterialBox box : merge(false)) {
                result.add(box.box());
            }
            collisionBoxes = List.copyOf(result);
        }
        return collisionBoxes;
    }

    /** Exact occupied volume, with every merged box containing one material. */
    public List<MaterialBox> materialBoxes() {
        if (materialBoxes == null) materialBoxes = merge(true);
        return materialBoxes;
    }

    private List<MaterialBox> merge(boolean matchMaterial) {
        boolean[] visited = new boolean[CELL_COUNT];
        List<MaterialBox> result = new ArrayList<>();
        for (int y = 0; y < SIZE; y++) {
            for (int z = 0; z < SIZE; z++) {
                for (int x = 0; x < SIZE; x++) {
                    int seed = index(x, y, z);
                    int material = materials[seed];
                    if (material == 0 || visited[seed]) continue;

                    int maxX = x + 1;
                    while (maxX < SIZE && matches(maxX, y, z, material, matchMaterial, visited)) maxX++;

                    int maxZ = z + 1;
                    while (maxZ < SIZE && matchesRow(x, maxX, y, maxZ, material, matchMaterial, visited)) maxZ++;

                    int maxY = y + 1;
                    while (maxY < SIZE && matchesSlab(x, maxX, z, maxZ, maxY,
                            material, matchMaterial, visited)) maxY++;

                    for (int boxY = y; boxY < maxY; boxY++) {
                        for (int boxZ = z; boxZ < maxZ; boxZ++) {
                            for (int boxX = x; boxX < maxX; boxX++) {
                                visited[index(boxX, boxY, boxZ)] = true;
                            }
                        }
                    }
                    result.add(new MaterialBox(new VoxelBox(x, y, z, maxX, maxY, maxZ), material));
                }
            }
        }
        return List.copyOf(result);
    }

    private boolean matchesSlab(int minX, int maxX, int minZ, int maxZ, int y,
                                int material, boolean matchMaterial, boolean[] visited) {
        for (int z = minZ; z < maxZ; z++) {
            if (!matchesRow(minX, maxX, y, z, material, matchMaterial, visited)) return false;
        }
        return true;
    }

    private boolean matchesRow(int minX, int maxX, int y, int z,
                               int material, boolean matchMaterial, boolean[] visited) {
        for (int x = minX; x < maxX; x++) {
            if (!matches(x, y, z, material, matchMaterial, visited)) return false;
        }
        return true;
    }

    private boolean matches(int x, int y, int z, int material,
                            boolean matchMaterial, boolean[] visited) {
        int index = index(x, y, z);
        return !visited[index] && materials[index] != 0
                && (!matchMaterial || materials[index] == material);
    }

    private void changed() {
        revision++;
        collisionBoxes = null;
        materialBoxes = null;
    }

    private static void checkCoordinate(int coordinate) {
        if (coordinate < 0 || coordinate >= SIZE) {
            throw new IndexOutOfBoundsException("Microcell coordinate outside [0, " + SIZE + "): " + coordinate);
        }
    }

    private static void checkIndex(int index) {
        if (index < 0 || index >= CELL_COUNT) {
            throw new IndexOutOfBoundsException("Microcell index outside [0, " + CELL_COUNT + "): " + index);
        }
    }

    private static void checkMaterial(int materialId) {
        if (materialId < 0) throw new IllegalArgumentException("Material ID cannot be negative");
    }

    /** Immutable, defensively copied values suitable for persistence or network transfer. */
    public static final class Snapshot {
        private final int[] materials;

        public Snapshot(int[] materials) {
            Objects.requireNonNull(materials, "materials");
            if (materials.length != CELL_COUNT) {
                throw new IllegalArgumentException("A snapshot requires exactly " + CELL_COUNT + " cells");
            }
            this.materials = materials.clone();
            for (int material : this.materials) checkMaterial(material);
        }

        public int get(int x, int y, int z) {
            return materials[index(x, y, z)];
        }

        public int get(int index) {
            checkIndex(index);
            return materials[index];
        }

        public int[] materials() {
            return materials.clone();
        }

        /** Preserves 0.1.0 geometry: each eighth-block becomes eight sixteenth-block cells. */
        public static Snapshot expandLegacy8(int[] legacyCells) {
            Objects.requireNonNull(legacyCells, "legacyCells");
            if (legacyCells.length != 8 * 8 * 8) {
                throw new IllegalArgumentException("A legacy snapshot requires exactly 512 cells");
            }
            int[] expanded = new int[CELL_COUNT];
            for (int y = 0; y < 8; y++) {
                for (int z = 0; z < 8; z++) {
                    for (int x = 0; x < 8; x++) {
                        int material = legacyCells[x + 8 * (z + 8 * y)];
                        checkMaterial(material);
                        for (int dy = 0; dy < 2; dy++) {
                            for (int dz = 0; dz < 2; dz++) {
                                for (int dx = 0; dx < 2; dx++) {
                                    expanded[index(x * 2 + dx, y * 2 + dy, z * 2 + dz)] = material;
                                }
                            }
                        }
                    }
                }
            }
            return new Snapshot(expanded);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Snapshot snapshot && Arrays.equals(materials, snapshot.materials);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(materials);
        }
    }
}
