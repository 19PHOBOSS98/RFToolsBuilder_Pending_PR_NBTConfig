package mcjty.rftoolsbuilder.modules.scanner.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
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
import net.minecraftforge.client.model.data.ModelData;

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

    private static List<BlockEntry> setupBlocks(BlockPos pos) {
        BlockState cobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState planks = Blocks.OAK_PLANKS.defaultBlockState();
        BlockState glass = Blocks.GLASS.defaultBlockState();
        return new BlockEntry.Builder()
                .add(pos, cobble)
                .add(pos.east(), cobble)
                .add(pos.west(), cobble)
                .add(pos.north(), cobble)
                .add(pos.south(), cobble)
                .add(pos.east().above(), glass)
                .add(pos.west().above(), glass)
                .add(pos.north().above(), glass)
                .add(pos.south().above(), glass)
                .add(pos.east().east(), planks)
                .add(pos.west().west(), planks)
                .add(pos.north().north(), planks)
                .add(pos.south().south(), planks)
                .add(pos.above(), Blocks.BAMBOO.defaultBlockState())
                .add(pos.east().south(), cobble)
                .add(pos.east().north(), cobble)
                .add(pos.west().south(), cobble)
                .add(pos.west().north(), cobble)
                .build();
    }

    public void render(PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.translate(0, 1, 0);
        poseStack.scale(.2f, .2f, .2f);
        int rot = (int)(System.currentTimeMillis() / 25L) % 360;
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));

        var usedLayers = RenderType.chunkBufferLayers();
        for (RenderType renderType : usedLayers) {
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
            shader.clear();
        }
        VertexBuffer.unbind();

        poseStack.popPose();
    }

    public void buildBuffer(BlockPos pos) {
        var blocks = setupBlocks(pos);
        BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        RandomSource random = Minecraft.getInstance().level.getRandom();
        BlockAndTintGetter level = new DummyBlockGetter(Minecraft.getInstance().level.registryAccess(), blocks);

        // Buffer building needs to use a disjoint PoseStack to build the buffer without camera position/orientation context
        PoseStack buildingPoseStack = new PoseStack();
        int relX = blocks.get(0).pos().getX();
        int relY = blocks.get(0).pos().getY();
        int relZ = blocks.get(0).pos().getZ();
        for (RenderType renderType : RenderType.chunkBufferLayers()) {
            BufferBuilder builder = fixedBuffers.builder(renderType);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            for (BlockEntry block : blocks) {
                buildingPoseStack.pushPose();
                buildingPoseStack.translate(block.pos().getX()-relX, block.pos().getY()-relY, block.pos().getZ()-relZ);
                BlockState state = block.state();
                blockRenderer.renderBatched(state, block.pos(), level, buildingPoseStack, builder, false, random, ModelData.EMPTY, renderType);
                buildingPoseStack.popPose();
            }
        }

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