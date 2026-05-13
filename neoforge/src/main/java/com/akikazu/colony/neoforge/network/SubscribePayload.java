package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Client → server opt-in for the delta stream of a given {@link ColonyId}.
 *
 * <p>
 * Sent when the client opens a Colony GUI. The server tracks the subscription in {@link ColonySubscriptionService};
 * only subscribed players receive deltas for that colony. See {@code docs/07-NETWORKING.md} subscription model.
 */
public record SubscribePayload(ColonyId colony) implements CustomPacketPayload
{

    public static final Type<SubscribePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "subscribe"));

    public static final StreamCodec<ByteBuf, SubscribePayload> STREAM_CODEC = StreamCodec.composite(
            ColonyId.STREAM_CODEC, SubscribePayload::colony,
            SubscribePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
