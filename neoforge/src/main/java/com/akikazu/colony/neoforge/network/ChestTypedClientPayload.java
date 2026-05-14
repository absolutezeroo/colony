package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Server → client payload announcing that the chest at {@code position} has been typed to a storage slot, carrying the
 * role's particle color so the client can emit the role-coloured indicator without having to look up the role locally.
 */
public record ChestTypedClientPayload(BlockPos position, int particleColor) implements CustomPacketPayload
{
    public static final Type<ChestTypedClientPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "chest_typed"));

    public static final StreamCodec<ByteBuf, ChestTypedClientPayload> STREAM_CODEC = StreamCodec.of(
            ChestTypedClientPayload::encode,
            ChestTypedClientPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, ChestTypedClientPayload payload)
    {
        BlockPos pos = payload.position();
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(payload.particleColor());
    }

    private static ChestTypedClientPayload decode(ByteBuf buf)
    {
        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();
        int color = buf.readInt();

        return new ChestTypedClientPayload(new BlockPos(x, y, z), color);
    }
}
