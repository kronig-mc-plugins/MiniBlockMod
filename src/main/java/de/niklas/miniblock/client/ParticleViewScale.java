package de.niklas.miniblock.client;

import de.niklas.miniblock.player.ParticleScaleMath;
import de.niklas.miniblock.player.PlayerScale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;

/** Read at extraction time so particles that already exist follow size changes immediately. */
public final class ParticleViewScale {
    private ParticleViewScale() {}

    public static float observerScale() {
        var player = Minecraft.getInstance().player;
        return player != null && PlayerScale.isMini(player) ? player.getScale() : 1.0F;
    }

    public static float sourceScale(Particle particle) {
        return ((ParticleScaleAccess) particle).miniblock$sourceScale();
    }

    public static float modelScale(Particle particle) {
        return ParticleScaleMath.displayScale(sourceScale(particle), observerScale());
    }
}
