package com.akikazu.colony.common.citizen.entity;

import com.akikazu.colony.api.citizen.Citizen;
import com.akikazu.colony.api.citizen.CitizenId;
import com.akikazu.colony.api.colony.ColonyId;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
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

    public CitizenId getCitizenId()
    {
        return citizenId;
    }

    @Override
    protected void registerGoals()
    {
    }

    // TODO(citizen-1.3): replace with ColonyPathNavigation once the custom node evaluator and path cache land.
    // Vanilla nav is a temporary placeholder — Mob.<init> requires a non-null navigation, so throwing here would crash
    // every spawn (including /summon and gametests). The custom nav swap is tracked under prompt 1.3.
    @Override
    protected PathNavigation createNavigation(Level level)
    {
        return new GroundPathNavigation(this, level);
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
    }
}
