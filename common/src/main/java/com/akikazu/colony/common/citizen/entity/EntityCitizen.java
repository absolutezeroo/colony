package com.akikazu.colony.common.citizen.entity;

import com.akikazu.colony.api.building.room.RoomId;
import com.akikazu.colony.api.citizen.Citizen;
import com.akikazu.colony.api.citizen.CitizenId;
import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.citizen.pathfinding.ColonyPathNavigation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Base custom entity for citizens. Extends {@link PathfinderMob} so that vanilla mob hooks (rendering, save, attack
 * targets) work out of the box; navigation and AI are deliberately not wired here so that the custom pathfinder and
 * intent queue land in later prompts without silent vanilla fallback.
 */
public class EntityCitizen extends PathfinderMob implements Citizen
{
    private CitizenId citizenId = CitizenId.random();
    private String displayName = "Unknown";
    private @Nullable ColonyId colony = null;
    private @Nullable RoomId assignedHomeRoom = null;

    public EntityCitizen(EntityType<? extends EntityCitizen> type, Level level)
    {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    public CitizenId id()
    {
        return citizenId;
    }

    @Override
    public String displayName()
    {
        return displayName;
    }

    @Override
    public Optional<ColonyId> colony()
    {
        return Optional.ofNullable(colony);
    }

    @Override
    public Optional<RoomId> assignedHomeRoom()
    {
        return Optional.ofNullable(assignedHomeRoom);
    }

    public CitizenId getCitizenId()
    {
        return citizenId;
    }

    public void setColony(@Nullable ColonyId colonyId)
    {
        this.colony = colonyId;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = java.util.Objects.requireNonNull(displayName, "displayName");
    }

    public void setAssignedHomeRoom(@Nullable RoomId room)
    {
        this.assignedHomeRoom = room;
    }

    @Override
    protected void registerGoals()
    {
    }

    @Override
    protected PathNavigation createNavigation(Level level)
    {
        return new ColonyPathNavigation(this, level);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag)
    {
        super.addAdditionalSaveData(tag);

        tag.putUUID("ColonyCitizenId", citizenId.value());
        tag.putString("DisplayName", displayName);

        if (colony != null)
        {
            tag.putUUID("ColonyId", colony.value());
        }

        if (assignedHomeRoom != null)
        {
            tag.putUUID("AssignedHomeRoom", assignedHomeRoom.value());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag)
    {
        super.readAdditionalSaveData(tag);

        if (tag.hasUUID("ColonyCitizenId"))
        {
            citizenId = new CitizenId(tag.getUUID("ColonyCitizenId"));
        }

        if (tag.contains("DisplayName"))
        {
            displayName = tag.getString("DisplayName");
        }

        if (tag.hasUUID("ColonyId"))
        {
            colony = new ColonyId(tag.getUUID("ColonyId"));
        }

        if (tag.hasUUID("AssignedHomeRoom"))
        {
            assignedHomeRoom = new RoomId(tag.getUUID("AssignedHomeRoom"));
        }
        else
        {
            assignedHomeRoom = null;
        }
    }
}
