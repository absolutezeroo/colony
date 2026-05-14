package com.akikazu.colony.neoforge.client.render;

import com.akikazu.colony.neoforge.network.PendingPlacementClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * World-space overlay that draws a translucent wireframe at the target of the active pending placement. The matching
 * label "Place: {hutName}" is drawn screen-space by {@link PendingPlacementHudOverlay}. Registered on the NeoForge
 * event bus on the client only; the layer is a no-op when no pending placement is active so it costs nothing in the
 * common case.
 */
@OnlyIn(Dist.CLIENT)
public final class PendingPlacementOverlay
{
    private static final float OUTLINE_RED = 0.19F;

    private static final float OUTLINE_GREEN = 0.44F;

    private static final float OUTLINE_BLUE = 0.82F;

    private static final float OUTLINE_ALPHA = 0.85F;

    private PendingPlacementOverlay()
    {
    }

    public static void register()
    {
        NeoForge.EVENT_BUS.register(PendingPlacementOverlay.class);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            return;
        }

        PendingPlacementClientState.Snapshot snapshot = PendingPlacementClientState.current();

        if (snapshot == null)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        BlockPos target = snapshot.targetPos();

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        renderCubeEdges(pose, consumer, target);

        buffers.endBatch(RenderType.lines());

        pose.popPose();
    }

    private static void renderCubeEdges(PoseStack pose, VertexConsumer consumer, BlockPos target)
    {
        float x0 = target.getX();
        float y0 = target.getY();
        float z0 = target.getZ();
        float x1 = x0 + 1.0F;
        float y1 = y0 + 1.0F;
        float z1 = z0 + 1.0F;

        edge(consumer, pose, x0, y0, z0, x1, y0, z0);
        edge(consumer, pose, x0, y0, z0, x0, y1, z0);
        edge(consumer, pose, x0, y0, z0, x0, y0, z1);

        edge(consumer, pose, x1, y1, z1, x0, y1, z1);
        edge(consumer, pose, x1, y1, z1, x1, y0, z1);
        edge(consumer, pose, x1, y1, z1, x1, y1, z0);

        edge(consumer, pose, x1, y0, z0, x1, y1, z0);
        edge(consumer, pose, x1, y0, z0, x1, y0, z1);
        edge(consumer, pose, x0, y1, z0, x0, y1, z1);
        edge(consumer, pose, x0, y1, z0, x1, y1, z0);
        edge(consumer, pose, x0, y0, z1, x0, y1, z1);
        edge(consumer, pose, x0, y0, z1, x1, y0, z1);
    }

    private static void edge(
            VertexConsumer consumer,
            PoseStack pose,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz)
    {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float len = (float) Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));

        if (len > 0.0F)
        {
            dx /= len;
            dy /= len;
            dz /= len;
        }

        consumer.addVertex(pose.last().pose(), ax, ay, az)
                .setColor(OUTLINE_RED, OUTLINE_GREEN, OUTLINE_BLUE, OUTLINE_ALPHA)
                .setNormal(pose.last(), dx, dy, dz);
        consumer.addVertex(pose.last().pose(), bx, by, bz)
                .setColor(OUTLINE_RED, OUTLINE_GREEN, OUTLINE_BLUE, OUTLINE_ALPHA)
                .setNormal(pose.last(), dx, dy, dz);
    }
}
