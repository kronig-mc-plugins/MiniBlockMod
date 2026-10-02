package de.niklas.miniblock.core;

import java.util.Objects;

/** A box whose cells all contain the same nonempty material. */
public record MaterialBox(VoxelBox box, int materialId) {
    public MaterialBox {
        Objects.requireNonNull(box, "box");
        if (materialId <= 0) {
            throw new IllegalArgumentException("Material boxes require a positive material ID");
        }
    }
}
