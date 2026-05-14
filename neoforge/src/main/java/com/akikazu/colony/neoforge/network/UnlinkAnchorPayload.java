package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.workzone.AnchorId;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Client → server: unlink the named anchor from whichever building currently owns it. Invoked by the per-anchor
 * "Unlink" button in the Building Work Zones tab.
 */
public record UnlinkAnchorPayload(AnchorId anchor) implements CustomPacketPayload
{
    public static final Type<UnlinkAnchorPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "unlink_anchor"));

    public static final StreamCodec<ByteBuf, UnlinkAnchorPayload> STREAM_CODEC = StreamCodec.of(
            UnlinkAnchorPayload::encode,
            UnlinkAnchorPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, UnlinkAnchorPayload payload)
    {
        AnchorId.STREAM_CODEC.encode(buf, payload.anchor());
    }

    private static UnlinkAnchorPayload decode(ByteBuf buf)
    {
        return new UnlinkAnchorPayload(AnchorId.STREAM_CODEC.decode(buf));
    }
}
