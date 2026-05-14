package com.akikazu.colony.neoforge.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.akikazu.colony.neoforge.blockentity.ScarecrowBlockEntity;

/**
 * Client-side handler for {@link ScarecrowConfigurationSyncPayload}. Refreshes the local scarecrow block entity so the
 * configuration screen and the wireframe overlay both see the new zone offsets immediately. Lives in the
 * {@code network} package — no static imports of client GUI classes — so the dedicated server can load it for
 * dispatcher registration without dragging in client-only types.
 */
public final class ScarecrowConfigurationClientState
{
    private ScarecrowConfigurationClientState()
    {
    }

    public static void apply(ScarecrowConfigurationSyncPayload payload)
    {
        Level level = clientLevel();

        if (level == null)
        {
            return;
        }

        BlockEntity be = level.getBlockEntity(payload.pos());

        if (be instanceof ScarecrowBlockEntity scarecrow)
        {
            scarecrow.setWorkZoneOffsets(
                    payload.north(),
                    payload.south(),
                    payload.east(),
                    payload.west(),
                    payload.up(),
                    payload.down());
        }
    }

    private static Level clientLevel()
    {
        return Minecraft.getInstance().level;
    }
}
