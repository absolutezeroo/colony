package com.akikazu.colony.common.building.room;

import com.akikazu.colony.api.building.room.RoomRequirement;
import com.akikazu.colony.api.building.room.RoomRequirement.FunctionalBlockCountRequirement;
import com.akikazu.colony.api.building.room.RoomType;
import com.akikazu.colony.core.registry.Identifier;

import java.util.List;

/**
 * Built-in {@link RoomType} for the basic bedroom, registered under {@code colony:room/bedroom}.
 *
 * <p>
 * Carries the V1 requirement set: at least one bed, at most two. Quality scoring (windows, decorations, etc.) lands in
 * a later prompt; this type only gates the binary {@link com.akikazu.colony.api.building.room.RoomStatus valid/invalid}
 * decision.
 */
public final class BedroomType implements RoomType
{
    public static final Identifier ID = Identifier.of("colony", "room/bedroom");

    public static final Identifier BED_FUNCTION = Identifier.of("colony", "bed");

    public static final BedroomType INSTANCE = new BedroomType();

    private static final List<RoomRequirement> REQUIREMENTS = List.of(
            new FunctionalBlockCountRequirement(BED_FUNCTION, 1, 2));

    private BedroomType()
    {
    }

    @Override
    public Identifier id()
    {
        return ID;
    }

    @Override
    public List<RoomRequirement> requirements()
    {
        return REQUIREMENTS;
    }
}
