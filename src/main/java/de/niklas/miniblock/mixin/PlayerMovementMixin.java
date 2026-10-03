package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.PlayerScale;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla's airborne steering and horizontal Creative flight bypass the movement-speed attribute. */
@Mixin(Player.class)
public abstract class PlayerMovementMixin {
    @Inject(method = "getFlyingSpeed", at = @At("RETURN"), cancellable = true)
    private void miniblock$scaleAirControl(CallbackInfoReturnable<Float> callback) {
        if (PlayerScale.isMini((Player) (Object) this)) {
            // Scale the effective result once, preserving vanilla's sprint multiplier and ability settings.
            callback.setReturnValue((float) (callback.getReturnValue() * PlayerScale.MINI_SCALE));
        }
    }
}
