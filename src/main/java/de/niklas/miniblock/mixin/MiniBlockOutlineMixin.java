package de.niklas.miniblock.mixin;

import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.world.MiniCell;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the ray geometry of all cells, but draw selection around the single hit block. */
@Mixin(value = LevelExtractor.class, remap = false)
public abstract class MiniBlockOutlineMixin {
    @Inject(method = "extractBlockOutline", at = @At("RETURN"))
    private void miniblock$highlightSingleCell(Camera camera, LevelRenderState state, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (state.blockOutlineRenderState == null || minecraft.level == null
                || !(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !(minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof MiniBlockEntity mini)) return;
        MiniCell cell = MiniCell.atHit(minecraft.level, hit, false);
        state.blockOutlineRenderState = new BlockOutlineRenderState(cell.pos(), false,
                minecraft.options.highContrastBlockOutline().get(), mini.cellShape(cell.x(), cell.y(), cell.z(), false));
    }
}
