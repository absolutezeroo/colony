package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.colony.registration.RegisterColonyValidator;
import com.akikazu.colony.common.colony.registration.RegistrationRateLimiter;
import com.akikazu.colony.common.colony.registration.RegistrationRejection;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Server-side decision path for an incoming colony registration request.
 *
 * <p>
 * Composes pure {@code :common} validators with the Minecraft-aware range check and the {@link ColonyIndex} write to
 * produce a single response payload. Takes the player UUID and position as plain values rather than a
 * {@code ServerPlayer} so the same code path can be exercised by gametest helpers that do not always have a real
 * connected client.
 *
 * <p>
 * Order matches {@code docs/07-NETWORKING.md}: permission → rate-limit (anti-spam) → name → range → state mutation.
 * Every rejected branch returns without mutating state.
 */
public final class ColonyRegistrationProcessor
{
    private final RegistrationRateLimiter rateLimiter;

    public ColonyRegistrationProcessor(RegistrationRateLimiter rateLimiter)
    {
        this.rateLimiter = rateLimiter;
    }

    public RegisterColonyResponsePayload process(
            ServerLevel level,
            UUID playerId,
            Vec3 playerPosition,
            boolean hasPermission,
            RegisterColonyPayload payload)
    {
        if (!hasPermission)
        {
            return RegisterColonyResponsePayload.rejected(RegistrationRejection.NO_PERMISSION);
        }

        if (!rateLimiter.tryAcquire(playerId))
        {
            return RegisterColonyResponsePayload.rejected(RegistrationRejection.RATE_LIMITED);
        }

        Optional<RegistrationRejection> nameRejection = RegisterColonyValidator.validateName(payload.name());

        if (nameRejection.isPresent())
        {
            return RegisterColonyResponsePayload.rejected(nameRejection.get());
        }

        BlockPos pos = payload.pos();
        double targetX = pos.getX() + 0.5D;
        double targetY = pos.getY() + 0.5D;
        double targetZ = pos.getZ() + 0.5D;

        boolean inRange = RegisterColonyValidator.isWithinRange(
                playerPosition.x,
                playerPosition.y,
                playerPosition.z,
                targetX,
                targetY,
                targetZ,
                RegisterColonyValidator.DEFAULT_RANGE_BLOCKS);

        if (!inRange)
        {
            return RegisterColonyResponsePayload.rejected(RegistrationRejection.OUT_OF_RANGE);
        }

        ColonyId id = payload.id();
        ColonyRegistrationService.Result result = ColonyRegistrationService.register(level, id, payload.name(), pos);

        if (!result.accepted())
        {
            return RegisterColonyResponsePayload.rejected(RegistrationRejection.DUPLICATE_ID);
        }

        return RegisterColonyResponsePayload.accepted(id);
    }
}
