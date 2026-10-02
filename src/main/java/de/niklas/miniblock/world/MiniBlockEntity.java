package de.niklas.miniblock.world;

import de.niklas.miniblock.MiniBlockMod;
import de.niklas.miniblock.core.VoxelGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Independent sixteenth-block cells; the containing world block is only storage. */
public final class MiniBlockEntity extends BlockEntity {
    private static final int SCHEMA = 2;
    private static final double CELL_SIZE = 1.0 / VoxelGrid.SIZE;

    private final VoxelGrid grid = new VoxelGrid();
    // Cell ID n refers to palette[n - 1]. Air always has ID zero.
    private final List<BlockState> palette = new ArrayList<>();
    private final Map<BlockState, Integer> paletteIds = new HashMap<>();
    private final Map<Integer, VoxelShape> nativeCollision = new HashMap<>();
    private final Map<Integer, VoxelShape> nativeOutline = new HashMap<>();
    private long shapeRevision = -1;
    private VoxelShape collision = Shapes.empty();
    private VoxelShape outline = Shapes.empty();

    public MiniBlockEntity(BlockPos pos, BlockState state) {
        super(MiniBlockMod.MINI_BLOCK_ENTITY.get(), pos, state);
    }

    public VoxelGrid grid() { return grid; }

    public BlockState stateAt(int x, int y, int z) { return stateForId(grid.get(x, y, z)); }

    public BlockState stateForId(int id) {
        return id > 0 && id <= palette.size() ? palette.get(id - 1) : Blocks.AIR.defaultBlockState();
    }

    /** Local palette IDs are runtime implementation details, never registry numeric IDs. */
    public int paletteId(BlockState state) {
        if (!validState(state)) return 0;
        Integer existing = paletteIds.get(state);
        if (existing != null) return existing;
        // Repeated replacement must not accumulate every previously used block state forever.
        if (palette.size() >= VoxelGrid.CELL_COUNT) compactPalette();
        int id = palette.size() + 1;
        palette.add(state);
        paletteIds.put(state, id);
        return id;
    }

    public boolean setCell(int x, int y, int z, BlockState state) {
        if (state == null) return false;
        int id = paletteId(state);
        if (id == 0 && !state.isAir()) return false;
        return changeCell(x, y, z, id);
    }

    public boolean removeCell(int x, int y, int z) { return changeCell(x, y, z, 0); }

    private boolean changeCell(int x, int y, int z, int id) {
        if (!grid.set(x, y, z, id)) return false;
        setChanged();
        if (level != null) {
            if (grid.isEmpty() && !level.isClientSide()) level.removeBlock(worldPosition, false);
            else {
                var state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
                if (!level.isClientSide()) {
                    // The support shape changes while the containing BlockState stays identical.
                    state.updateNeighbourShapes(level, worldPosition, Block.UPDATE_ALL);
                    level.updateNeighborsAt(worldPosition, state.getBlock());
                }
            }
        }
        return true;
    }

    public VoxelShape collisionShape() { refreshShapes(); return collision; }
    public VoxelShape outlineShape() { refreshShapes(); return outline; }

    /** Shape of one cell in container-local coordinates, for targeting and placement checks. */
    public VoxelShape cellShape(int x, int y, int z, boolean collisionShape) {
        int id = grid.get(x, y, z);
        return scaledShape(nativeShape(id, collisionShape), x, y, z);
    }

    /** Cells can hide their enclosed neighbours only when their actual model is an opaque cube. */
    public boolean occludesCell(int id) {
        BlockState state = stateForId(id);
        return !state.isAir() && state.isSolidRender()
                && Shapes.equal(nativeShape(id, false), Shapes.block());
    }

    private void refreshShapes() {
        if (shapeRevision == grid.revision()) return;
        collision = buildShape(true);
        outline = buildShape(false);
        shapeRevision = grid.revision();
    }

    private VoxelShape buildShape(boolean collisionShape) {
        VoxelGrid fullCubes = new VoxelGrid();
        List<VoxelShape> pieces = new ArrayList<>();
        for (int index = 0; index < VoxelGrid.CELL_COUNT; index++) {
            int id = grid.get(index);
            if (id == 0) continue;
            VoxelShape source = nativeShape(id, collisionShape);
            if (source.isEmpty()) continue;
            if (Shapes.equal(source, Shapes.block())) {
                fullCubes.set(index, 1);
            } else {
                int x = index % VoxelGrid.SIZE;
                int z = index / VoxelGrid.SIZE % VoxelGrid.SIZE;
                int y = index / (VoxelGrid.SIZE * VoxelGrid.SIZE);
                pieces.add(scaledShape(source, x, y, z));
            }
        }
        // Merge adjoining cubes before constructing Minecraft shapes. A full container is one box.
        for (var box : fullCubes.collisionBoxes()) {
            pieces.add(Shapes.create(box.minXNormalized(), box.minYNormalized(), box.minZNormalized(),
                    box.maxXNormalized(), box.maxYNormalized(), box.maxZNormalized()));
        }
        // Balanced, unoptimized unions avoid repeatedly rebuilding a growing 4096-cell shape.
        return union(pieces, 0, pieces.size()).optimize();
    }

