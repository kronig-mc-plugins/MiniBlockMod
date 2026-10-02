package de.niklas.miniblock.mixin;

import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.world.MiniBlockInteractions;
import de.niklas.miniblock.world.MiniCell;
import de.niklas.miniblock.client.MiniMiningState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MultiPlayerGameMode.class, remap = false)
public abstract class MultiPlayerGameModeMixin implements MiniMiningState {
    @Shadow @Final private Minecraft minecraft;
    @Shadow private boolean isDestroying;
    @Shadow private float destroyProgress;
    @Shadow public abstract void stopDestroyBlock();
    @Unique private MiniCell miniblock$startedCell;

    @Override public MiniCell miniblock$miningCell() { return isDestroying ? miniblock$startedCell : null; }
    @Override public int miniblock$miningStage() {
        return isDestroying && destroyProgress > 0.0F ? Math.clamp((int) (destroyProgress * 10), 0, 9) : -1;
    }

    @Inject(method = "startDestroyBlock", at = @At("HEAD"))
    private void miniblock$selectCell(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        MiniCell next = minecraft.player == null ? null : MiniBlockInteractions.target(minecraft.player, pos);
        if (isDestroying && !java.util.Objects.equals(next, miniblock$startedCell)) stopDestroyBlock();
        miniblock$startedCell = next;
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void miniblock$resetWhenChangingCell(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        MiniCell next = minecraft.player == null ? null : MiniBlockInteractions.target(minecraft.player, pos);
        if (isDestroying && !java.util.Objects.equals(next, miniblock$startedCell)) stopDestroyBlock();
    }

    @Inject(method = "stopDestroyBlock", at = @At("TAIL"))
    private void miniblock$clearCell(CallbackInfo ci) { miniblock$startedCell = null; }

    @Inject(method = "sameDestroyTarget", at = @At("RETURN"), cancellable = true)
    private void miniblock$compareIndividualTarget(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && minecraft.player != null && minecraft.level != null
                && minecraft.level.getBlockEntity(pos) instanceof MiniBlockEntity)
            cir.setReturnValue(java.util.Objects.equals(miniblock$startedCell, MiniBlockInteractions.target(minecraft.player, pos)));
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void miniblock$keepOtherCellsUntilServerUpdate(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof MiniBlockEntity)
            cir.setReturnValue(minecraft.player != null && MiniBlockInteractions.target(minecraft.player, pos) != null);
    }
}
