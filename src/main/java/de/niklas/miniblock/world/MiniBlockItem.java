package de.niklas.miniblock.world;

import de.niklas.miniblock.MiniBlockMod;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.function.Consumer;

public final class MiniBlockItem extends Item {
    private final MiniMaterial material;

    public MiniBlockItem(MiniMaterial material, Properties properties) {
        super(properties);
        this.material = material;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        if (player == null || !player.mayBuild()) return InteractionResult.FAIL;
        var cell = MiniCell.atHit(context, true);
        var pos = cell.pos();
        if (!level.hasChunkAt(pos) || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack))
            return InteractionResult.FAIL;
        var existing = level.getBlockEntity(pos);
        if (existing instanceof MiniBlockEntity mini) {
            if (mini.grid().get(cell.x(), cell.y(), cell.z()) != 0) return InteractionResult.FAIL;
        } else if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) {
            return InteractionResult.FAIL;
        }
        double x = pos.getX() + cell.x() / 8.0;
        double y = pos.getY() + cell.y() / 8.0;
        double z = pos.getZ() + cell.z() / 8.0;
        var cellBounds = new AABB(x, y, z, x + 0.125, y + 0.125, z + 0.125);
        if (!level.noCollision(null, cellBounds.deflate(1.0e-7))
                || !level.getEntities((Entity) null, cellBounds, entity -> !entity.isSpectator() && entity.isPickable()).isEmpty())
            return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(existing instanceof MiniBlockEntity)) {
            if (!level.setBlock(pos, MiniBlockMod.MINI_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL))
                return InteractionResult.FAIL;
        }
        if (!(level.getBlockEntity(pos) instanceof MiniBlockEntity mini)) return InteractionResult.FAIL;
        mini.setCell(cell.x(), cell.y(), cell.z(), material.id);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.5F, 1.5F);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.miniblock.mini_block"));
    }
}
