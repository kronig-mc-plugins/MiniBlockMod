package de.niklas.miniblock.world;

import de.niklas.miniblock.MiniBlockMod;
import de.niklas.miniblock.core.VoxelGrid;
import de.niklas.miniblock.player.PlayerScale;
import de.niklas.miniblock.player.MiniSurfaceEffects;
import de.niklas.miniblock.player.PlayerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;

/** Ordinary item placement and vanilla mining rules, applied to individual miniature blocks. */
public final class MiniBlockInteractions {
    public static final double CELL_SIZE = 1.0 / VoxelGrid.SIZE;

    private MiniBlockInteractions() {}

    public static InteractionResult placeSmall(UseOnContext context, BlockItem item) {
        var level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.mayBuild() || player.isSpectator() || stack.isEmpty()) return InteractionResult.FAIL;
        MiniCell cell = MiniCell.atHit(context, true);
        BlockPos pos = cell.pos();
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack))
            return InteractionResult.FAIL;
        Block block = item.getBlock();
        // Nested inventories and multi-block structures need their own miniature simulation.
        // Reject these before consuming the ordinary item rather than losing its data or other half.
        if (!block.isEnabled(level.enabledFeatures()) || block instanceof EntityBlock || block instanceof DoorBlock
                || block instanceof BedBlock || block instanceof DoublePlantBlock || block instanceof GameMasterBlock) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.miniblock.placement.unsupported"));
            return InteractionResult.FAIL;
        }
        var existing = level.getBlockEntity(pos);
        if (existing instanceof MiniBlockEntity mini) {
            if (!mini.stateAt(cell.x(), cell.y(), cell.z()).isAir()) return InteractionResult.FAIL;
        } else if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) {
            return InteractionResult.FAIL;
        }
        BlockState state = block.getStateForPlacement(new MiniPlacementContext(context, cell));
        if (state == null || state.isAir()) return InteractionResult.FAIL;
        state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(state);
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) state = state.setValue(BlockStateProperties.WATERLOGGED, false);
        VoxelShape placedShape = MiniBlockEntity.scaledShape(MiniBlockEntity.shapeOf(state, true), cell.x(), cell.y(), cell.z());
        for (var localBox : placedShape.toAabbs()) {
            var bounds = localBox.move(pos).deflate(1.0e-7);
            if (!level.noCollision(null, bounds)
                    || !level.getEntities((Entity) null, bounds, entity -> !entity.isSpectator() && entity.isPickable()).isEmpty())
                return InteractionResult.FAIL;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(existing instanceof MiniBlockEntity)
                && !level.setBlock(pos, MiniBlockMod.MINI_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL))
            return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof MiniBlockEntity mini)
                || !mini.setCell(cell.x(), cell.y(), cell.z(), state)) return InteractionResult.FAIL;
        stack.consume(1, player);
        var sound = state.getSoundType(level, pos, player);
        Vec3 center = center(cell);
        level.playSound(null, center.x, center.y, center.z, sound.getPlaceSound(), SoundSource.BLOCKS,
                0.35F, sound.getPitch());
        Vec3 placedParticleOrigin = placedShape.isEmpty() ? center : placedShape.bounds().move(pos).getCenter();
        if (state.shouldSpawnTerrainParticles() && level instanceof ServerLevel server) PlayerNetwork.sendParticles(server,
                MiniSurfaceEffects.blockBurst(state, pos, placedParticleOrigin, (float) CELL_SIZE, 4,
                        new Vec3(CELL_SIZE * 0.15, CELL_SIZE * 0.15, CELL_SIZE * 0.15)));
        level.gameEvent(GameEvent.BLOCK_PLACE, center, GameEvent.Context.of(player, state));
        return InteractionResult.SUCCESS_SERVER;
    }

    /** A real outline ray, so server mining cannot harvest an arbitrary cell in a known container. */
    public static MiniCell target(Player player, BlockPos expectedPos) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(expectedPos)
                || !(player.level().getBlockEntity(expectedPos) instanceof MiniBlockEntity mini)) return null;
        MiniCell cell = MiniCell.atHit(player.level(), hit, false);
        return mini.stateAt(cell.x(), cell.y(), cell.z()).isAir() ? null : cell;
    }

    public static float destroyProgress(Player player, BlockGetter level, BlockPos pos) {
        MiniCell cell = target(player, pos);
        if (cell == null || !(level.getBlockEntity(pos) instanceof MiniBlockEntity mini)) return 0.0F;
        return mini.stateAt(cell.x(), cell.y(), cell.z()).getDestroyProgress(player, level, pos);
    }

    /** Called in the real vanilla destroyBlock path when its normal mining timer completes. */
    public static boolean breakCell(ServerPlayer player, GameType mode, BlockPos pos, MiniCell startedCell) {
        ServerLevel level = player.level();
        MiniCell cell = target(player, pos);
        if (cell == null || (startedCell != null && !startedCell.equals(cell))
                || !level.mayInteract(player, pos) || !level.getWorldBorder().isWithinBounds(pos)
                || player.blockActionRestricted(level, pos, mode)
                || !(level.getBlockEntity(pos) instanceof MiniBlockEntity mini)) return false;
        BlockState state = mini.stateAt(cell.x(), cell.y(), cell.z());
        var sourceShape = mini.cellShape(cell.x(), cell.y(), cell.z(), false);
        Vec3 particleOrigin = sourceShape.isEmpty() ? center(cell) : sourceShape.bounds().move(pos).getCenter();
        ItemStack tool = player.getMainHandItem();
        if (state.getDestroySpeed(level, pos) < 0.0F && !player.getAbilities().instabuild) return false;
        if (!tool.canDestroyBlock(state, level, pos, player) || tool.onBlockStartBreak(pos, player)
                || ForgeHooks.onBlockBreakEvent(level, mode, player, pos) == -1) return false;
        ItemStack usedTool = tool.copy();
        boolean harvest = ForgeHooks.isCorrectToolForDrops(state, player);
        if (!player.preventsBlockDrops()) {
            tool.mineBlock(level, state, pos, player);
            if (tool.isEmpty() && !usedTool.isEmpty())
                ForgeEventFactory.onPlayerDestroyItem(player, usedTool, InteractionHand.MAIN_HAND);
        }
        if (!mini.removeCell(cell.x(), cell.y(), cell.z())) return false;
        Vec3 center = center(cell);
        if (!player.preventsBlockDrops() && harvest) {
            player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
            player.causeFoodExhaustion(0.005F);
            if (level.getGameRules().get(GameRules.BLOCK_DROPS)) {
                for (ItemStack drop : Block.getDrops(state, level, pos, null, player, usedTool)) {
                    ItemEntity entity = new ItemEntity(level, center.x, center.y, center.z, drop);
                    entity.setDefaultPickUpDelay();
                    entity.setDeltaMovement(Vec3.ZERO);
                    level.addFreshEntity(entity);
                }
            }
            ForgeHooks.dropXpForBlock(state, level, pos, usedTool);
        }
        var sound = state.getSoundType(level, pos, player);
        level.playSound(null, center.x, center.y, center.z, sound.getBreakSound(), SoundSource.BLOCKS, 0.35F, sound.getPitch());
        if (state.shouldSpawnTerrainParticles()) PlayerNetwork.sendParticles(level, MiniSurfaceEffects.blockBurst(state, pos, particleOrigin,
                (float) CELL_SIZE, 8, new Vec3(CELL_SIZE * 0.3, CELL_SIZE * 0.3, CELL_SIZE * 0.3)));
        level.gameEvent(GameEvent.BLOCK_DESTROY, center, GameEvent.Context.of(player, state));
        return true;
    }

    public static Vec3 center(MiniCell cell) {
        return new Vec3(cell.pos().getX() + (cell.x() + 0.5) * CELL_SIZE,
                cell.pos().getY() + (cell.y() + 0.5) * CELL_SIZE,
                cell.pos().getZ() + (cell.z() + 0.5) * CELL_SIZE);
    }

    /** Native placement properties (e.g. stair direction and slab half) see cell-local hit coordinates. */
    private static final class MiniPlacementContext extends BlockPlaceContext {
        private final MiniCell cell;
        private MiniPlacementContext(UseOnContext context, MiniCell cell) {
            super(context);
            this.cell = cell;
        }
        @Override public BlockPos getClickedPos() { return cell == null ? super.getClickedPos() : cell.pos(); }
        @Override public boolean canPlace() { return true; }
        @Override public boolean replacingClickedOnBlock() { return true; }
        @Override public Vec3 getClickLocation() {
            Vec3 hit = super.getClickLocation();
            if (cell == null) return hit;
            Vec3 origin = center(cell).subtract(CELL_SIZE * 0.5, CELL_SIZE * 0.5, CELL_SIZE * 0.5);
            return new Vec3(cell.pos().getX() + Math.clamp((hit.x - origin.x) / CELL_SIZE, 0.0, 1.0),
                    cell.pos().getY() + Math.clamp((hit.y - origin.y) / CELL_SIZE, 0.0, 1.0),
                    cell.pos().getZ() + Math.clamp((hit.z - origin.z) / CELL_SIZE, 0.0, 1.0));
        }
    }
}
