package de.niklas.miniblock.gametest;

import de.niklas.miniblock.MiniBlockMod;
import de.niklas.miniblock.core.VoxelGrid;
import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.world.MiniCell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;

/** Real registry codecs, world collisions and independent cells, registered by MiniBlockGameTests. */
@GameTestNamespace(MiniBlockMod.MODID)
public final class MiniStateGameTests {
    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);
    private MiniStateGameTests() {}

    @GameTest(structure = "miniblock:empty_3x3x3")
    public static void schema2PaletteRoundTrip(GameTestHelper helper) {
        MiniBlockEntity source = placeContainer(helper);
        BlockState topSlab = Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP);
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState stair = Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.WEST)
                .setValue(StairBlock.HALF, Half.TOP).setValue(StairBlock.SHAPE, StairsShape.INNER_LEFT);
        source.setCell(1, 2, 3, topSlab);
        source.setCell(15, 15, 15, glass);
        source.setCell(7, 8, 9, stair);
        var lookup = helper.getLevel().registryAccess();
        CompoundTag saved = source.saveCustomOnly(lookup);
        helper.assertValueEqual(saved.getIntOr("schema", 0), 2, "New containers must use schema 2");
        MiniBlockEntity restored = detachedCopy(helper, source, saved);
        assertPalette(helper, restored, topSlab, glass, stair);
        helper.assertTrue(Shapes.equal(source.collisionShape(), restored.collisionShape()),
                "The registry palette must preserve slab and rotated stair collision geometry");
        MiniBlockEntity synced = detachedCopy(helper, source, source.getUpdateTag(lookup));
        assertPalette(helper, synced, topSlab, glass, stair);
        helper.assertTrue(source.grid().snapshot().equals(synced.grid().snapshot()),
                "The block entity network update must preserve every cell and palette ID");
        helper.succeed();
    }

    @GameTest(structure = "miniblock:empty_3x3x3")
    public static void legacyEighthWorldKeepsDimensions(GameTestHelper helper) {
        MiniBlockEntity migrated = placeContainer(helper);
        int[] oldCells = new int[512];
        oldCells[3 + 8 * (5 + 8 * 7)] = 2; // Retired 0.1.0 cobblestone material ID.
        oldCells[0] = 99999; // Unknown retired IDs must produce neither geometry nor items.
        CompoundTag oldSave = new CompoundTag();
        oldSave.putInt("schema", 1);
        oldSave.putIntArray("cells", oldCells);
        migrated.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(), oldSave));
        helper.assertValueEqual(migrated.grid().occupiedCount(), 8, "An old cell must expand to 2 by 2 by 2 cells");
        for (int y = 14; y < 16; y++) {
            for (int z = 10; z < 12; z++) {
                for (int x = 6; x < 8; x++) helper.assertTrue(migrated.stateAt(x, y, z).is(Blocks.COBBLESTONE),
                        "Legacy material IDs must map to ordinary vanilla block states");
            }
        }
        var shape = migrated.collisionShape();
        helper.assertValueEqual(shape.toAabbs().size(), 1, "The migrated old cell must have one exact merged shape");
        helper.assertTrue(shape.bounds().equals(new AABB(3.0 / 8, 7.0 / 8, 5.0 / 8, 4.0 / 8, 1, 6.0 / 8)),
                "Existing builds must keep their exact world dimensions after switching to sixteenths");
        helper.assertTrue(migrated.stateAt(0, 0, 0).isAir(), "Unknown legacy materials must load as air");
        var drops = migrated.drops();
        helper.assertValueEqual(drops.size(), 1, "Only ordinary cobblestone items must be returned");
        helper.assertTrue(drops.getFirst().is(Blocks.COBBLESTONE.asItem()) && drops.getFirst().getCount() == 8,
                "Migrated independent cells must use ordinary cobblestone items");
        helper.succeed();
    }

    @GameTest(structure = "miniblock:empty_3x3x3")
    public static void invalidPaletteEntriesKeepLaterIds(GameTestHelper helper) {
        MiniBlockEntity source = placeContainer(helper);
        source.setCell(0, 0, 0, Blocks.STONE.defaultBlockState());
        source.setCell(1, 0, 0, Blocks.GLASS.defaultBlockState());
        CompoundTag save = source.saveCustomOnly(helper.getLevel().registryAccess());
        CompoundTag missingState = new CompoundTag();
        missingState.putString("Name", "miniblock:removed_unknown_block");
        save.getListOrEmpty("palette").getCompoundOrEmpty(0).put("state", missingState);
        int[] cells = save.getIntArray("cells").orElseThrow();
        cells[VoxelGrid.index(2, 0, 0)] = -3;
        cells[VoxelGrid.index(3, 0, 0)] = 999999;
        save.putIntArray("cells", cells);
        MiniBlockEntity restored = detachedCopy(helper, source, save);
        helper.assertTrue(restored.stateAt(0, 0, 0).isAir(), "An unknown block registry entry must load as air");
        helper.assertTrue(restored.stateAt(1, 0, 0).is(Blocks.GLASS),
                "A malformed first palette entry must never shift the later valid palette IDs");
        helper.assertTrue(restored.stateAt(2, 0, 0).isAir() && restored.stateAt(3, 0, 0).isAir(),
                "Negative and out of range IDs must produce no phantom cells");
        helper.assertValueEqual(restored.grid().occupiedCount(), 1, "Only the later valid glass cell may survive");
        helper.assertTrue(restored.collisionShape().bounds().equals(new AABB(1.0 / 16, 0, 0, 2.0 / 16, 1.0 / 16, 1.0 / 16)),
                "Unknown states must produce no invisible collision geometry");
        helper.succeed();
    }

    @GameTest(structure = "miniblock:empty_3x3x3")
    public static void partialBlockShapesKeepRealEmptySpace(GameTestHelper helper) {
        MiniBlockEntity mini = placeContainer(helper);
        mini.setCell(4, 4, 4, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        mini.setCell(8, 4, 4, Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        BlockPos absolute = helper.absolutePos(ORIGIN);
        helper.assertTrue(mini.cellShape(4, 4, 4, true).bounds().equals(
                        new AABB(4.0 / 16, 4.0 / 16, 4.0 / 16, 5.0 / 16, 4.5 / 16, 5.0 / 16)),
                "A bottom slab must retain its native half-height, scaled to one thirty-second of a block");
        helper.assertTrue(helper.getLevel().noCollision(null, microBounds(absolute, 4.1, 4.6, 4.1, 4.9, 4.9, 4.9)),
                "The empty upper half of a mini slab must remain traversable in the actual world");
        helper.assertFalse(helper.getLevel().noCollision(null, microBounds(absolute, 4.1, 4.1, 4.1, 4.9, 4.4, 4.9)),
                "The occupied lower half of a mini slab must still collide");
        helper.assertTrue(helper.getLevel().noCollision(null, microBounds(absolute, 8.6, 4.6, 4.6, 8.9, 4.9, 4.9)),
                "The missing upper south half of a north-facing mini stair must remain empty");
        helper.assertFalse(helper.getLevel().noCollision(null, microBounds(absolute, 8.1, 4.6, 4.1, 8.9, 4.9, 4.4)),
                "The upper north half of a north-facing mini stair must collide");
        // The top face lies inside the cell; an exact X tangent edge must still select this slab.
        var hit = new BlockHitResult(new Vec3(absolute.getX() + 5.0 / 16, absolute.getY() + 4.5 / 16,
                absolute.getZ() + 4.5 / 16), Direction.UP, absolute, false);
        helper.assertTrue(MiniCell.atHit(helper.getLevel(), hit, false).equals(new MiniCell(absolute, 4, 4, 4)),
                "Slab targeting must use the real partial face, including exact tangent edges");
        helper.succeed();
    }

    @GameTest(structure = "miniblock:empty_3x3x3")
    public static void singleCellRemovalKeepsConnectedNeighbours(GameTestHelper helper) {
        MiniBlockEntity mini = placeContainer(helper);
        for (int x = 0; x < VoxelGrid.SIZE; x++) mini.setCell(x, 0, 0, Blocks.COBBLESTONE.defaultBlockState());
        helper.assertValueEqual(mini.collisionShape().toAabbs().size(), 1, "A connected row may merge its collision boxes");
        BlockPos absolute = helper.absolutePos(ORIGIN);
        var hit = new BlockHitResult(new Vec3(absolute.getX() + 7.5 / 16, absolute.getY() + 1.0 / 16,
                absolute.getZ() + 0.5 / 16), Direction.UP, absolute, false);
        MiniCell cell = MiniCell.atHit(helper.getLevel(), hit, false);
        helper.assertTrue(cell.equals(new MiniCell(absolute, 7, 0, 0)),
                "Targeting must select the individual cell even when the outline is merged");
        helper.assertTrue(mini.removeCell(cell.x(), cell.y(), cell.z()), "Removing the selected cell must succeed");
        helper.assertValueEqual(mini.grid().occupiedCount(), 15, "One selected cell may remove exactly one block");
        helper.assertTrue(mini.stateAt(6, 0, 0).is(Blocks.COBBLESTONE) && mini.stateAt(8, 0, 0).is(Blocks.COBBLESTONE),
                "Directly connected neighbours must remain ordinary cobblestone cells");
        helper.assertValueEqual(mini.collisionShape().toAabbs().size(), 2,
                "The remaining row must split around the exact sixteenth-block gap");
        helper.assertTrue(helper.getLevel().noCollision(null, microBounds(absolute, 7.1, 0.1, 0.1, 7.9, 0.9, 0.9)),
                "Removing one cell must immediately open its exact space in actual world collision");
        helper.assertFalse(helper.getLevel().noCollision(null, microBounds(absolute, 8.1, 0.1, 0.1, 8.9, 0.9, 0.9)),
                "The next connected block must retain its collision");
        helper.succeed();
    }

    private static void assertPalette(GameTestHelper helper, MiniBlockEntity entity,
                                      BlockState slab, BlockState glass, BlockState stair) {
        helper.assertTrue(entity.stateAt(1, 2, 3).equals(slab), "The top slab's native properties must survive its codec");
        helper.assertTrue(entity.stateAt(15, 15, 15).equals(glass), "Glass must survive at the final sixteenth-cell coordinate");
        helper.assertTrue(entity.stateAt(7, 8, 9).equals(stair), "Facing, half and shape of the stair must survive its codec");
        helper.assertValueEqual(entity.grid().occupiedCount(), 3, "Each state must occupy exactly its own independent cell");
    }

    private static MiniBlockEntity placeContainer(GameTestHelper helper) {
        helper.setBlock(ORIGIN, MiniBlockMod.MINI_BLOCK.get().defaultBlockState());
        return helper.getBlockEntity(ORIGIN, MiniBlockEntity.class);
    }

    private static MiniBlockEntity detachedCopy(GameTestHelper helper, MiniBlockEntity source, CompoundTag data) {
        MiniBlockEntity copy = new MiniBlockEntity(source.getBlockPos(), source.getBlockState());
        copy.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), data));
        return copy;
    }

    private static AABB microBounds(BlockPos pos, double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ) {
        return new AABB(pos.getX() + minX / 16, pos.getY() + minY / 16, pos.getZ() + minZ / 16,
                pos.getX() + maxX / 16, pos.getY() + maxY / 16, pos.getZ() + maxZ / 16);
    }
}
