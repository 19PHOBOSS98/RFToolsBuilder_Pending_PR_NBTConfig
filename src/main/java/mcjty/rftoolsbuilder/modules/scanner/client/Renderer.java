package mcjty.rftoolsbuilder.modules.scanner.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Renderer {
    private final ChunkBufferBuilderPack fixedBuffers;
    private final Map<RenderType, VertexBuffer> buffers;

    public Renderer() {
        this.fixedBuffers = new ChunkBufferBuilderPack();
        this.buffers = RenderType.chunkBufferLayers().stream().collect(Collectors.toMap(key -> key, key -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
    }

    record Bl(BlockPos pos, BlockState state) {
        public static Bl of(BlockPos pos, BlockState state) {
            return new Bl(pos, state);
        }

        // A builder for creating a list of Bl objects
        public static class Builder {
            private final List<Bl> list = new ArrayList<>();

            public Builder add(BlockPos pos, BlockState state) {
                list.add(Bl.of(pos, state));
                return this;
            }

            public List<Bl> build() {
                return list;
            }
        }
    }

    public void render(PoseStack poseStack, BlockPos pos, RandomSource random) {
        var blocks = new Bl.Builder()
                .add(pos, Blocks.COBBLESTONE.defaultBlockState())
                .add(pos.east(), Blocks.COBBLESTONE.defaultBlockState())
                .add(pos.west(), Blocks.COBBLESTONE.defaultBlockState())
                .add(pos.north(), Blocks.COBBLESTONE.defaultBlockState())
                .add(pos.south(), Blocks.COBBLESTONE.defaultBlockState())
                .add(pos.above(), Blocks.BAMBOO.defaultBlockState())
                .build();

        buildBuffer(random, blocks);
        actualRender(poseStack, pos);
    }

    private void actualRender(PoseStack poseStack, BlockPos pos) {
        // Pop off translation to BE position to render the buffer in world space
        poseStack.popPose();

        var renderTypes = RenderType.chunkBufferLayers();
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
            VertexBuffer buffer = buffers.get(renderType);
            buffer.bind();
            buffer.draw();
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

    private void buildBuffer(RandomSource random, List<Bl> blocks) {
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        BlockAndTintGetter level = Minecraft.getInstance().level;

        // Buffer building needs to use a disjoint PoseStack to build the buffer without camera position/orientation context
        PoseStack buildingPoseStack = new PoseStack();
        buildingPoseStack.pushPose();
        for (RenderType renderType : RenderType.chunkBufferLayers()) {
            BufferBuilder builder = fixedBuffers.builder(renderType);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            for (Bl block : blocks) {
                BlockState state = block.state;
                blockRenderer.renderBatched(state, block.pos, level, buildingPoseStack, builder, false, random, ModelData.EMPTY, renderType);
            }
        }
        buildingPoseStack.popPose();

        // Upload buffers
        for (RenderType renderType : RenderType.chunkBufferLayers()) {
            BufferBuilder builder = fixedBuffers.builder(renderType);
            BufferBuilder.RenderedBuffer renderedBuffer = builder.endOrDiscardIfEmpty();
            if (renderedBuffer == null) continue;

            VertexBuffer buffer = buffers.get(renderType);
            buffer.bind();
            buffer.upload(renderedBuffer);
        }
        VertexBuffer.unbind();
    }
}