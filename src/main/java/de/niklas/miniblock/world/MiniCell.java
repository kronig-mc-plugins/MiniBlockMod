package de.niklas.miniblock.world;

import de.niklas.miniblock.core.MicroCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.UseOnContext;

/** Resolves the occupied hit cell before crossing a face, including exact tangent edges. */
public record MiniCell(BlockPos pos, int x, int y, int z) {
    public static MiniCell atHit(UseOnContext context, boolean adjacent) {
        var hit = context.getClickLocation();
        var carrier = context.getClickedPos();
        var grid = context.getLevel().getBlockEntity(carrier) instanceof MiniBlockEntity mini ? mini.grid() : null;
        var normal = MicroCoordinates.Face.valueOf(context.getClickedFace().name());
        var address = MicroCoordinates.hitCell(hit.x, hit.y, hit.z, normal,
                carrier.getX(), carrier.getY(), carrier.getZ(), grid);
        if (adjacent) address = address.offset(normal);
        return new MiniCell(new BlockPos(address.blockX(), address.blockY(), address.blockZ()),
                address.cellX(), address.cellY(), address.cellZ());
    }
}
