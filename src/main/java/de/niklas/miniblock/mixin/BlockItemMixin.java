package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.PlayerScale;
import de.niklas.miniblock.world.MiniBlockInteractions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockItem.class, remap = false)
public abstract class BlockItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void miniblock$placeOrdinaryItem(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getPlayer() != null && PlayerScale.isMini(context.getPlayer()))
            cir.setReturnValue(MiniBlockInteractions.placeSmall(context, (BlockItem) (Object) this));
    }
}
