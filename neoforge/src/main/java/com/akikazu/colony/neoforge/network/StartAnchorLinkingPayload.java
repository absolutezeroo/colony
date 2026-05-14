package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.core.registry.Identifier;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

/**
 * Client → server: the player clicked "Add Anchor" on the Work Zones tab of a Building screen. Arms the
 * {@code (building, slotId)} pair as the active anchor link selection and answers with {@link ActivateLinkModePayload}
 * so the client closes its screen and the tool snaps into LINK mode.
 */
public record StartAnchorLinkingPayload(BuildingId building, Identifier slotId) implements CustomPacketPayload
{
    public static final Type<StartAnchorLinkingPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "start_anchor_linking"));

    public static final StreamCodec<ByteBuf, StartAnchorLinkingPayload> STREAM_CODEC = StreamCodec.of(
            StartAnchorLinkingPayload::encode,
            StartAnchorLinkingPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, StartAnchorLinkingPayload payload)
    {
        BuildingId.STREAM_CODEC.encode(buf, payload.building());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.slotId().namespace());
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.slotId().path());
    }

    private static StartAnchorLinkingPayload decode(ByteBuf buf)
    {
        BuildingId building = BuildingId.STREAM_CODEC.decode(buf);
        String ns = ByteBufCodecs.STRING_UTF8.decode(buf);
        String path = ByteBufCodecs.STRING_UTF8.decode(buf);

        return new StartAnchorLinkingPayload(building, Identifier.of(ns, path));
    }
}
