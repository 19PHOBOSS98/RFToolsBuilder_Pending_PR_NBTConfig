package mcjty.rftoolsbuilder.modules.scanner.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Map;
import java.util.stream.Collectors;

public class RenderSystem {
    private final ChunkBufferBuilderPack fixedBuffers;
    private final Map<RenderType, VertexBuffer> buffers;

    public RenderSystem() {
        this.fixedBuffers = new ChunkBufferBuilderPack();
        this.buffers = RenderType.chunkBufferLayers().stream().collect(Collectors.toMap(key -> key, key -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
    }

    public void render(PoseStack poseStack, BlockPos pos, RandomSource random) {
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        var level = Minecraft.getInstance().level;
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        BlockState state = Blocks.BAMBOO_BLOCK.defaultBlockState();
        var model = blockRenderer.getBlockModel(state);
        ChunkRenderTypeSet renderTypes = model.getRenderTypes(state, random, ModelData.EMPTY);
        for (RenderType renderType : renderTypes) {
            BufferBuilder builder = fixedBuffers.builder(renderType);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            blockRenderer.renderBatched(state, pos, level, poseStack, builder,
                    false, random, ModelData.EMPTY, renderType);
        }

        for (RenderType renderType : renderTypes) {
            BufferBuilder builder = fixedBuffers.builder(renderType);
            BufferBuilder.RenderedBuffer renderedBuffer = builder.endOrDiscardIfEmpty();
            VertexBuffer buffer = buffers.get(renderType);
            buffer.bind();
            buffer.upload(renderedBuffer);
        }
        VertexBuffer.unbind();

        for (RenderType renderType : renderTypes) {
            VertexBuffer pBuffer = buffers.get(renderType);
            pBuffer.bind();
            pBuffer.draw();
        }
        VertexBuffer.unbind();

        poseStack.popPose();
    }
}
