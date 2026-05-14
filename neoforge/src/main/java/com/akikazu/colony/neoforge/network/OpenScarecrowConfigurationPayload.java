package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Server → client: the player right-clicked a scarecrow with an empty hand; instruct the client to open the scarecrow
 * configuration screen for the block entity at {@code pos}. The client resolves the scarecrow's current state from its
 * own block-entity copy (which is already client-synced via the standard block-entity update packet).
 */
public record OpenScarecrowConfigurationPayload(BlockPos pos) implements CustomPacketPayload
{
    public static final Type<OpenScarecrowConfigurationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "open_scarecrow_configuration"));

    public static final StreamCodec<ByteBuf, OpenScarecrowConfigurationPayload> STREAM_CODEC = StreamCodec.of(
            OpenScarecrowConfigurationPayload::encode,
            OpenScarecrowConfigurationPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, OpenScarecrowConfigurationPayload payload)
    {
        BlockPos pos = payload.pos();
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    private static OpenScarecrowConfigurationPayload decode(ByteBuf buf)
    {
        return new OpenScarecrowConfigurationPayload(new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()));
    }
}
