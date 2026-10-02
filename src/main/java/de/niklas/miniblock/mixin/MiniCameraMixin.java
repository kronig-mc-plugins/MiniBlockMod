package de.niklas.miniblock.mixin;

import de.niklas.miniblock.player.CameraGeometry;
import de.niklas.miniblock.player.PlayerScale;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the near plane, camera smoothing, and collision probes inside a miniature head. */
@Mixin(Camera.class)
public abstract class MiniCameraMixin {
    @Shadow private Entity entity;
    @Shadow private float eyeHeight;
    @Shadow private float eyeHeightOld;
    @Shadow private float fov;
    @Unique private float miniblock$previousScale = Float.NaN;
    @Unique private Entity miniblock$previousEntity;

    @Unique
    private float miniblock$cameraScale() {
        return entity instanceof Player player && PlayerScale.isMini(player) ? (float) PlayerScale.MINI_SCALE : 1.0F;
    }

    @ModifyConstant(method = {"update", "createProjectionMatrixForCulling"}, constant = @Constant(floatValue = 0.05F))
    private float miniblock$scaleNearPlane(float original) {
        Minecraft minecraft = Minecraft.getInstance();
        float aspect = (float) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        return CameraGeometry.nearPlane(original, miniblock$cameraScale(),
                Math.max(fov, minecraft.options.fov().get().floatValue()), aspect);
    }

    @ModifyConstant(method = "getMaxZoom", constant = @Constant(floatValue = 0.1F))
    private float miniblock$scaleThirdPersonCollisionProbe(float original) {
        return original * miniblock$cameraScale();
    }

    @Inject(method = "alignWithEntity", at = @At("HEAD"))
    private void miniblock$resetEyeHeightAfterSizeChange(float partialTicks, CallbackInfo callback) {
        float scale = miniblock$cameraScale();
        if (entity != miniblock$previousEntity || scale != miniblock$previousScale) {
            // Interpolating from the old, full-size eyes can put the camera inside a ceiling.
            eyeHeight = entity.getEyeHeight();
            eyeHeightOld = eyeHeight;
            miniblock$previousScale = scale;
            miniblock$previousEntity = entity;
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void miniblock$boundViewBob(CameraRenderState state, DeltaTracker deltaTracker, CallbackInfo callback) {
        float scale = miniblock$cameraScale();
        if (scale < 1.0F && state.entityRenderState.isPlayer) {
            float maximumBob = CameraGeometry.maximumViewBob(scale);
            state.entityRenderState.bob = Math.clamp(state.entityRenderState.bob, -maximumBob, maximumBob);
            state.entityRenderState.backwardsInterpolatedWalkDistance /= scale;
        }
    }
}
