package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Server → client: the scarecrow at {@code pos} just had its zone offsets updated; nearby clients receive this to
 * refresh their local copy of the block entity (so wireframe overlays redraw without waiting for the next chunk send).
 */
public record ScarecrowConfigurationSyncPayload(
        BlockPos pos,
        int north,
        int south,
        int east,
        int west,
        int up,
        int down) implements CustomPacketPayload
{

    public static final Type<ScarecrowConfigurationSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "scarecrow_configuration_sync"));

    public static final StreamCodec<ByteBuf, ScarecrowConfigurationSyncPayload> STREAM_CODEC = StreamCodec.of(
            ScarecrowConfigurationSyncPayload::encode,
            ScarecrowConfigurationSyncPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, ScarecrowConfigurationSyncPayload payload)
    {
        BlockPos pos = payload.pos();
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(payload.north());
        buf.writeInt(payload.south());
        buf.writeInt(payload.east());
        buf.writeInt(payload.west());
        buf.writeInt(payload.up());
        buf.writeInt(payload.down());
    }

    private static ScarecrowConfigurationSyncPayload decode(ByteBuf buf)
    {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        int north = buf.readInt();
        int south = buf.readInt();
        int east = buf.readInt();
        int west = buf.readInt();
        int up = buf.readInt();
        int down = buf.readInt();

        return new ScarecrowConfigurationSyncPayload(new BlockPos(x, y, z), north, south, east, west, up, down);
    }
}
