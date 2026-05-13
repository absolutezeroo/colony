package com.akikazu.colony.api.colony;

import net.minecraft.core.BlockPos;

/**
 * Public contract for a colony observed at runtime. Implementations live in {@code :common}.
 *
 * <p>
 * Only the stable identity, display name, and Town Hall location are exposed at this layer. Mutable runtime state
 * (citizens, buildings, treasury) flows through dedicated views and snapshots so that addons compiled against {@code
 * :api} never assume an in-memory representation.
 */
public interface Colony
{
    ColonyId id();

    String name();

    BlockPos townHallPos();
}
