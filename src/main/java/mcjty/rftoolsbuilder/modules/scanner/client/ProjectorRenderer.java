package mcjty.rftoolsbuilder.modules.scanner.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mcjty.rftoolsbuilder.modules.scanner.ScannerModule;
import mcjty.rftoolsbuilder.modules.scanner.blocks.ProjectorTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import javax.annotation.Nonnull;


public class ProjectorRenderer implements BlockEntityRenderer<ProjectorTileEntity> {

    public ProjectorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(@Nonnull ProjectorTileEntity te, float partialTicks, @Nonnull PoseStack matrixStack, @Nonnull MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        Level level = te.getLevel();
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BlockState state = Blocks.BAMBOO_BLOCK.defaultBlockState();
        var model = blockRenderer.getBlockModel(state);
        ChunkRenderTypeSet renderTypes = model.getRenderTypes(state, level.random, ModelData.EMPTY);

        matrixStack.pushPose();
        matrixStack.translate(0, 1, 0);
        for (RenderType renderType : renderTypes) {
            blockRenderer.renderBatched(state, te.getBlockPos(), level, matrixStack, buffer.getBuffer(renderType),
                    false, level.random, ModelData.EMPTY, renderType);
        }
        matrixStack.popPose();
    }

    public static void register() {
        BlockEntityRenderers.register(ScannerModule.TYPE_PROJECTOR.get(), ProjectorRenderer::new);
    }
}
