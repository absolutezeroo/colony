package com.akikazu.colony.neoforge.gametest;

import com.akikazu.colony.api.colony.ColonyId;
import com.akikazu.colony.common.colony.ColonyIndex;
import com.akikazu.colony.common.colony.ColonyMetadata;
import com.akikazu.colony.common.colony.registration.RegistrationRateLimiter;
import com.akikazu.colony.common.colony.registration.RegistrationRejection;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.network.ColonyRegistrationProcessor;
import com.akikazu.colony.neoforge.network.ColonyRegistrationService;
import com.akikazu.colony.neoforge.network.RegisterColonyPayload;
import com.akikazu.colony.neoforge.network.RegisterColonyResponsePayload;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.time.Duration;
import java.util.UUID;

@GameTestHolder(ColonyMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ColonyRegistrationGameTest
{
    private ColonyRegistrationGameTest()
    {
    }

    @GameTest(template = "empty_3x3x3")
    public static void colonyPersistsAcrossSaveAndReload(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        ColonyId id = ColonyId.random();
        String name = "TestColony";
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        ColonyRegistrationService.Result result = ColonyRegistrationService.register(level, id, name, pos);

        if (!result.accepted())
        {
            helper.fail("Expected colony registration to be accepted on first call.");

            return;
        }

        ColonyIndex live = ColonyIndex.get(level);

        if (!live.contains(id))
        {
            helper.fail("Expected colony id " + id + " in live index after registration.");

            return;
        }

        CompoundTag saved = live.save(new CompoundTag(), level.registryAccess());
        ColonyIndex reloaded = ColonyIndex.factory().deserializer().apply(saved, level.registryAccess());

        if (!reloaded.contains(id))
        {
            helper.fail("Expected colony id " + id + " in reloaded index after save/reload cycle.");

            return;
        }

        ColonyMetadata reloadedMetadata = reloaded.entries().get(id);

        if (reloadedMetadata == null)
        {
            helper.fail("Reloaded index missing colony metadata for id " + id + ".");

            return;
        }

        if (!pos.equals(reloadedMetadata.townHallPos()))
        {
            helper.fail("Town hall position was not preserved across reload.");

            return;
        }

        if (!level.dimension().equals(reloadedMetadata.dimension()))
        {
            helper.fail("Dimension was not preserved across reload.");

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void invalidNameProducesRejection(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        ColonyId id = ColonyId.random();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Vec3 playerPosition = Vec3.atCenterOf(pos);

        ColonyRegistrationProcessor processor = new ColonyRegistrationProcessor(
                new RegistrationRateLimiter(Duration.ofSeconds(30), System::currentTimeMillis));

        RegisterColonyPayload payload = new RegisterColonyPayload(id, "no!", pos);

        RegisterColonyResponsePayload response = processor.process(
                level,
                UUID.randomUUID(),
                playerPosition,
                true,
                payload);

        if (response.success())
        {
            helper.fail("Expected invalid registration to be rejected.");

            return;
        }

        if (response.id() != null)
        {
            helper.fail("Rejected response must not carry a colony id.");

            return;
        }

        if (!RegistrationRejection.NAME_CHARS.wireCode().equals(response.errorReason()))
        {
            helper.fail("Expected rejection reason '" + RegistrationRejection.NAME_CHARS.wireCode()
                    + "' but got '" + response.errorReason() + "'.");

            return;
        }

        ColonyIndex index = ColonyIndex.get(level);

        if (index.contains(id))
        {
            helper.fail("Rejected registration must not write to the colony index.");

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void outOfRangeProducesRejection(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        ColonyId id = ColonyId.random();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Vec3 farAway = Vec3.atCenterOf(pos).add(1000.0D, 0.0D, 0.0D);

        ColonyRegistrationProcessor processor = new ColonyRegistrationProcessor(
                new RegistrationRateLimiter(Duration.ofSeconds(30), System::currentTimeMillis));

        RegisterColonyPayload payload = new RegisterColonyPayload(id, "ValidName", pos);

        RegisterColonyResponsePayload response = processor.process(
                level,
                UUID.randomUUID(),
                farAway,
                true,
                payload);

        if (response.success())
        {
            helper.fail("Expected out-of-range registration to be rejected.");

            return;
        }

        if (!RegistrationRejection.OUT_OF_RANGE.wireCode().equals(response.errorReason()))
        {
            helper.fail("Expected rejection reason '" + RegistrationRejection.OUT_OF_RANGE.wireCode()
                    + "' but got '" + response.errorReason() + "'.");

            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty_3x3x3")
    public static void validRequestProducesAcceptance(GameTestHelper helper)
    {
        ServerLevel level = helper.getLevel();
        ColonyId id = ColonyId.random();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Vec3 playerPosition = Vec3.atCenterOf(pos);

        ColonyRegistrationProcessor processor = new ColonyRegistrationProcessor(
                new RegistrationRateLimiter(Duration.ofSeconds(30), System::currentTimeMillis));

        RegisterColonyPayload payload = new RegisterColonyPayload(id, "ValidName", pos);

        RegisterColonyResponsePayload response = processor.process(
                level,
                UUID.randomUUID(),
                playerPosition,
                true,
                payload);

        if (!response.success())
        {
            helper.fail("Expected valid registration to succeed but reason was: " + response.errorReason());

            return;
        }

        if (response.id() == null || !response.id().equals(id))
        {
            helper.fail("Accepted response must echo the registered colony id.");

            return;
        }

        if (!ColonyIndex.get(level).contains(id))
        {
            helper.fail("Accepted registration must write to the colony index.");

            return;
        }

        helper.succeed();
    }
}
