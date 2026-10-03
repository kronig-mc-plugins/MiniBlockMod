package de.niklas.miniblock.player;

import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

/** The client requests a toggle; the server decides and acknowledges the resulting state. */
public final class PlayerNetwork {
    private static SimpleChannel channel;
    private static Consumer<Boolean> clientReceiver = ignored -> {};
    private static Consumer<MiniParticleBurst> particleReceiver = ignored -> {};

    private PlayerNetwork() {}

    static void register() {
        channel = ChannelBuilder.named("miniblock:player").networkProtocolVersion(2).simpleChannel();
        channel.play()
                .serverbound(flow -> flow.addMain(CrawlToggle.class, StreamCodec.unit(new CrawlToggle()),
                        (message, context) -> {
                            ServerPlayer sender = context.getSender();
                            if (sender != null) {
                                PlayerScale.toggleCrawling(sender);
                            }
                        }))
                .clientbound(flow -> flow.addMain(CrawlState.class, new StreamCodec<RegistryFriendlyByteBuf, CrawlState>() {
                    @Override
                    public CrawlState decode(RegistryFriendlyByteBuf buffer) {
                        return new CrawlState(buffer.readBoolean());
                    }

                    @Override
                    public void encode(RegistryFriendlyByteBuf buffer, CrawlState message) {
                        buffer.writeBoolean(message.enabled());
                    }
                }, (message, context) -> clientReceiver.accept(message.enabled()))
                .addMain(MiniParticleBurst.class, MiniParticleBurst.STREAM_CODEC,
                        (message, context) -> particleReceiver.accept(message)));
        channel.build();
    }

    /** Installed exclusively by the Dist.CLIENT event subscriber; common code never loads Minecraft. */
    static void setClientReceiver(Consumer<Boolean> receiver) {
        clientReceiver = receiver;
    }

    public static void setParticleReceiver(Consumer<MiniParticleBurst> receiver) {
        particleReceiver = receiver;
    }

    public static void sendParticles(ServerLevel level, MiniParticleBurst burst) {
        channel.send(burst, PacketDistributor.NEAR.with(new PacketDistributor.TargetPoint(
                burst.origin().x, burst.origin().y, burst.origin().z, 48.0, level.dimension())));
    }

    static void requestCrawlToggle() {
        channel.send(new CrawlToggle(), PacketDistributor.SERVER.noArg());
    }

    static void sendCrawlState(ServerPlayer player, boolean enabled) {
        channel.send(new CrawlState(enabled), PacketDistributor.PLAYER.with(player));
    }

    private record CrawlToggle() {}
    private record CrawlState(boolean enabled) {}
}
