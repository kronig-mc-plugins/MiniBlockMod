package de.niklas.miniblock.world;

import de.niklas.miniblock.MiniBlockMod;
import de.niklas.miniblock.core.VoxelGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public final class MiniBlockEntity extends BlockEntity {
    private final VoxelGrid grid = new VoxelGrid();
    private long shapeRevision = -1;
    private VoxelShape shape = Shapes.empty();

    public MiniBlockEntity(BlockPos pos, BlockState state) {
        super(MiniBlockMod.MINI_BLOCK_ENTITY.get(), pos, state);
    }

    public VoxelGrid grid() { return grid; }

    public VoxelShape collisionShape() {
        if (shapeRevision != grid.revision()) {
            VoxelShape merged = Shapes.empty();
            for (var box : grid.collisionBoxes()) {
                merged = Shapes.or(merged, Shapes.create(box.minXNormalized(), box.minYNormalized(), box.minZNormalized(),
                        box.maxXNormalized(), box.maxYNormalized(), box.maxZNormalized()));
            }
            shape = merged.optimize();
            shapeRevision = grid.revision();
        }
        return shape;
    }

    public boolean setCell(int x, int y, int z, int material) {
        if (material != 0 && MiniMaterial.byId(material) == null) return false;
        if (!grid.set(x, y, z, material)) return false;
        setChanged();
        if (level != null) {
            if (grid.isEmpty() && !level.isClientSide()) level.removeBlock(worldPosition, false);
            else {
                var state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
                if (!level.isClientSide()) {
                    // A cell can change support even though the container's BlockState is unchanged.
                    state.updateNeighbourShapes(level, worldPosition, Block.UPDATE_ALL);
                    level.updateNeighborsAt(worldPosition, state.getBlock());
                }
            }
        }
        return true;
    }

    public List<ItemStack> drops() {
        int[] counts = new int[MiniMaterial.values().length + 1];
        for (int i = 0; i < VoxelGrid.CELL_COUNT; i++) counts[grid.get(i)]++;
        List<ItemStack> drops = new ArrayList<>();
        for (var material : MiniMaterial.values()) {
            for (int remaining = counts[material.id]; remaining > 0; remaining -= 64) {
                drops.add(material.stack(Math.min(remaining, 64)));
            }
        }
        return drops;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("schema", 1);
        output.putIntArray("cells", grid.snapshot().materials());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int[] cells = input.getIntArray("cells").orElse(new int[VoxelGrid.CELL_COUNT]);
        if (cells.length != VoxelGrid.CELL_COUNT) cells = new int[VoxelGrid.CELL_COUNT];
        // Invalid or newer unknown materials must never produce phantom collision boxes.
        for (int i = 0; i < cells.length; i++) if (MiniMaterial.byId(cells[i]) == null) cells[i] = 0;
        grid.load(new VoxelGrid.Snapshot(cells));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
