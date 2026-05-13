package com.akikazu.colony.api.colony;

import com.akikazu.colony.api.citizen.CitizenId;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.ApiStatus;

import java.util.List;

/**
 * Public contract for a colony observed at runtime. Implementations live in {@code :common}.
 *
 * <p>
 * Marked {@link ApiStatus.NonExtendable} so addons compiled against this jar cannot ship their own {@code Colony}
 * subclass: the only legitimate producer is the {@code ColonyManager} service, which guarantees identity, persistence,
 * and event-bus invariants. Treat any reference returned from a public API as a read-only snapshot of state at call
 * time; mutation flows through dedicated services.
 */
@ApiStatus.NonExtendable
public interface Colony
{
    ColonyId id();

    String name();

    BlockPos townHallPos();

    ResourceKey<Level> dimension();

    long foundedAtTick();

    List<CitizenId> citizens();
}
