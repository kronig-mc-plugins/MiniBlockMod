package de.niklas.miniblock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.niklas.miniblock.MiniBlockMod;
import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.core.VoxelGrid;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/** Render the actual native block models at one-sixteenth scale. */
@Mod.EventBusSubscriber(modid = MiniBlockMod.MODID, value = Dist.CLIENT)
public final class MiniBlockClient {
    private MiniBlockClient() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // A menu-only development smoke test must also validate the gameplay-only mining mixin.
        if (!FMLLoader.isProduction() && !MiniMiningState.class.isAssignableFrom(MultiPlayerGameMode.class))
            throw new IllegalStateException("MiniBlock client mining mixin was not applied");
        event.registerBlockEntityRenderer(MiniBlockMod.MINI_BLOCK_ENTITY.get(), MiniBlockRenderer::new);
    }

    private static final class MiniBlockRenderer implements BlockEntityRenderer<MiniBlockEntity, MiniRenderState> {
        private static final float CELL_SIZE = 1.0F / VoxelGrid.SIZE;
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
            state.occluding.clear();
            state.breakCell = -1;
            state.breakParts.clear();
            for (int materialId : state.cells) {
                if (materialId == 0 || state.models.containsKey(materialId)) {
                    continue;
                }
                BlockModelRenderState model = new BlockModelRenderState();
                modelResolver.update(model, entity.stateForId(materialId), DISPLAY_CONTEXT);
                state.models.put(materialId, model);
                if (entity.occludesCell(materialId)) state.occluding.add(materialId);
            }
            if (Minecraft.getInstance().gameMode instanceof MiniMiningState mining) {
                var selected = mining.miniblock$miningCell();
                if (selected != null && selected.pos().equals(entity.getBlockPos()) && mining.miniblock$miningStage() >= 0) {
                    var nativeState = entity.stateAt(selected.x(), selected.y(), selected.z());
                    var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(nativeState);
                    model.collectParts(RandomSource.create(nativeState.getSeed(entity.getBlockPos())), state.breakParts);
                    state.breakCell = VoxelGrid.index(selected.x(), selected.y(), selected.z());
                    state.breakStage = mining.miniblock$miningStage();
                    state.breakTranslucent = model.hasMaterialFlag(1);
                }
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
                int x = index % VoxelGrid.SIZE;
                int z = index / VoxelGrid.SIZE % VoxelGrid.SIZE;
                int y = index / (VoxelGrid.SIZE * VoxelGrid.SIZE);
                // Glass and partial models must never hide a neighbour's visible faces.
                if (x > 0 && x < VoxelGrid.SIZE - 1 && y > 0 && y < VoxelGrid.SIZE - 1 && z > 0 && z < VoxelGrid.SIZE - 1
                        && state.occluding.contains(state.cells[index - 1]) && state.occluding.contains(state.cells[index + 1])
                        && state.occluding.contains(state.cells[index - VoxelGrid.SIZE]) && state.occluding.contains(state.cells[index + VoxelGrid.SIZE])
                        && state.occluding.contains(state.cells[index - VoxelGrid.SIZE * VoxelGrid.SIZE])
                        && state.occluding.contains(state.cells[index + VoxelGrid.SIZE * VoxelGrid.SIZE])) continue;
                poses.pushPose();
                poses.translate(x * CELL_SIZE, y * CELL_SIZE, z * CELL_SIZE);
                poses.scale(CELL_SIZE, CELL_SIZE, CELL_SIZE);
                state.models.get(materialId).submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
                if (index == state.breakCell)
                    collector.submitBreakingBlockModel(poses, List.copyOf(state.breakParts), state.breakStage, state.breakTranslucent);
                poses.popPose();
            }
        }
    }

    private static final class MiniRenderState extends BlockEntityRenderState {
        private int[] cells = new int[0];
        private final Map<Integer, BlockModelRenderState> models = new HashMap<>();
        private final Set<Integer> occluding = new HashSet<>();
        private int breakCell = -1;
        private int breakStage;
        private boolean breakTranslucent;
        private final List<BlockStateModelPart> breakParts = new ArrayList<>();
    }
}
