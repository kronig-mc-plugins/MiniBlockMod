package de.niklas.miniblock.player;

import de.niklas.miniblock.core.VoxelGrid;
import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.world.MiniCell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Resolves the actual support face and material instead of the invisible storage block. */
public final class MiniSurfaceEffects {
    private static final double CELL = 1.0 / VoxelGrid.SIZE;
    private MiniSurfaceEffects() {}

    public static Surface microSurface(Entity entity) {
        AABB footprint = entity.getBoundingBox().deflate(1.0E-7);
        double feetY = entity.getY();
        double tolerance = 0.025 * (entity instanceof Player player && PlayerScale.isMini(player) ? player.getScale() : 1.0);
        Surface closest = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(footprint.minX), Mth.floor(feetY - tolerance - 1.0E-5), Mth.floor(footprint.minZ),
                Mth.floor(footprint.maxX), Mth.floor(feetY + 1.0E-5), Mth.floor(footprint.maxZ))) {
            if (!(entity.level().getBlockEntity(pos) instanceof MiniBlockEntity mini)) continue;
            int minX = Math.clamp(Mth.floor((footprint.minX - pos.getX()) * VoxelGrid.SIZE), 0, VoxelGrid.SIZE - 1);
            int maxX = Math.clamp(Mth.floor((footprint.maxX - pos.getX()) * VoxelGrid.SIZE), 0, VoxelGrid.SIZE - 1);
            int minZ = Math.clamp(Mth.floor((footprint.minZ - pos.getZ()) * VoxelGrid.SIZE), 0, VoxelGrid.SIZE - 1);
            int maxZ = Math.clamp(Mth.floor((footprint.maxZ - pos.getZ()) * VoxelGrid.SIZE), 0, VoxelGrid.SIZE - 1);
            int minY = Math.clamp(Mth.floor((feetY - tolerance - 1.0E-5 - pos.getY()) * VoxelGrid.SIZE), 0, VoxelGrid.SIZE - 1);
            int maxY = Math.clamp(Mth.floor((feetY + 1.0E-5 - pos.getY()) * VoxelGrid.SIZE), 0, VoxelGrid.SIZE - 1);
            for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) {
                BlockState material = mini.stateAt(x, y, z);
                if (material.isAir()) continue;
                for (AABB localBox : mini.cellShape(x, y, z, true).toAabbs()) {
                    AABB box = localBox.move(pos);
                    if (box.maxX < footprint.minX || box.minX > footprint.maxX || box.maxZ < footprint.minZ || box.minZ > footprint.maxZ
                            || box.maxY > feetY + 1.0E-5 || box.maxY < feetY - tolerance - 1.0E-5) continue;
                    double surfaceX = Math.clamp(entity.getX(), box.minX + 1.0E-7, box.maxX - 1.0E-7);
                    double surfaceZ = Math.clamp(entity.getZ(), box.minZ + 1.0E-7, box.maxZ - 1.0E-7);
                    // Prefer the nearest contact to the feet, including partial slabs and edge support.
                    double distance = Math.abs(feetY - box.maxY) * 100.0
                            + Mth.square(surfaceX - entity.getX()) + Mth.square(surfaceZ - entity.getZ());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        closest = new Surface(new MiniCell(pos.immutable(), x, y, z), material,
                                new Vec3(surfaceX, box.maxY, surfaceZ), (float) CELL);
                    }
                }
            }
        }
        return closest;
    }

    public static void emitWalkingDust(Player player, Surface surface, boolean sprinting) {
        if (!surface.state().shouldSpawnTerrainParticles()) return;
        float size = surface.sourceScale();
        float runnerScale = PlayerScale.isMini(player) ? player.getScale() : 1.0F;
        Vec3 motion = player.getDeltaMovement();
        Vec3 point = surface.point();
        ParticleSourceScale.duringEmission(size, () -> player.level().addParticle(
                new BlockParticleOption(ParticleTypes.BLOCK, surface.state()).setPos(surface.cell().pos()),
                point.x, point.y + 0.1 * size, point.z,
                -motion.x * (sprinting ? 4.0 : 2.0) / runnerScale, sprinting ? 1.5 : 0.6,
                -motion.z * (sprinting ? 4.0 : 2.0) / runnerScale));
    }

    public static MiniParticleBurst blockBurst(BlockState state, BlockPos pos, Vec3 origin, float scale, int count, Vec3 spread) {
        return new MiniParticleBurst(new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(pos), pos,
                origin, spread, Vec3.ZERO, count, scale, 0.15);
    }

    public static void emitLanding(ServerLevel level, Player player, BlockState nativeState, BlockPos pos, int count) {
        Surface surface = microSurface(player);
        BlockState material = surface == null ? nativeState : surface.state();
        Vec3 point = surface == null ? player.position() : surface.point();
        float size = surface == null ? player.getScale() : surface.sourceScale();
        if (!material.isAir() && material.shouldSpawnTerrainParticles()) PlayerNetwork.sendParticles(level, blockBurst(material,
                surface == null ? pos : surface.cell().pos(), point, size, Math.clamp(count, 1, 512), Vec3.ZERO));
    }

    public record Surface(MiniCell cell, BlockState state, Vec3 point, float sourceScale) {}
}
