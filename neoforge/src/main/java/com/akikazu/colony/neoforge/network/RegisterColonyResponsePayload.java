package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.colony.registration.RegistrationRejection;
import com.akikazu.colony.neoforge.ColonyMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import org.jspecify.annotations.Nullable;

import io.netty.buffer.ByteBuf;

/**
 * Server → client response to {@link RegisterColonyPayload}.
 *
 * <p>
 * On success the registered {@link ColonyId} is echoed back so the client can reconcile any optimistic state. On
 * rejection {@code errorReason} carries a wire code from {@link RegistrationRejection#wireCode()}; the client maps it
 * to a localized user-facing message.
 */
public record RegisterColonyResponsePayload(
        boolean success,
        @Nullable ColonyId id,
        @Nullable String errorReason)
        implements CustomPacketPayload
{

    public static final Type<RegisterColonyResponsePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID, "register_colony_response"));

    public static final StreamCodec<ByteBuf, RegisterColonyResponsePayload> STREAM_CODEC = new StreamCodec<>()
    {
        @Override
        public RegisterColonyResponsePayload decode(ByteBuf buf)
        {
            boolean success = ByteBufCodecs.BOOL.decode(buf);
            ColonyId id = null;

            if (buf.readBoolean())
            {
                id = ColonyId.STREAM_CODEC.decode(buf);
            }

            String reason = null;

            if (buf.readBoolean())
            {
                reason = ByteBufCodecs.STRING_UTF8.decode(buf);
            }

            return new RegisterColonyResponsePayload(success, id, reason);
        }

        @Override
        public void encode(ByteBuf buf, RegisterColonyResponsePayload value)
        {
            ByteBufCodecs.BOOL.encode(buf, value.success());

            ColonyId encodedId = value.id();

            if (encodedId != null)
            {
                buf.writeBoolean(true);
                ColonyId.STREAM_CODEC.encode(buf, encodedId);
            }
            else
            {
                buf.writeBoolean(false);
            }

            String encodedReason = value.errorReason();

            if (encodedReason != null)
            {
                buf.writeBoolean(true);
                ByteBufCodecs.STRING_UTF8.encode(buf, encodedReason);
            }
            else
            {
                buf.writeBoolean(false);
            }
        }
    };

    public static RegisterColonyResponsePayload accepted(ColonyId id)
    {
        return new RegisterColonyResponsePayload(true, id, null);
    }

    public static RegisterColonyResponsePayload rejected(RegistrationRejection reason)
    {
        return new RegisterColonyResponsePayload(false, null, reason.wireCode());
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
