package com.akikazu.colony.neoforge.client.particle;

import com.akikazu.colony.neoforge.network.ChestTypingClientState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import org.joml.Vector3f;

import java.util.Map;

/**
 * Renders the per-role coloured particle indicator on every chest the client knows is typed.
 *
 * <p>
 * Tick gated to every {@link #EMIT_INTERVAL_TICKS} server-equivalent ticks so the cumulative cost stays trivial even
 * with many typed chests; only chests within {@link #VISIBLE_RANGE_BLOCKS} of the local player emit, matching the
 * "subtle indicator visible nearby" UX from {@code docs/04-BUILDING-SYSTEM.md}. Reads {@link ChestTypingClientState}
 * which is hydrated by the {@code ChestTypedClientPayload} server push.
 */
@OnlyIn(Dist.CLIENT)
public final class TypedChestParticleHandler
{
    public static final int EMIT_INTERVAL_TICKS = 20;

    public static final double VISIBLE_RANGE_BLOCKS = 24.0D;

    private static final float PARTICLE_SCALE = 1.0F;

    private static final double VISIBLE_RANGE_SQ = VISIBLE_RANGE_BLOCKS * VISIBLE_RANGE_BLOCKS;

    private static int tickCounter;

    private TypedChestParticleHandler()
    {
    }

    public static void register()
    {
        NeoForge.EVENT_BUS.register(TypedChestParticleHandler.class);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        tickCounter += 1;

        if (tickCounter < EMIT_INTERVAL_TICKS)
        {
            return;
        }

        tickCounter = 0;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;

        if (level == null || player == null)
        {
            return;
        }

        Map<BlockPos, Integer> typed = ChestTypingClientState.snapshot();

        if (typed.isEmpty())
        {
            return;
        }

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();

        for (Map.Entry<BlockPos, Integer> entry : typed.entrySet())
        {
            BlockPos pos = entry.getKey();
            double dx = (pos.getX() + 0.5D) - playerX;
            double dy = (pos.getY() + 0.5D) - playerY;
            double dz = (pos.getZ() + 0.5D) - playerZ;
            double distSq = (dx * dx) + (dy * dy) + (dz * dz);

            if (distSq > VISIBLE_RANGE_SQ)
            {
                continue;
            }

            spawn(level, pos, entry.getValue());
        }
    }

    private static void spawn(ClientLevel level, BlockPos pos, int color)
    {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        DustParticleOptions options = new DustParticleOptions(new Vector3f(red, green, blue), PARTICLE_SCALE);

        level.addParticle(
                options,
                pos.getX() + 0.5D,
                pos.getY() + 1.05D,
                pos.getZ() + 0.5D,
                0.0D,
                0.02D,
                0.0D);
    }
}
