package de.niklas.miniblock.world;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

public final class MiniChiselItem extends Item {
    public MiniChiselItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        if (player == null || !player.mayBuild()) return InteractionResult.FAIL;
        var cell = MiniCell.atHit(context, false);
        if (!level.hasChunkAt(cell.pos()) || !level.mayInteract(player, cell.pos())
                || !player.mayUseItemAt(cell.pos(), context.getClickedFace(), context.getItemInHand()))
            return InteractionResult.FAIL;
        if (!(level.getBlockEntity(cell.pos()) instanceof MiniBlockEntity mini)) return InteractionResult.PASS;
        var material = MiniMaterial.byId(mini.grid().get(cell.x(), cell.y(), cell.z()));
        if (material == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        mini.setCell(cell.x(), cell.y(), cell.z(), 0);
        if (!player.getAbilities().instabuild) Block.popResource(level, cell.pos(), material.stack(1));
        level.playSound(null, cell.pos(), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.4F, 1.6F);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.miniblock.chisel"));
    }
}
