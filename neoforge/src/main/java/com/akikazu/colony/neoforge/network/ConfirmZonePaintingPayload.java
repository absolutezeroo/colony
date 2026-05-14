package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Client → server payload. The player pressed Enter with two corners painted; the server constructs the
 * {@link com.akikazu.colony.api.building.AxisAlignedOuterZone}, runs
 * {@link com.akikazu.colony.common.building.validation.ZoneValidator}, and on success places the Hut block and
 * registers the building. On failure, the pending placement is left intact so the player can adjust their corners.
 */
public record ConfirmZonePaintingPayload(BlockPos cornerA, BlockPos cornerB) implements CustomPacketPayload
{
    public static final Type<ConfirmZonePaintingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "confirm_zone_painting"));

    public static final StreamCodec<ByteBuf, ConfirmZonePaintingPayload> STREAM_CODEC = StreamCodec.of(
            ConfirmZonePaintingPayload::encode,
            ConfirmZonePaintingPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, ConfirmZonePaintingPayload payload)
    {
        BlockPos a = payload.cornerA();
        BlockPos b = payload.cornerB();
        buf.writeInt(a.getX());
        buf.writeInt(a.getY());
        buf.writeInt(a.getZ());
        buf.writeInt(b.getX());
        buf.writeInt(b.getY());
        buf.writeInt(b.getZ());
    }

    private static ConfirmZonePaintingPayload decode(ByteBuf buf)
    {
        int ax = buf.readInt();
        int ay = buf.readInt();
        int az = buf.readInt();
        int bx = buf.readInt();
        int by = buf.readInt();
        int bz = buf.readInt();

        return new ConfirmZonePaintingPayload(new BlockPos(ax, ay, az), new BlockPos(bx, by, bz));
    }
}
