package com.akikazu.colony.neoforge.storage;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.storage.StorageRoleType;
import com.akikazu.colony.common.bootstrap.ColonyBootstrap;
import com.akikazu.colony.common.storage.impl.ChestTypingResult;
import com.akikazu.colony.common.storage.impl.ChestTypingResult.Reason;
import com.akikazu.colony.common.storage.impl.ChestTypingResult.Rejected;
import com.akikazu.colony.common.storage.impl.ChestTypingResult.Success;
import com.akikazu.colony.common.storage.impl.ChestTypingService;
import com.akikazu.colony.common.storage.impl.TypedChest;
import com.akikazu.colony.core.registry.Identifier;
import com.akikazu.colony.neoforge.network.ChestTypedClientPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;

import org.joml.Vector3f;

/**
 * Server-side entry point bridging the {@link ChestTypingService} result to player-visible feedback: chat message keyed
 * by the failure mode, role-coloured particle burst on success, and the {@link ChestTypedClientPayload} S2C so any
 * future client state tracking ({@code ChestTypingClientState}) updates.
 *
 * <p>
 * Lives in {@code :neoforge} because it touches NeoForge's {@link PacketDistributor} and Minecraft's particle pipeline;
 * the pure decision logic stays in {@link ChestTypingService} so {@code :common} unit tests can cover validation
 * without spinning up a level.
 */
public final class ChestTypingDispatcher
{
    private static final int PARTICLE_COUNT = 18;

    private static final double PARTICLE_SPREAD = 0.35D;

    private static final float PARTICLE_SCALE = 1.0F;

    private ChestTypingDispatcher()
    {
    }

    public static ChestTypingResult assign(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BuildingId building,
            Identifier slotId)
    {
        ChestTypingResult result = ChestTypingService.assignChest(level, player, pos, building, slotId);
        announce(level, player, result, slotId);

        return result;
    }

    private static void announce(ServerLevel level, ServerPlayer player, ChestTypingResult result, Identifier slotId)
    {
        switch (result)
        {
            case Success success -> onSuccess(level, player, success.typed());
            case Rejected rejected -> onRejected(player, rejected.reason(), slotId);
        }
    }

    private static void onRejected(ServerPlayer player, Reason reason, Identifier slotId)
    {
        switch (reason)
        {
            case NOT_A_CHEST -> player.sendSystemMessage(
                    Component.translatable("colony.message.storage.not_a_chest"));
            case OUTSIDE_BUILDING -> player.sendSystemMessage(
                    Component.translatable("colony.message.storage.not_inside_building"));
            case UNKNOWN_SLOT -> player.sendSystemMessage(
                    Component.translatable("colony.message.storage.unknown_slot", slotId.toString()));
            case CAPACITY_FULL -> player.sendSystemMessage(
                    Component.translatable("colony.message.storage.capacity_full", slotId.toString()));
            case ALREADY_TYPED -> player.sendSystemMessage(
                    Component.translatable("colony.message.storage.already_typed", slotId.toString()));
            case NO_PERMISSION -> player.sendSystemMessage(
                    Component.translatable("colony.message.storage.no_permission"));
        }
    }

    private static void onSuccess(ServerLevel level, ServerPlayer player, TypedChest chest)
    {
        int color = ColonyBootstrap.storageRolesView()
                .get(chest.role())
                .map(StorageRoleType::particleColor)
                .orElse(0xFFFFFF);

        spawnParticles(level, chest.position(), color);

        PacketDistributor.sendToPlayersTrackingChunk(
                level,
                new ChunkPos(chest.position()),
                new ChestTypedClientPayload(chest.position(), color));

        player.sendSystemMessage(Component.translatable(
                "colony.message.storage.assigned",
                chest.slotId().toString()));
    }

    private static void spawnParticles(ServerLevel level, BlockPos pos, int color)
    {
        float red = ((color >> 16) & 0xFF) / 255.0F;
        float green = ((color >> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;
        DustParticleOptions options = new DustParticleOptions(new Vector3f(red, green, blue), PARTICLE_SCALE);

        level.sendParticles(
                options,
                pos.getX() + 0.5D,
                pos.getY() + 1.1D,
                pos.getZ() + 0.5D,
                PARTICLE_COUNT,
                PARTICLE_SPREAD,
                PARTICLE_SPREAD,
                PARTICLE_SPREAD,
                0.0D);
    }
}
