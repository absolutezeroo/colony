package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Client → server: the player clicked "Save and Close" on the scarecrow configuration screen. The server validates the
 * offsets against the configured min/max volume and, if accepted, mutates the corresponding
 * {@link com.akikazu.colony.neoforge.blockentity.ScarecrowBlockEntity} and syncs the result back via
 * {@link ScarecrowConfigurationSyncPayload}.
 */
public record UpdateScarecrowConfigurationPayload(
        BlockPos pos,
        int north,
        int south,
        int east,
        int west,
        int up,
        int down) implements CustomPacketPayload
{

    public static final Type<UpdateScarecrowConfigurationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "update_scarecrow_configuration"));

    public static final StreamCodec<ByteBuf, UpdateScarecrowConfigurationPayload> STREAM_CODEC = StreamCodec.of(
            UpdateScarecrowConfigurationPayload::encode,
            UpdateScarecrowConfigurationPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, UpdateScarecrowConfigurationPayload payload)
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

    private static UpdateScarecrowConfigurationPayload decode(ByteBuf buf)
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

        return new UpdateScarecrowConfigurationPayload(new BlockPos(x, y, z), north, south, east, west, up, down);
    }
}
