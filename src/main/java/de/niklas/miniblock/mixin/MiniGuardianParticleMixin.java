package de.niklas.miniblock.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.niklas.miniblock.client.ParticleViewScale;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ElderGuardianParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Elder-guardian effects use their own model group and never call quad extraction. */
@Mixin(targets = "net.minecraft.client.particle.ElderGuardianParticleGroup$ElderGuardianParticleRenderState", remap = false)
public abstract class MiniGuardianParticleMixin {
    @Redirect(method = "fromParticle", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private static void miniblock$sizeGuardianModel(PoseStack pose, float x, float y, float z,
                                                   ElderGuardianParticle particle, Camera camera, float partialTicks) {
        pose.translate(x, y, z);
        // Preserve the apparition's anchor by applying the size after its native translation.
        float scale = ParticleViewScale.modelScale(particle);
        pose.scale(scale, scale, scale);
    }
}
