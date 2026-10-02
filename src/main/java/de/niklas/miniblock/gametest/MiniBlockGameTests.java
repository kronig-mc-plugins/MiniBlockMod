package de.niklas.miniblock.gametest;

import de.niklas.miniblock.MiniBlockMod;
import de.niklas.miniblock.core.VoxelGrid;
import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.world.MiniMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.RegisterEvent;

/** Server integration checks, explicitly registered during development runs. */
@GameTestNamespace(MiniBlockMod.MODID)
@Mod.EventBusSubscriber(modid = MiniBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MiniBlockGameTests {
    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        if (FMLLoader.isProduction()) return;
        // Forge 66 does not connect its GameTest annotations to the vanilla test registries yet.
        event.register(Registries.TEST_FUNCTION, functions -> {
            functions.register("actual_world_collision_preserves_crawl_tunnel",
                    MiniBlockGameTests::actualWorldCollisionPreservesCrawlTunnel);
            functions.register("block_entity_persistence_and_update_tags_keep_cells",
                    MiniBlockGameTests::blockEntityPersistenceAndUpdateTagsKeepCells);
            functions.register("placement_and_chisel_cross_container_boundary",
                    MiniBlockGameTests::placementAndChiselCrossContainerBoundary);
            functions.register("miniature_player_can_grow_only_when_current_pose_fits",
                    de.niklas.miniblock.player.PlayerScaleGameTests::miniaturePlayerCanGrowOnlyWhenCurrentPoseFits);
            functions.register("changed_microcell_updates_attached_torch",
                    MiniBlockGameTests::changedMicrocellUpdatesAttachedTorch);
        });
    }

    @GameTest(structure = "miniblock:empty_3x3x3")
    public static void actualWorldCollisionPreservesCrawlTunnel(GameTestHelper helper) {
        MiniBlockEntity mini = placeContainer(helper, ORIGIN);
        mini.grid().fill(MiniMaterial.STONE.id);
        // Six eighths wide, five eighths high, open through both Z faces.
        for (int y = 0; y < 5; y++) {
            for (int z = 0; z < 8; z++) {
                for (int x = 1; x < 7; x++) {
                    mini.grid().set(x, y, z, 0);
                }
            }
        }
        BlockPos absolute = helper.absolutePos(ORIGIN);
        for (int step = 0; step <= 4; step++) {
            double centerZ = step / 4.0;
            helper.assertTrue(helper.getLevel().noCollision(null, bounds(absolute, 0.6, 0.6, centerZ)),
                    "A normal crawling player must fit through the 0.75 by 0.625 tunnel");
            helper.assertTrue(helper.getLevel().noCollision(null, bounds(absolute, 0.075, 0.225, centerZ)),
                    "The one-eighth player must fit through the same tunnel");
        }
        helper.assertFalse(helper.getLevel().noCollision(null, bounds(absolute, 0.6, 1.8, 0.5)),
                "A normal standing player must collide with the mini-block ceiling");

        // A mutation must immediately affect the world collision shape, including a cached shape.
        mini.grid().set(4, 2, 4, MiniMaterial.BRICKS.id);
        helper.assertFalse(helper.getLevel().noCollision(null, bounds(absolute, 0.6, 0.6, 0.5)),
                "Adding an obstruction must invalidate the cached collision shape");
        mini.grid().set(4, 2, 4, 0);
        helper.assertTrue(helper.getLevel().noCollision(null, bounds(absolute, 0.6, 0.6, 0.5)),
                "Removing the obstruction must restore the actual crawl passage");
        helper.succeed();
    }

    @GameTest(structure = "miniblock:empty_3x3x3")
    public static void blockEntityPersistenceAndUpdateTagsKeepCells(GameTestHelper helper) {
        MiniBlockEntity source = placeContainer(helper, ORIGIN);
        source.grid().set(0, 0, 0, MiniMaterial.STONE.id);
        source.grid().set(7, 6, 5, MiniMaterial.OAK_PLANKS.id);
        source.grid().set(3, 2, 1, MiniMaterial.BLUE_CONCRETE.id);
        var lookup = helper.getLevel().registryAccess();
        var saved = source.saveCustomOnly(lookup);
        MiniBlockEntity restored = new MiniBlockEntity(helper.absolutePos(ORIGIN), source.getBlockState());
        restored.collisionShape();
        restored.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, lookup, saved));
        helper.assertTrue(source.grid().snapshot().equals(restored.grid().snapshot()),
                "Saving and loading must preserve every material and cell coordinate");
        helper.assertTrue(source.collisionShape().toAabbs().equals(restored.collisionShape().toAabbs()),
                "A restored block entity must recreate the original collision geometry");

        MiniBlockEntity clientCopy = new MiniBlockEntity(helper.absolutePos(ORIGIN), source.getBlockState());
        clientCopy.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, lookup, source.getUpdateTag(lookup)));
        helper.assertTrue(source.grid().snapshot().equals(clientCopy.grid().snapshot()),
                "The block entity update tag must transfer the same mini-block geometry");

        int[] unknownMaterial = new int[VoxelGrid.CELL_COUNT];
        unknownMaterial[VoxelGrid.index(4, 4, 4)] = 9999;
        var invalid = saved.copy();
        invalid.putIntArray("cells", unknownMaterial);
        restored.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, lookup, invalid));
        helper.assertTrue(restored.grid().isEmpty() && restored.collisionShape().isEmpty(),
                "Unknown persisted materials must not create invisible collision walls");
        invalid.putIntArray("cells", new int[VoxelGrid.CELL_COUNT - 1]);
        restored.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, lookup, invalid));
        helper.assertTrue(restored.grid().isEmpty(), "Malformed cell arrays must load as empty");
        helper.succeed();
    }

    @GameTest(structure = "miniblock:empty_3x3x3")
    public static void placementAndChiselCrossContainerBoundary(GameTestHelper helper) {
        MiniBlockEntity original = placeContainer(helper, ORIGIN);
        original.grid().set(7, 2, 3, MiniMaterial.STONE.id);
        BlockPos absolute = helper.absolutePos(ORIGIN);
        BlockPos neighborRelative = ORIGIN.east();
        BlockPos neighborAbsolute = absolute.east();
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 playerPosition = helper.absoluteVec(new Vec3(0.3, 1.0, 0.3));
        player.setPos(playerPosition.x, playerPosition.y, playerPosition.z);
        ItemStack materialStack = MiniMaterial.OAK_PLANKS.stack(2);
        player.setItemInHand(InteractionHand.MAIN_HAND, materialStack);
        // An exact tangent edge must resolve to the occupied source cell, not its empty neighbor.
        var face = new BlockHitResult(new Vec3(absolute.getX() + 1.0, absolute.getY() + 3.0 / 8.0,
                absolute.getZ() + 3.5 / 8.0), Direction.EAST, absolute, false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, face);
        var materialItem = MiniBlockMod.MINI_ITEMS.get(MiniMaterial.OAK_PLANKS).get();
        helper.assertTrue(materialItem.useOn(context) == InteractionResult.SUCCESS_SERVER,
                "Placing against a boundary cell must succeed in the next world block");
        MiniBlockEntity neighbor = helper.getBlockEntity(neighborRelative, MiniBlockEntity.class);
        helper.assertValueEqual(neighbor.grid().get(0, 2, 3), MiniMaterial.OAK_PLANKS.id,
                "The neighbor must contain the exact adjacent cell");
        helper.assertValueEqual(neighbor.grid().occupiedCount(), 1, "Exactly one cell must be placed");
        helper.assertValueEqual(materialStack.getCount(), 1, "Survival placement must consume one mini block");
        helper.assertTrue(materialItem.useOn(context) == InteractionResult.FAIL,
                "Placing into the same occupied cell must fail");
        helper.assertValueEqual(materialStack.getCount(), 1, "A rejected placement must not consume an item");

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MiniBlockMod.CHISEL.get()));
        var outerFace = new BlockHitResult(new Vec3(neighborAbsolute.getX() + 1.0 / 8.0,
                neighborAbsolute.getY() + 3.0 / 8.0, neighborAbsolute.getZ() + 3.5 / 8.0),
                Direction.EAST, neighborAbsolute, false);
        helper.assertTrue(MiniBlockMod.CHISEL.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, outerFace))
                        == InteractionResult.SUCCESS_SERVER,
                "The chisel must remove the selected microcell");
        helper.assertTrue(helper.getLevel().getBlockState(neighborAbsolute).isAir(),
                "Removing the final cell must delete its empty container");
        helper.assertTrue(helper.getLevel().getBlockEntity(neighborAbsolute) == null,
                "The deleted container must leave no block entity behind");
        helper.assertValueEqual(original.grid().get(7, 2, 3), MiniMaterial.STONE.id,
                "Chiseling across the boundary must preserve the original cell");
        helper.assertItemEntityCountIs(materialItem, neighborRelative, 1.0, 1);
        helper.succeed();
    }

    @GameTest(structure = "miniblock:empty_3x3x3")
    public static void changedMicrocellUpdatesAttachedTorch(GameTestHelper helper) {
        MiniBlockEntity support = placeContainer(helper, ORIGIN);
        support.grid().fill(MiniMaterial.STONE.id);
        BlockPos torchPos = ORIGIN.above();
        helper.setBlock(torchPos, Blocks.TORCH);
        helper.assertTrue(Blocks.TORCH.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(torchPos)),
                "A filled mini-container must provide a valid torch support surface");
        support.setCell(4, 7, 4, 0);
        helper.assertTrue(!support.grid().isEmpty(), "The support container must remain after one cell is removed");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(torchPos)).isAir(),
                "Removing a central support cell must notify and detach the torch immediately");
        helper.succeed();
    }

    private static MiniBlockEntity placeContainer(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, MiniBlockMod.MINI_BLOCK.get().defaultBlockState());
        return helper.getBlockEntity(relative, MiniBlockEntity.class);
    }

    private static AABB bounds(BlockPos origin, double width, double height, double centerZ) {
        double minX = origin.getX() + 0.5 - width / 2.0;
        double minY = origin.getY() + 0.01;
        double minZ = origin.getZ() + centerZ - width / 2.0;
        return new AABB(minX, minY, minZ, minX + width, minY + height, minZ + width);
    }
}
