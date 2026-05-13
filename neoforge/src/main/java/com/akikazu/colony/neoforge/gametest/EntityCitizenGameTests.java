package com.akikazu.colony.neoforge.gametest;

import com.akikazu.colony.api.citizen.CitizenId;
import com.akikazu.colony.common.citizen.entity.EntityCitizen;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.entity.ColonyEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ColonyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class EntityCitizenGameTests
{
    private EntityCitizenGameTests()
    {
    }

    @GameTest(template = "empty_3x3_platform")
    public static void spawnCitizenWithSummon(GameTestHelper helper)
    {
        EntityCitizen entity = helper.spawn(ColonyEntities.CITIZEN.get(), new BlockPos(1, 2, 1));

        helper.succeedWhen(() -> assertSpawnedCitizenInvariants(helper, entity));
    }

    @GameTest(template = "empty_3x3_platform")
    public static void citizenIdPersistsAcrossSaveReload(GameTestHelper helper)
    {
        EntityCitizen entity = helper.spawn(ColonyEntities.CITIZEN.get(), new BlockPos(1, 2, 1));
        CitizenId originalId = entity.getCitizenId();

        CompoundTag tag = new CompoundTag();
        entity.addAdditionalSaveData(tag);

        EntityCitizen restored = helper.spawn(ColonyEntities.CITIZEN.get(), new BlockPos(1, 2, 2));
        restored.readAdditionalSaveData(tag);

        helper.succeedWhen(() -> helper.assertTrue(
                restored.getCitizenId().equals(originalId),
                "CitizenId should round-trip through save data"));
    }

    private static void assertSpawnedCitizenInvariants(GameTestHelper helper, EntityCitizen entity)
    {
        helper.assertTrue(entity.getCitizenId() != null, "CitizenId should be assigned on spawn");
        helper.assertTrue(entity.displayName().equals("Unknown"),
                "Default display name should be 'Unknown'");
        helper.assertTrue(entity.colony().isEmpty(),
                "Citizen should have no colony affiliation on spawn");
    }
}
