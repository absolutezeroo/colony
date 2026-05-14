package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.item.ColonyToolMode;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import io.netty.buffer.ByteBuf;

/**
 * Client → server payload requesting that the Colony Tool in the named hand cycle to {@code newMode}. The server is
 * authoritative: it re-checks the hand, that the player actually holds a Colony Tool, and a per-player tick cooldown
 * before mutating the stack's {@link com.akikazu.colony.neoforge.item.ColonyDataComponents#TOOL_MODE} component.
 */
public record CycleColonyToolModePayload(InteractionHand hand, ColonyToolMode newMode) implements CustomPacketPayload
{
    public static final Type<CycleColonyToolModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "cycle_colony_tool_mode"));

    private static final StreamCodec<ByteBuf, InteractionHand> HAND_STREAM_CODEC = ByteBufCodecs.idMapper(
            CycleColonyToolModePayload::handByOrdinal,
            CycleColonyToolModePayload::handNetworkId);

    public static final StreamCodec<ByteBuf, CycleColonyToolModePayload> STREAM_CODEC = StreamCodec.composite(
            HAND_STREAM_CODEC, CycleColonyToolModePayload::hand,
            ColonyToolMode.STREAM_CODEC, CycleColonyToolModePayload::newMode,
            CycleColonyToolModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static InteractionHand handByOrdinal(int ordinal)
    {
        InteractionHand[] hands = InteractionHand.values();

        if (ordinal < 0 || ordinal >= hands.length)
        {
            return InteractionHand.MAIN_HAND;
        }

        return hands[ordinal];
    }

    @SuppressWarnings("EnumOrdinal")
    private static int handNetworkId(InteractionHand hand)
    {
        return hand.ordinal();
    }
}
