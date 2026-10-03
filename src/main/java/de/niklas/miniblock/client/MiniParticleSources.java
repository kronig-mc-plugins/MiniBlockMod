package de.niklas.miniblock.client;

import de.niklas.miniblock.player.MiniParticleBurst;
import de.niklas.miniblock.player.MiniSurfaceEffects;
import de.niklas.miniblock.player.ParticleSourceScale;
import de.niklas.miniblock.player.PlayerNetwork;
import de.niklas.miniblock.player.PlayerScale;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Source-sized bursts use ordinary providers, preserving texture, tint and native particle settings. */
@Mod.EventBusSubscriber(modid = "miniblock", value = Dist.CLIENT)
public final class MiniParticleSources {
    private MiniParticleSources() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        PlayerNetwork.setParticleReceiver(MiniParticleSources::receiveBurst);
    }

    private static void receiveBurst(MiniParticleBurst burst) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var options = burst.options();
        if (options instanceof BlockParticleOption block) block.setPos(burst.tintPos());
        ParticleSourceScale.duringEmission(burst.sourceScale(), () -> {
            var random = level.getRandom();
            for (int index = 0; index < burst.count(); index++) {
                level.addParticle(options,
                        burst.origin().x + random.nextGaussian() * burst.spread().x,
                        burst.origin().y + random.nextGaussian() * burst.spread().y,
                        burst.origin().z + random.nextGaussian() * burst.spread().z,
                        burst.velocity().x + random.nextGaussian() * burst.randomSpeed(),
                        burst.velocity().y + random.nextGaussian() * burst.randomSpeed(),
                        burst.velocity().z + random.nextGaussian() * burst.randomSpeed());
            }
        });
    }

    @SubscribeEvent
    public static void walking(TickEvent.PlayerTickEvent.Post event) {
        var player = event.player();
        if (!player.level().isClientSide() || !player.onGround() || player.isSprinting()
                || !PlayerScale.canCrawl(player) || player.tickCount % 4 != 0) return;
        double scale = PlayerScale.isMini(player) ? player.getScale() : 1.0;
        if (player.getDeltaMovement().horizontalDistanceSqr() < 1.0E-6 * scale * scale) return;
        var surface = MiniSurfaceEffects.microSurface(player);
        if (surface != null) MiniSurfaceEffects.emitWalkingDust(player, surface, false);
    }
}
