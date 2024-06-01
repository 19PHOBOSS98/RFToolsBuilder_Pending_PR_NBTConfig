package mcjty.rftoolsbuilder.modules.scanner.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Map;
import java.util.stream.Collectors;

public class Renderer {
    private final ChunkBufferBuilderPack fixedBuffers;
    private final Map<RenderType, VertexBuffer> buffers;

    public Renderer() {
        this.fixedBuffers = new ChunkBufferBuilderPack();
        this.buffers = RenderType.chunkBufferLayers().stream().collect(Collectors.toMap(key -> key, key -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
    }

    public void render(PoseStack poseStack, BlockPos pos, RandomSource random) {
        var blockRenderer = Minecraft.getInstance().getBlockRenderer();
        var level = Minecraft.getInstance().level;

        BlockState state = Blocks.BAMBOO_BLOCK.defaultBlockState();
        var model = blockRenderer.getBlockModel(state);
        // Buffer building needs to use a disjoint PoseStack to build the buffer without camera position/orientation context
        PoseStack buildingPoseStack = new PoseStack();
        buildingPoseStack.pushPose();
        ChunkRenderTypeSet renderTypes = model.getRenderTypes(state, random, ModelData.EMPTY);
        for (RenderType renderType : renderTypes) {
            BufferBuilder builder = fixedBuffers.builder(renderType);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            blockRenderer.renderBatched(state, pos, level, buildingPoseStack, builder, false, random, ModelData.EMPTY, renderType);
        }
        buildingPoseStack.popPose();

        // Upload buffers
        for (RenderType renderType : renderTypes) {
            BufferBuilder builder = fixedBuffers.builder(renderType);
            BufferBuilder.RenderedBuffer renderedBuffer = builder.endOrDiscardIfEmpty();
            if (renderedBuffer == null) continue;

            VertexBuffer buffer = buffers.get(renderType);
            buffer.bind();
            buffer.upload(renderedBuffer);
        }
        VertexBuffer.unbind();

        // Pop off translation to BE position to render the buffer in world space
        poseStack.popPose();

        for (RenderType renderType : renderTypes) {
            // Setup GL state for render type
            renderType.setupRenderState();

            // Get the shader which was bound by the state setup
            ShaderInstance shader = RenderSystem.getShader();

            // Setup shader uniforms
            for (int i = 0; i < 12; i++) {
                int texId = RenderSystem.getShaderTexture(i);
                shader.setSampler("Sampler" + i, texId);
            }
            if (shader.MODEL_VIEW_MATRIX != null) {
                shader.MODEL_VIEW_MATRIX.set(poseStack.last().pose());
            }
            if (shader.PROJECTION_MATRIX != null) {
                shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
            }
            if (shader.COLOR_MODULATOR != null) {
                shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
            }
            if (shader.GLINT_ALPHA != null) {
                shader.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
            }
            if (shader.FOG_START != null) {
                shader.FOG_START.set(RenderSystem.getShaderFogStart());
            }
            if (shader.FOG_END != null) {
                shader.FOG_END.set(RenderSystem.getShaderFogEnd());
            }
            if (shader.FOG_COLOR != null) {
                shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
            }
            if (shader.FOG_SHAPE != null) {
                shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
            }
            if (shader.TEXTURE_MATRIX != null) {
                shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
            }
            if (shader.GAME_TIME != null) {
                shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
            }
            if (shader.CHUNK_OFFSET != null) {
                // Set up the offset between the camera and the position where the buffer's origin should be rendered
                Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                double offX = (double) pos.getX() - cam.x;
                double offY = (double) pos.getY() + 1 - cam.y;
                double offZ = (double) pos.getZ() - cam.z;
                shader.CHUNK_OFFSET.set((float) offX, (float) offY, (float) offZ);
            }

            // Setup lighting
            RenderSystem.setupShaderLights(shader);
            // Upload shader configuration
            shader.apply();

            // Bind and draw buffer
            VertexBuffer pBuffer = buffers.get(renderType);
            pBuffer.bind();
            pBuffer.draw();
            renderType.clearRenderState();

            // Cleanup shader state
            if (shader.CHUNK_OFFSET != null) {
                shader.CHUNK_OFFSET.set(0.0F, 0.0F, 0.0F);
            }
            shader.clear();
        }
        VertexBuffer.unbind();

        // Reinstate stack depth expected by BER
        poseStack.pushPose();
    }
}