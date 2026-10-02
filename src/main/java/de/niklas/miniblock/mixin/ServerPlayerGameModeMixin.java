package de.niklas.miniblock.mixin;

import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.world.MiniBlockInteractions;
import de.niklas.miniblock.world.MiniCell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerPlayerGameMode.class, remap = false)
public abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;
    @Shadow private GameType gameModeForPlayer;
    @Unique private MiniCell miniblock$startedCell;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void miniblock$rememberCell(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                      Direction direction, int maxY, int sequence, CallbackInfo ci) {
        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK)
            miniblock$startedCell = MiniBlockInteractions.target(player, pos);
        else if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) miniblock$startedCell = null;
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void miniblock$destroyOnlyOneCell(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (player.level().getBlockEntity(pos) instanceof MiniBlockEntity) {
            cir.setReturnValue(MiniBlockInteractions.breakCell(player, gameModeForPlayer, pos, miniblock$startedCell));
            miniblock$startedCell = null;
        }
    }
}