    private VoxelShape nativeShape(int id, boolean collisionShape) {
        Map<Integer, VoxelShape> cache = collisionShape ? nativeCollision : nativeOutline;
        return cache.computeIfAbsent(id, unused -> shapeOf(stateForId(id), collisionShape));
    }

    public static VoxelShape shapeOf(BlockState state, boolean collisionShape) {
        if (!validState(state)) return Shapes.empty();
        return collisionShape
                ? state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)
                : state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    public static VoxelShape scaledShape(VoxelShape source, int x, int y, int z) {
        if (source.isEmpty()) return Shapes.empty();
        List<VoxelShape> pieces = new ArrayList<>();
        source.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> pieces.add(Shapes.create(
                (x + minX) * CELL_SIZE, (y + minY) * CELL_SIZE, (z + minZ) * CELL_SIZE,
                (x + maxX) * CELL_SIZE, (y + maxY) * CELL_SIZE, (z + maxZ) * CELL_SIZE)));
        return union(pieces, 0, pieces.size());
    }

    private static VoxelShape union(List<VoxelShape> shapes, int from, int to) {
        if (from == to) return Shapes.empty();
        if (to - from == 1) return shapes.get(from);
        int middle = (from + to) >>> 1;
        return Shapes.joinUnoptimized(union(shapes, from, middle), union(shapes, middle, to), BooleanOp.OR);
    }

    private static boolean validState(BlockState state) {
        return state != null && !state.isAir() && !state.liquid() && !(state.getBlock() instanceof MiniBlock);
    }

    /** Fallback for explosions/container removal; ordinary mining uses the targeted cell's loot. */
    public List<ItemStack> drops() {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (int i = 0; i < VoxelGrid.CELL_COUNT; i++) {
            Item item = stateForId(grid.get(i)).getBlock().asItem();
            if (item != Items.AIR) counts.merge(item, 1, Integer::sum);
        }
        List<ItemStack> drops = new ArrayList<>();
        counts.forEach((item, count) -> {
            int limit = new ItemStack(item).getMaxStackSize();
            for (int remaining = count; remaining > 0; remaining -= limit) {
                drops.add(new ItemStack(item, Math.min(remaining, limit)));
            }
        });
        return drops;
    }

    private void compactPalette() {
        int[] cells = grid.snapshot().materials();
        Map<BlockState, Integer> compactIds = new HashMap<>();
        List<BlockState> compact = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            BlockState state = stateForId(cells[i]);
            if (!validState(state)) { cells[i] = 0; continue; }
            Integer id = compactIds.get(state);
            if (id == null) {
                id = compact.size() + 1;
                compact.add(state);
                compactIds.put(state, id);
            }
            cells[i] = id;
        }
        palette.clear();
        palette.addAll(compact);
        paletteIds.clear();
        paletteIds.putAll(compactIds);
        invalidateShapes();
        grid.load(new VoxelGrid.Snapshot(cells));
    }

    private void invalidateShapes() {
        nativeCollision.clear();
        nativeOutline.clear();
        shapeRevision = -1;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (palette.size() > VoxelGrid.CELL_COUNT) compactPalette();
        output.putInt("schema", SCHEMA);
        output.putIntArray("cells", grid.snapshot().materials());
        var states = output.childrenList("palette");
        for (BlockState state : palette) states.addChild().store("state", BlockState.CODEC, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        palette.clear();
        paletteIds.clear();
        invalidateShapes();
        int[] cells = input.getIntArray("cells").orElse(new int[0]);
        if (input.getIntOr("schema", 1) == 1 && cells.length == 512) {
            // Convert the retired IDs before expanding, retaining the old build's exact dimensions.
            for (int i = 0; i < cells.length; i++) {
                MiniMaterial legacy = MiniMaterial.byId(cells[i]);
                cells[i] = legacy == null ? 0 : paletteId(legacy.state());
            }
            cells = VoxelGrid.Snapshot.expandLegacy8(cells).materials();
        } else if (input.getIntOr("schema", 1) == SCHEMA && cells.length == VoxelGrid.CELL_COUNT) {
            // Decode entries separately so an unknown block cannot shift the following palette IDs.
            for (var entry : input.childrenListOrEmpty("palette")) {
                if (palette.size() >= VoxelGrid.CELL_COUNT) break;
                BlockState state = entry.read("state", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
                if (!validState(state)) state = Blocks.AIR.defaultBlockState();
                palette.add(state);
                if (!state.isAir()) paletteIds.putIfAbsent(state, palette.size());
            }
            for (int i = 0; i < cells.length; i++) {
                if (cells[i] < 0 || stateForId(cells[i]).isAir()) cells[i] = 0;
            }
        } else {
            cells = new int[VoxelGrid.CELL_COUNT];
        }
        grid.load(new VoxelGrid.Snapshot(cells));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
