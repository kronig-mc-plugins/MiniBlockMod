package de.niklas.miniblock.client;

/** Intrinsic size retained by each particle, independently of the current camera's size. */
public interface ParticleScaleAccess {
    float miniblock$sourceScale();
    void miniblock$applySourceScale(float scale);
}
