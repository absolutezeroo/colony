package com.akikazu.colony.neoforge.client.render;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;
import com.akikazu.colony.common.building.validation.ZoneValidator;
import com.akikazu.colony.neoforge.network.PendingPlacementClientState;
import com.akikazu.colony.neoforge.network.ZonePaintingClientState;
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

import java.util.Optional;

/**
 * Renders the painted zone wireframe in real time while a pending placement is active. Color of the wireframe encodes
 * the local approximation of validity:
 *
 * <ul>
 * <li>RED — locally invalid (volume out of bounds or zone doesn't contain the hut target)</li>
 * <li>YELLOW — close to the volume ceiling (server may still reject for overlap/unloaded chunks)</li>
 * <li>GREEN — locally valid</li>
 * </ul>
 *
 * <p>
 * Until both corners are set, the overlay falls back to highlighting whichever corner(s) the player has placed so the
 * paint state is visible. Server-side validation still runs at confirm time — the client color is a hint, not a
 * guarantee.
 */
@OnlyIn(Dist.CLIENT)
public final class ZonePaintingOverlay
{
    private static final float OUTLINE_ALPHA = 0.85F;

    private static final float CORNER_RED = 0.95F;

    private static final float CORNER_GREEN = 0.65F;

    private static final float CORNER_BLUE = 0.10F;

    private static final int YELLOW_THRESHOLD = (ZoneValidator.MAX_VOLUME * 3) / 4;

    private ZonePaintingOverlay()
    {
    }

    public static void register()
    {
        NeoForge.EVENT_BUS.register(ZonePaintingOverlay.class);
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

        Optional<BlockPos> cornerA = ZonePaintingClientState.cornerA();
        Optional<BlockPos> cornerB = ZonePaintingClientState.cornerB();

        if (cornerA.isEmpty() && cornerB.isEmpty())
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());

        if (cornerA.isPresent() && cornerB.isPresent())
        {
            AxisAlignedOuterZone zone = AxisAlignedOuterZone.fromCorners(cornerA.get(), cornerB.get());
            float[] rgb = colorForZone(zone, snapshot.targetPos());
            renderZoneWireframe(pose, consumer, zone, rgb);
        }
        else
        {
            cornerA.ifPresent(pos -> renderSingleBlock(pose, consumer, pos));
            cornerB.ifPresent(pos -> renderSingleBlock(pose, consumer, pos));
        }

        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    private static float[] colorForZone(AxisAlignedOuterZone zone, BlockPos hutTarget)
    {
        int volume = zone.volume();

        if (volume < ZoneValidator.MIN_VOLUME || volume > ZoneValidator.MAX_VOLUME)
        {
            return new float[] { 0.95F, 0.10F, 0.10F };
        }

        if (!zone.contains(hutTarget))
        {
            return new float[] { 0.95F, 0.10F, 0.10F };
        }

        if (volume >= YELLOW_THRESHOLD)
        {
            return new float[] { 0.95F, 0.85F, 0.20F };
        }

        return new float[] { 0.30F, 0.85F, 0.30F };
    }

    private static void renderZoneWireframe(
            PoseStack pose,
            VertexConsumer consumer,
            AxisAlignedOuterZone zone,
            float[] rgb)
    {
        float x0 = zone.min().getX();
        float y0 = zone.min().getY();
        float z0 = zone.min().getZ();
        float x1 = zone.max().getX() + 1.0F;
        float y1 = zone.max().getY() + 1.0F;
        float z1 = zone.max().getZ() + 1.0F;

        renderBoxEdges(pose, consumer, x0, y0, z0, x1, y1, z1, rgb[0], rgb[1], rgb[2]);
    }

    private static void renderSingleBlock(PoseStack pose, VertexConsumer consumer, BlockPos pos)
    {
        float x0 = pos.getX();
        float y0 = pos.getY();
        float z0 = pos.getZ();
        float x1 = x0 + 1.0F;
        float y1 = y0 + 1.0F;
        float z1 = z0 + 1.0F;

        renderBoxEdges(pose, consumer, x0, y0, z0, x1, y1, z1, CORNER_RED, CORNER_GREEN, CORNER_BLUE);
    }

    private static void renderBoxEdges(
            PoseStack pose,
            VertexConsumer consumer,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float r,
            float g,
            float b)
    {
        edge(consumer, pose, x0, y0, z0, x1, y0, z0, r, g, b);
        edge(consumer, pose, x0, y0, z0, x0, y1, z0, r, g, b);
        edge(consumer, pose, x0, y0, z0, x0, y0, z1, r, g, b);

        edge(consumer, pose, x1, y1, z1, x0, y1, z1, r, g, b);
        edge(consumer, pose, x1, y1, z1, x1, y0, z1, r, g, b);
        edge(consumer, pose, x1, y1, z1, x1, y1, z0, r, g, b);

        edge(consumer, pose, x1, y0, z0, x1, y1, z0, r, g, b);
        edge(consumer, pose, x1, y0, z0, x1, y0, z1, r, g, b);
        edge(consumer, pose, x0, y1, z0, x0, y1, z1, r, g, b);
        edge(consumer, pose, x0, y1, z0, x1, y1, z0, r, g, b);
        edge(consumer, pose, x0, y0, z1, x0, y1, z1, r, g, b);
        edge(consumer, pose, x0, y0, z1, x1, y0, z1, r, g, b);
    }

    private static void edge(
            VertexConsumer consumer,
            PoseStack pose,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float r,
            float g,
            float bColor)
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
                .setColor(r, g, bColor, OUTLINE_ALPHA)
                .setNormal(pose.last(), dx, dy, dz);
        consumer.addVertex(pose.last().pose(), bx, by, bz)
                .setColor(r, g, bColor, OUTLINE_ALPHA)
                .setNormal(pose.last(), dx, dy, dz);
    }
}
