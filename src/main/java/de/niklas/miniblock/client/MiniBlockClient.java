package de.niklas.miniblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.niklas.miniblock.MiniBlockMod;
import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.world.MiniMaterial;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/** Client rendering for the actual occupied eighth-block cells. */
@Mod.EventBusSubscriber(modid = MiniBlockMod.MODID, value = Dist.CLIENT)
public final class MiniBlockClient {
    private MiniBlockClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MiniBlockMod.MINI_BLOCK_ENTITY.get(), MiniBlockRenderer::new);
    }

    private static final class MiniBlockRenderer implements BlockEntityRenderer<MiniBlockEntity, MiniRenderState> {
        private static final float CELL_SIZE = 1.0F / 8.0F;
        private static final BlockDisplayContext DISPLAY_CONTEXT = BlockDisplayContext.create();
        private final BlockModelResolver modelResolver;

        private MiniBlockRenderer(BlockEntityRendererProvider.Context context) {
            modelResolver = context.blockModelResolver();
        }

        @Override
        public MiniRenderState createRenderState() {
            return new MiniRenderState();
        }

        @Override
        public void extractRenderState(MiniBlockEntity entity, MiniRenderState state, float partialTick,
                                       Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, cameraPosition, breakProgress);
            state.cells = entity.grid().snapshot().materials();
            state.models.clear();
            for (int materialId : state.cells) {
                if (materialId == 0 || state.models.containsKey(materialId)) {
                    continue;
                }
                BlockModelRenderState model = new BlockModelRenderState();
                modelResolver.update(model, MiniMaterial.byId(materialId).state(), DISPLAY_CONTEXT);
                state.models.put(materialId, model);
            }
        }

        @Override
        public void submit(MiniRenderState state, PoseStack poses, SubmitNodeCollector collector,
                           CameraRenderState cameraState) {
            for (int index = 0; index < state.cells.length; index++) {
                int materialId = state.cells[index];
                if (materialId == 0) {
                    continue;
                }
                int x = index & 7;
                int z = (index >> 3) & 7;
                int y = index >> 6;
                // All palette entries are solid cubes: fully enclosed cells have no visible face.
                // Keep boundary cells, since adjacent containers may not be loaded on this client.
                if (x > 0 && x < 7 && y > 0 && y < 7 && z > 0 && z < 7
                        && state.cells[index - 1] != 0 && state.cells[index + 1] != 0
                        && state.cells[index - 8] != 0 && state.cells[index + 8] != 0
                        && state.cells[index - 64] != 0 && state.cells[index + 64] != 0) continue;
                poses.pushPose();
                poses.translate(x * CELL_SIZE, y * CELL_SIZE, z * CELL_SIZE);
                poses.scale(CELL_SIZE, CELL_SIZE, CELL_SIZE);
                state.models.get(materialId).submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                poses.popPose();
            }
        }
    }

    private static final class MiniRenderState extends BlockEntityRenderState {
        private int[] cells = new int[0];
        private final Map<Integer, BlockModelRenderState> models = new HashMap<>();
    }
}
