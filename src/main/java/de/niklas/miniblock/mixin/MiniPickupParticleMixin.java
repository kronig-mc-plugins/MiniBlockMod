package de.niklas.miniblock.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.niklas.miniblock.client.ParticleViewScale;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Pickup particles are entity models, separate from ordinary particle quads. */
@Mixin(targets = "net.minecraft.client.particle.ItemPickupParticleGroup$State", remap = false)
public abstract class MiniPickupParticleMixin {
    @Redirect(method = "submit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"))
    private void miniblock$sizePickupModel(EntityRenderDispatcher dispatcher, EntityRenderState state,
                                         CameraRenderState camera, double x, double y, double z,
                                         PoseStack pose, SubmitNodeCollector collector) {
        float scale = ParticleViewScale.observerScale();
        if (scale == 1.0F) {
            dispatcher.submit(state, camera, x, y, z, pose, collector);
            return;
        }
        pose.pushPose();
        try {
            // Translate first: shrink model geometry without moving the pickup's flight trajectory.
            pose.translate(x, y, z);
            pose.scale(scale, scale, scale);
            dispatcher.submit(state, camera, 0.0, 0.0, 0.0, pose, collector);
        } finally {
            pose.popPose();
        }
    }
}
