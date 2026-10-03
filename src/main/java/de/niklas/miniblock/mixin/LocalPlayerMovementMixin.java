package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.PlayerScale;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The local ascend/descend input reads ability speed directly instead of Player.getFlyingSpeed(). */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMovementMixin {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"), require = 1)
    private float miniblock$scaleVerticalCreativeFlight(Abilities abilities) {
        float nativeSpeed = abilities.getFlyingSpeed();
        return PlayerScale.isMini((LocalPlayer) (Object) this)
                ? (float) (nativeSpeed * PlayerScale.MINI_SCALE) : nativeSpeed;
    }
}
