package de.niklas.miniblock.world;

import de.niklas.miniblock.core.MicroCoordinates;
import de.niklas.miniblock.core.VoxelGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;

/** Resolves the occupied hit cell before crossing a face, including exact tangent edges. */
public record MiniCell(BlockPos pos, int x, int y, int z) {
    public static MiniCell atHit(UseOnContext context, boolean adjacent) {
        return atHit(context.getLevel(), new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), false), adjacent);
    }

    public static MiniCell atHit(BlockGetter level, BlockHitResult blockHit, boolean adjacent) {
        var hit = blockHit.getLocation();
        var carrier = blockHit.getBlockPos();
        var entity = level.getBlockEntity(carrier) instanceof MiniBlockEntity mini ? mini : null;
        var grid = entity != null ? entity.grid() : null;
        var normal = MicroCoordinates.Face.valueOf(blockHit.getDirection().name());
        var address = MicroCoordinates.hitCell(hit.x, hit.y, hit.z, normal,
                carrier.getX(), carrier.getY(), carrier.getZ(), grid);
        if (entity != null && !touchesFace(entity, address.cellX(), address.cellY(), address.cellZ(),
                hit.x - carrier.getX(), hit.y - carrier.getY(), hit.z - carrier.getZ(), normal)) {
            // Slab and stair surfaces can lie halfway through a cell. Resolve their real faces,
            // including tangent edges, instead of assuming that every occupied cell is a cube.
            search:
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int x = address.cellX() + dx;
                        int y = address.cellY() + dy;
                        int z = address.cellZ() + dz;
                        if (x < 0 || x >= VoxelGrid.SIZE || y < 0 || y >= VoxelGrid.SIZE || z < 0 || z >= VoxelGrid.SIZE)
                            continue;
                        if (touchesFace(entity, x, y, z, hit.x - carrier.getX(), hit.y - carrier.getY(),
                                hit.z - carrier.getZ(), normal)) {
                            address = new MicroCoordinates.CellAddress(carrier.getX(), carrier.getY(), carrier.getZ(), x, y, z);
                            break search;
                        }
                    }
                }
            }
        }
        if (adjacent) address = address.offset(normal);
        return new MiniCell(new BlockPos(address.blockX(), address.blockY(), address.blockZ()),
                address.cellX(), address.cellY(), address.cellZ());
    }

    private static boolean touchesFace(MiniBlockEntity entity, int x, int y, int z,
                                       double hitX, double hitY, double hitZ, MicroCoordinates.Face face) {
        if (entity.grid().get(x, y, z) == 0) return false;
        boolean[] found = {false};
        entity.cellShape(x, y, z, false).forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> {
            double epsilon = 1.0e-7;
            if (hitX < minX - epsilon || hitX > maxX + epsilon
                    || hitY < minY - epsilon || hitY > maxY + epsilon
                    || hitZ < minZ - epsilon || hitZ > maxZ + epsilon) return;
            if (face.dx != 0 && Math.abs(hitX - (face.dx > 0 ? maxX : minX)) <= epsilon
                    || face.dy != 0 && Math.abs(hitY - (face.dy > 0 ? maxY : minY)) <= epsilon
                    || face.dz != 0 && Math.abs(hitZ - (face.dz > 0 ? maxZ : minZ)) <= epsilon) found[0] = true;
        });
        return found[0];
    }
}
