package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.PlayerScale;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MiniLivingMovementMixin {
    @Unique
    private double miniblock$movementScale() {
        return (Object) this instanceof Player player && PlayerScale.isMini(player) ? PlayerScale.MINI_SCALE : 1.0;
    }

    @ModifyConstant(method = "jumpFromGround", constant = @Constant(doubleValue = 0.2))
    private double miniblock$scaleSprintJump(double original) {
        return original * miniblock$movementScale();
    }

    @Inject(method = "getJumpBoostPower", at = @At("RETURN"), cancellable = true)
    private void miniblock$scaleJumpEffect(CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue((float) (callback.getReturnValue() * miniblock$movementScale()));
    }

    @ModifyConstant(method = "aiStep", constant = @Constant(doubleValue = 0.003))
    private double miniblock$scaleStoppedVelocity(double original) {
        return original * miniblock$movementScale();
    }

    @ModifyConstant(method = "aiStep", constant = @Constant(doubleValue = 9.0E-6))
    private double miniblock$scaleStoppedHorizontalVelocitySquared(double original) {
        double scale = miniblock$movementScale();
        return original * scale * scale;
    }
}
