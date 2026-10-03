package de.niklas.miniblock.gametest;

import de.niklas.miniblock.MiniBlockMod;
import de.niklas.miniblock.player.PlayerScale;
import de.niklas.miniblock.player.PlayerScaleGameTests;
import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.world.MiniBlockInteractions;
import de.niklas.miniblock.world.MiniCell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.RegisterEvent;

/** Exercise the actual vanilla-item and vanilla-server-mining entry points, including Mixins. */
@Mod.EventBusSubscriber(modid = MiniBlockMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MiniBlockGameTests {
    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        if (FMLLoader.isProduction()) return;
        event.register(Registries.TEST_FUNCTION, functions -> {
            functions.register("actual_world_collision_preserves_crawl_tunnel", MiniBlockGameTests::actualWorldCollisionPreservesCrawlTunnel);
            functions.register("ordinary_block_item_uses_player_size", MiniBlockGameTests::ordinaryBlockItemUsesPlayerSize);
            functions.register("ordinary_placement_crosses_cell_boundary", MiniBlockGameTests::ordinaryPlacementCrossesCellBoundary);
            functions.register("normal_pickaxe_breaks_one_cell", MiniBlockGameTests::normalPickaxeBreaksOneCell);
            functions.register("wrong_tool_and_creative_keep_vanilla_drops", MiniBlockGameTests::wrongToolAndCreativeKeepVanillaDrops);
            functions.register("unsupported_small_block_keeps_inventory", MiniBlockGameTests::unsupportedSmallBlockKeepsInventory);
            functions.register("changed_microcell_updates_attached_torch", MiniBlockGameTests::changedMicrocellUpdatesAttachedTorch);
            functions.register("miniature_player_can_grow_only_when_current_pose_fits", PlayerScaleGameTests::miniaturePlayerCanGrowOnlyWhenCurrentPoseFits);
            functions.register("miniature_jump_preserves_native_arc_and_sprint_control", PlayerScaleGameTests::miniatureJumpPreservesNativeArcAndSprintControl);
            functions.register("creative_flight_preserves_scale_and_native_ability_settings", PlayerScaleGameTests::creativeFlightPreservesScaleAndNativeAbilitySettings);
            functions.register("schema2_palette_round_trip", MiniStateGameTests::schema2PaletteRoundTrip);
            functions.register("legacy_eighth_world_keeps_dimensions", MiniStateGameTests::legacyEighthWorldKeepsDimensions);
            functions.register("invalid_palette_entries_keep_later_ids", MiniStateGameTests::invalidPaletteEntriesKeepLaterIds);
            functions.register("partial_block_shapes_keep_real_empty_space", MiniStateGameTests::partialBlockShapesKeepRealEmptySpace);
            functions.register("single_cell_removal_keeps_connected_neighbours", MiniStateGameTests::singleCellRemovalKeepsConnectedNeighbours);
        });
    }

    public static void actualWorldCollisionPreservesCrawlTunnel(GameTestHelper helper) {
        MiniBlockEntity mini = placeContainer(helper, ORIGIN);
        mini.grid().fill(mini.paletteId(Blocks.STONE.defaultBlockState()));
        for (int y = 0; y < 10; y++) for (int z = 0; z < 16; z++) for (int x = 2; x < 14; x++) mini.grid().set(x, y, z, 0);
        BlockPos absolute = helper.absolutePos(ORIGIN);
        for (int step = 0; step <= 4; step++) {
            helper.assertTrue(helper.getLevel().noCollision(null, bounds(absolute, 0.6, 0.6, step / 4.0)), "A native crawling player fits through the unchanged passage");
            helper.assertTrue(helper.getLevel().noCollision(null, bounds(absolute, 0.0375, 0.1125, step / 4.0)), "A sixteenth-scale standing player fits");
        }
        helper.assertFalse(helper.getLevel().noCollision(null, bounds(absolute, 0.6, 1.8, 0.5)), "Standing at full size collides with the ceiling");
        mini.setCell(8, 4, 8, Blocks.BRICKS.defaultBlockState());
        helper.assertFalse(helper.getLevel().noCollision(null, bounds(absolute, 0.6, 0.6, 0.5)), "Added miniature obstruction immediately updates cached geometry");
        mini.removeCell(8, 4, 8);
        helper.assertTrue(helper.getLevel().noCollision(null, bounds(absolute, 0.6, 0.6, 0.5)), "Removing one block restores the crawl passage");
        helper.succeed();
    }

    public static void ordinaryBlockItemUsesPlayerSize(GameTestHelper helper) {
        helper.setBlock(ORIGIN.below(), Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(2.8, 3.0, 2.8)));
        ItemStack stack = new ItemStack(Items.COBBLESTONE, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = helper.absolutePos(ORIGIN.below());
        var hit = new BlockHitResult(new Vec3(floor.getX() + 0.53, floor.getY() + 1.0, floor.getZ() + 0.53), Direction.UP, floor, false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        BlockItem item = (BlockItem) Items.COBBLESTONE;
        helper.assertTrue(item.useOn(context).consumesAction(), "The ordinary cobblestone item places normally when large");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(ORIGIN)).is(Blocks.COBBLESTONE), "Large placement is an ordinary vanilla block");
        helper.setBlock(ORIGIN, Blocks.AIR);
        PlayerScale.setMini(player, true);
        helper.assertTrue(item.useOn(context) == InteractionResult.SUCCESS_SERVER, "The same ordinary item uses the mini placement hook when small");
        MiniBlockEntity mini = helper.getBlockEntity(ORIGIN, MiniBlockEntity.class);
        helper.assertTrue(mini.stateAt(8, 0, 8).is(Blocks.COBBLESTONE), "Cobblestone is stored as its native block state");
        helper.assertValueEqual(mini.grid().occupiedCount(), 1, "One ordinary item places exactly one sixteenth block");
        helper.assertValueEqual(stack.getCount(), 1, "Each placement consumes one ordinary block item");
        helper.assertTrue(Math.abs(mini.collisionShape().bounds().getXsize() - 1.0 / 16.0) < 1.0e-8, "The resulting miniature block has exact one-sixteenth dimensions");
        helper.succeed();
    }

    public static void ordinaryPlacementCrossesCellBoundary(GameTestHelper helper) {
        MiniBlockEntity original = placeContainer(helper, ORIGIN);
        original.setCell(15, 4, 7, Blocks.STONE.defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(0.2, 3.0, 0.2)));
        PlayerScale.setMini(player, true);
        ItemStack stack = new ItemStack(Items.OAK_PLANKS, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(ORIGIN);
        var hit = new BlockHitResult(new Vec3(absolute.getX() + 1.0, absolute.getY() + 5.0 / 16.0, absolute.getZ() + 7.5 / 16.0), Direction.EAST, absolute, false);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(((BlockItem) Items.OAK_PLANKS).useOn(context) == InteractionResult.SUCCESS_SERVER, "Normal planks place across a container boundary at an exact tangent edge");
        MiniBlockEntity adjacent = helper.getBlockEntity(ORIGIN.east(), MiniBlockEntity.class);
        helper.assertTrue(adjacent.stateAt(0, 4, 7).is(Blocks.OAK_PLANKS), "The exact adjacent cell is occupied");
        helper.assertValueEqual(stack.getCount(), 1, "Successful miniature placement consumes one normal plank");
        helper.assertTrue(((BlockItem) Items.OAK_PLANKS).useOn(context) == InteractionResult.FAIL, "An occupied miniature cell rejects another placement");
        helper.assertValueEqual(stack.getCount(), 1, "Rejected placement keeps its item");
        helper.assertTrue(original.stateAt(15, 4, 7).is(Blocks.STONE), "The previous cell is not replaced");
        helper.succeed();
    }

    public static void normalPickaxeBreaksOneCell(GameTestHelper helper) {
        MiniBlockEntity mini = placeContainer(helper, ORIGIN);
        mini.setCell(7, 7, 7, Blocks.COBBLESTONE.defaultBlockState());
        mini.setCell(8, 7, 7, Blocks.COBBLESTONE.defaultBlockState());
        var player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        aimAt(player, new MiniCell(helper.absolutePos(ORIGIN), 7, 7, 7));
        float expected = Blocks.COBBLESTONE.defaultBlockState().getDestroyProgress(player, helper.getLevel(), helper.absolutePos(ORIGIN));
        helper.assertTrue(Math.abs(MiniBlockInteractions.destroyProgress(player, helper.getLevel(), helper.absolutePos(ORIGIN)) - expected) < 1.0e-8, "Native material and tool determine miniature mining speed");
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(ORIGIN)), "The real vanilla server mining endpoint breaks a miniature cell");
        helper.assertTrue(mini.stateAt(7, 7, 7).isAir(), "Only the targeted cell is removed");
        helper.assertTrue(mini.stateAt(8, 7, 7).is(Blocks.COBBLESTONE), "The connected neighbour remains independently placed");
        helper.assertValueEqual(tool.getDamageValue(), 1, "Normal pickaxe loses one durability point");
        helper.assertItemEntityCountIs(Items.COBBLESTONE, ORIGIN, 1.0, 1);
        aimAt(player, new MiniCell(helper.absolutePos(ORIGIN), 8, 7, 7));
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(ORIGIN)), "The remaining block can be mined independently");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(ORIGIN)).isAir(), "The empty internal container disappears after the last block");
        helper.assertValueEqual(tool.getDamageValue(), 2, "Each individual block consumes one durability point");
        helper.assertItemEntityCountIs(Items.COBBLESTONE, ORIGIN, 1.0, 2);
        helper.succeed();
    }

    public static void wrongToolAndCreativeKeepVanillaDrops(GameTestHelper helper) {
        MiniBlockEntity mini = placeContainer(helper, ORIGIN);
        mini.setCell(7, 7, 7, Blocks.STONE.defaultBlockState());
        mini.setCell(8, 7, 7, Blocks.COBBLESTONE.defaultBlockState());
        var survival = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        aimAt(survival, new MiniCell(helper.absolutePos(ORIGIN), 7, 7, 7));
        helper.assertTrue(survival.gameMode.destroyBlock(helper.absolutePos(ORIGIN)), "An empty hand can mine a stone cell without obtaining stone drops");
        helper.assertItemEntityCountIs(Items.COBBLESTONE, ORIGIN, 1.0, 0);
        var creative = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, tool);
        aimAt(creative, new MiniCell(helper.absolutePos(ORIGIN), 8, 7, 7));
        helper.assertTrue(creative.gameMode.destroyBlock(helper.absolutePos(ORIGIN)), "Creative mining removes only the aimed miniature block");
        helper.assertValueEqual(tool.getDamageValue(), 0, "Creative mining does not wear the normal tool");
        helper.assertItemEntityCountIs(Items.COBBLESTONE, ORIGIN, 1.0, 0);
        helper.succeed();
    }

    public static void unsupportedSmallBlockKeepsInventory(GameTestHelper helper) {
        helper.setBlock(ORIGIN.below(), Blocks.STONE);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerScale.setMini(player, true);
        ItemStack chest = new ItemStack(Items.CHEST, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, chest);
        BlockPos floor = helper.absolutePos(ORIGIN.below());
        var hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
        helper.assertTrue(((BlockItem) Items.CHEST).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)) == InteractionResult.FAIL, "Unsupported miniature inventories are rejected");
        helper.assertValueEqual(chest.getCount(), 2, "Rejected miniature chest keeps the whole item stack");
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(ORIGIN)).isAir(), "Rejected placement creates no empty storage container");
        helper.succeed();
    }

    public static void changedMicrocellUpdatesAttachedTorch(GameTestHelper helper) {
        MiniBlockEntity mini = placeContainer(helper, ORIGIN);
        mini.grid().fill(mini.paletteId(Blocks.STONE.defaultBlockState()));
        helper.setBlock(ORIGIN.above(), Blocks.TORCH);
        mini.removeCell(8, 15, 8);
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(ORIGIN.above())).isAir(), "Removing a miniature support block immediately updates its neighbour");
        helper.succeed();
    }

    private static void aimAt(ServerPlayer player, MiniCell cell) {
        Vec3 center = MiniBlockInteractions.center(cell);
        player.setPos(center.x, center.y - player.getEyeHeight(), center.z - 0.25);
        player.setYRot(0.0F);
        player.setXRot(0.0F);
    }

    private static MiniBlockEntity placeContainer(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, MiniBlockMod.MINI_BLOCK.get().defaultBlockState());
        return helper.getBlockEntity(relative, MiniBlockEntity.class);
    }

    private static AABB bounds(BlockPos pos, double width, double height, double centerZ) {
        double x = pos.getX() + 0.5 - width / 2.0, y = pos.getY() + 0.01, z = pos.getZ() + centerZ - width / 2.0;
        return new AABB(x, y, z, x + width, y + height, z + width);
    }
}
