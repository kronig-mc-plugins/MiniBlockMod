package de.niklas.miniblock.player;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** Carries intrinsic source size even when the source microcell has already been removed. */
public record MiniParticleBurst(ParticleOptions options, BlockPos tintPos, Vec3 origin, Vec3 spread,
                                Vec3 velocity, int count, float sourceScale, double randomSpeed) {
    public static final StreamCodec<RegistryFriendlyByteBuf, MiniParticleBurst> STREAM_CODEC = new StreamCodec<>() {
        @Override public MiniParticleBurst decode(RegistryFriendlyByteBuf buffer) {
            return new MiniParticleBurst(ParticleTypes.STREAM_CODEC.decode(buffer), buffer.readBlockPos(),
                    readVec(buffer), readVec(buffer), readVec(buffer), buffer.readVarInt(), buffer.readFloat(), buffer.readDouble());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, MiniParticleBurst burst) {
            ParticleTypes.STREAM_CODEC.encode(buffer, burst.options());
            buffer.writeBlockPos(burst.tintPos());
            writeVec(buffer, burst.origin());
            writeVec(buffer, burst.spread());
            writeVec(buffer, burst.velocity());
            buffer.writeVarInt(burst.count());
            buffer.writeFloat(burst.sourceScale());
            buffer.writeDouble(burst.randomSpeed());
        }
    };

    public MiniParticleBurst {
        if (count < 1 || count > 512 || !Float.isFinite(sourceScale) || sourceScale <= 0.0F || sourceScale > 16.0F
                || !Double.isFinite(randomSpeed) || randomSpeed < 0.0 || !origin.isFinite() || !spread.isFinite() || !velocity.isFinite()) {
            throw new IllegalArgumentException("Invalid miniature particle burst");
        }
    }

    private static Vec3 readVec(RegistryFriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    private static void writeVec(RegistryFriendlyByteBuf buffer, Vec3 vector) {
        buffer.writeDouble(vector.x);
        buffer.writeDouble(vector.y);
        buffer.writeDouble(vector.z);
    }
}
