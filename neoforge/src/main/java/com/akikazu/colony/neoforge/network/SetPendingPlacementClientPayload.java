package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.core.registry.Identifier;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import io.netty.buffer.ByteBuf;

import org.jspecify.annotations.Nullable;

/**
 * Server → client payload telling the local client that the player has just entered (or left) a pending hut placement.
 * {@code hutTypeId == null} signals "no longer pending" and the client clears its ghost preview.
 */
public record SetPendingPlacementClientPayload(@Nullable Identifier hutTypeId, BlockPos targetPos)
        implements CustomPacketPayload
{
    public static final Type<SetPendingPlacementClientPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "set_pending_placement"));

    public static final StreamCodec<ByteBuf, SetPendingPlacementClientPayload> STREAM_CODEC = StreamCodec.of(
            SetPendingPlacementClientPayload::encode,
            SetPendingPlacementClientPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    private static void encode(ByteBuf buf, SetPendingPlacementClientPayload payload)
    {
        Identifier id = payload.hutTypeId();

        if (id == null)
        {
            buf.writeBoolean(false);
        }
        else
        {
            buf.writeBoolean(true);
            ByteBufCodecs.STRING_UTF8.encode(buf, id.namespace());
            ByteBufCodecs.STRING_UTF8.encode(buf, id.path());
        }

        BlockPos pos = payload.targetPos();
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    private static SetPendingPlacementClientPayload decode(ByteBuf buf)
    {
        Identifier id = null;

        if (buf.readBoolean())
        {
            String ns = ByteBufCodecs.STRING_UTF8.decode(buf);
            String path = ByteBufCodecs.STRING_UTF8.decode(buf);
            id = Identifier.of(ns, path);
        }

        int x = buf.readInt();
        int y = buf.readInt();
        int z = buf.readInt();

        return new SetPendingPlacementClientPayload(id, new BlockPos(x, y, z));
    }
}
