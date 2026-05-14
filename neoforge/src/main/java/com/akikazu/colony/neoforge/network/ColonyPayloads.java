package com.akikazu.colony.neoforge.network;

import com.akikazu.colony.api.building.BuildingId;
import com.akikazu.colony.api.item.ColonyToolMode;
import com.akikazu.colony.common.building.BuildingIndex;
import com.akikazu.colony.common.building.BuildingMetadata;
import com.akikazu.colony.common.building.placement.BuildingPlacementService;
import com.akikazu.colony.common.building.placement.PendingPlacement;
import com.akikazu.colony.common.building.placement.PendingPlacementManager;
import com.akikazu.colony.common.building.validation.ZoneValidationError;
import com.akikazu.colony.common.building.validation.ZoneValidationError.DoesNotContainHutPos;
import com.akikazu.colony.common.building.validation.ZoneValidationError.OutsideLoadedChunks;
import com.akikazu.colony.common.building.validation.ZoneValidationError.OverlapsExistingBuilding;
import com.akikazu.colony.common.building.validation.ZoneValidationError.TooLarge;
import com.akikazu.colony.common.building.validation.ZoneValidationError.TooSmall;
import com.akikazu.colony.common.storage.impl.SlotSelectionManager;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.block.ColonyBlocks;
import com.akikazu.colony.neoforge.item.ColonyDataComponents;
import com.akikazu.colony.neoforge.item.ColonyItems;
import com.akikazu.colony.neoforge.item.ColonyToolItem;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;

/**
 * Central registration for Colony's {@code CustomPacketPayload}s. Versioned via the registrar string so that
 * incompatible client/server combos are rejected at handshake.
 *
 * <p>
 * C2S validation lives in {@link ColonyRegistrationProcessor}; the handler here is intentionally thin so wire and
 * gametest paths share the same decision logic.
 */
public final class ColonyPayloads
{
    public static final String VERSION = "1";

    private ColonyPayloads()
    {
    }

    public static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(VERSION);

        registrar.playToServer(
                RegisterColonyPayload.TYPE,
                RegisterColonyPayload.STREAM_CODEC,
                ColonyPayloads::handleRegisterColony);

        registrar.playToServer(
                SubscribePayload.TYPE,
                SubscribePayload.STREAM_CODEC,
                ColonyPayloads::handleSubscribe);

        registrar.playToServer(
                UnsubscribePayload.TYPE,
                UnsubscribePayload.STREAM_CODEC,
                ColonyPayloads::handleUnsubscribe);

        registrar.playToServer(
                CycleColonyToolModePayload.TYPE,
                CycleColonyToolModePayload.STREAM_CODEC,
                ColonyPayloads::handleCycleColonyToolMode);

        registrar.playToServer(
                CancelPendingPlacementPayload.TYPE,
                CancelPendingPlacementPayload.STREAM_CODEC,
                ColonyPayloads::handleCancelPendingPlacement);

        registrar.playToServer(
                ConfirmZonePaintingPayload.TYPE,
                ConfirmZonePaintingPayload.STREAM_CODEC,
                ColonyPayloads::handleConfirmZonePainting);

        registrar.playToServer(
                StartChestDesignationPayload.TYPE,
                StartChestDesignationPayload.STREAM_CODEC,
                ColonyPayloads::handleStartChestDesignation);

        registrar.playToClient(
                RegisterColonyResponsePayload.TYPE,
                RegisterColonyResponsePayload.STREAM_CODEC,
                ColonyPayloads::handleRegisterColonyResponse);

        registrar.playToClient(
                SetPendingPlacementClientPayload.TYPE,
                SetPendingPlacementClientPayload.STREAM_CODEC,
                ColonyPayloads::handleSetPendingPlacement);

        registrar.playToClient(
                ChestTypedClientPayload.TYPE,
                ChestTypedClientPayload.STREAM_CODEC,
                ColonyPayloads::handleChestTyped);

        registrar.playToClient(
                ActivateStorageModePayload.TYPE,
                ActivateStorageModePayload.STREAM_CODEC,
                ColonyPayloads::handleActivateStorageMode);
    }

    public static boolean hasRegistrationPermission(ServerPlayer player)
    {
        return player != null;
    }

    private static void handleRegisterColony(RegisterColonyPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer sender))
        {
            return;
        }

        ServerLevel level = sender.serverLevel();
        MinecraftServer server = sender.getServer();

        if (server == null)
        {
            return;
        }

        ColonyServerSession session = ColonyServerSession.get(server);
        ColonyRegistrationProcessor processor = new ColonyRegistrationProcessor(session.rateLimiter());

        RegisterColonyResponsePayload response = processor.process(
                level,
                sender.getUUID(),
                sender.position(),
                hasRegistrationPermission(sender),
                payload);

        if (response.success())
        {
            ColonyMod.LOGGER.info(
                    "Registered colony {} '{}' at {} for player {}",
                    payload.id(),
                    payload.name(),
                    payload.pos(),
                    sender.getUUID());
        }
        else
        {
            ColonyMod.LOGGER.warn(
                    "Rejected colony registration from {} (name='{}', pos={}): {}",
                    sender.getUUID(),
                    payload.name(),
                    payload.pos(),
                    response.errorReason());
        }

        PacketDistributor.sendToPlayer(sender, response);
    }

    private static void handleSubscribe(SubscribePayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer sender))
        {
            return;
        }

        MinecraftServer server = sender.getServer();

        if (server == null)
        {
            return;
        }

        ColonyServerSession.get(server).subscriptions().subscribe(sender.getUUID(), payload.colony());
    }

    private static void handleUnsubscribe(UnsubscribePayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer sender))
        {
            return;
        }

        MinecraftServer server = sender.getServer();

        if (server == null)
        {
            return;
        }

        ColonyServerSession.get(server).subscriptions().unsubscribe(sender.getUUID(), payload.colony());
    }

    private static void handleCycleColonyToolMode(CycleColonyToolModePayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer sender))
        {
            return;
        }

        MinecraftServer server = sender.getServer();

        if (server == null)
        {
            return;
        }

        InteractionHand hand = payload.hand();
        ItemStack stack = sender.getItemInHand(hand);

        if (!stack.is(ColonyItems.COLONY_TOOL.get()))
        {
            return;
        }

        ColonyServerSession session = ColonyServerSession.get(server);

        if (!session.toolCycleLimiter().tryAcquire(sender.getUUID(), server.getTickCount()))
        {
            return;
        }

        ColonyToolItem.setMode(stack, payload.newMode());
    }

    private static void handleCancelPendingPlacement(CancelPendingPlacementPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer sender))
        {
            return;
        }

        MinecraftServer server = sender.getServer();

        if (server == null)
        {
            return;
        }

        PendingPlacementManager manager = ColonyServerSession.get(server).pendingPlacements();
        Optional<PendingPlacement> removed = manager.cancel(sender.getUUID());

        if (removed.isEmpty())
        {
            return;
        }

        PacketDistributor.sendToPlayer(
                sender,
                new SetPendingPlacementClientPayload(null, BlockPos.ZERO));

        sender.sendSystemMessage(Component.translatable("colony.message.pending_placement.cancelled"));
    }

    private static void handleConfirmZonePainting(ConfirmZonePaintingPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer sender))
        {
            return;
        }

        MinecraftServer server = sender.getServer();

        if (server == null)
        {
            return;
        }

        ServerLevel level = sender.serverLevel();
        PendingPlacementManager manager = ColonyServerSession.get(server).pendingPlacements();
        Optional<PendingPlacement> pendingOpt = manager.get(sender.getUUID());

        if (pendingOpt.isEmpty())
        {
            return;
        }

        PendingPlacement pending = pendingOpt.get();

        BuildingPlacementService.Result result = BuildingPlacementService.get(level)
                .attemptPlacement(
                        sender.getUUID(),
                        pending,
                        payload.cornerA(),
                        payload.cornerB(),
                        ColonyBlocks.RESIDENCE_HUT.get());

        if (result instanceof BuildingPlacementService.Result.Invalid invalid)
        {
            ZoneValidationError firstError = invalid.validation().errors().get(0);
            sender.sendSystemMessage(translateZoneError(firstError));

            return;
        }

        manager.confirm(sender.getUUID());

        PacketDistributor.sendToPlayer(
                sender,
                new SetPendingPlacementClientPayload(null, BlockPos.ZERO));

        sender.sendSystemMessage(Component.translatable("colony.message.pending_placement.building_created"));
    }

    private static Component translateZoneError(ZoneValidationError error)
    {
        return switch (error)
        {
            case TooSmall too -> Component.translatable(
                    "colony.message.zone_validation.too_small", too.actual(), too.minRequired());
            case TooLarge too -> Component.translatable(
                    "colony.message.zone_validation.too_large", too.actual(), too.maxAllowed());
            case DoesNotContainHutPos miss -> Component.translatable(
                    "colony.message.zone_validation.does_not_contain_hut",
                    miss.hutPos().getX(),
                    miss.hutPos().getY(),
                    miss.hutPos().getZ());
            case OverlapsExistingBuilding overlap -> Component.translatable(
                    "colony.message.zone_validation.overlap", overlap.conflict().toString());
            case OutsideLoadedChunks outside -> Component.translatable(
                    "colony.message.zone_validation.outside_loaded_chunks");
        };
    }

    private static void handleRegisterColonyResponse(RegisterColonyResponsePayload payload, IPayloadContext context)
    {
        if (payload.success())
        {
            ColonyMod.LOGGER.info("Colony registration accepted: id={}", payload.id());

            return;
        }

        ColonyMod.LOGGER.info("Colony registration rejected: reason={}", payload.errorReason());
    }

    private static void handleSetPendingPlacement(SetPendingPlacementClientPayload payload, IPayloadContext context)
    {
        PendingPlacementClientState.apply(payload);
    }

    private static void handleChestTyped(ChestTypedClientPayload payload, IPayloadContext context)
    {
        ChestTypingClientState.apply(payload);
    }

    private static void handleStartChestDesignation(StartChestDesignationPayload payload, IPayloadContext context)
    {
        if (!(context.player() instanceof ServerPlayer sender))
        {
            return;
        }

        MinecraftServer server = sender.getServer();

        if (server == null)
        {
            return;
        }

        ServerLevel level = sender.serverLevel();
        BuildingId building = payload.building();
        Optional<BuildingMetadata> metadataOpt = BuildingIndex.get(level).find(building);

        if (metadataOpt.isEmpty())
        {
            return;
        }

        boolean hasSlot = metadataOpt.get().hutType().storageSlots().stream()
                .anyMatch(s -> s.slotId().equals(payload.slotId()));

        if (!hasSlot)
        {
            return;
        }

        SlotSelectionManager manager = ColonyServerSession.get(server).slotSelections();
        manager.arm(sender.getUUID(), building, payload.slotId(), server.getTickCount());

        ItemStack mainHand = sender.getMainHandItem();

        if (mainHand.is(ColonyItems.COLONY_TOOL.get()))
        {
            mainHand.set(ColonyDataComponents.TOOL_MODE.get(), ColonyToolMode.STORAGE);
        }
        else
        {
            ItemStack offHand = sender.getOffhandItem();

            if (offHand.is(ColonyItems.COLONY_TOOL.get()))
            {
                offHand.set(ColonyDataComponents.TOOL_MODE.get(), ColonyToolMode.STORAGE);
            }
        }

        PacketDistributor.sendToPlayer(sender, new ActivateStorageModePayload(building, payload.slotId()));
    }

    private static void handleActivateStorageMode(ActivateStorageModePayload payload, IPayloadContext context)
    {
        StorageDesignationClientState.apply(payload);
    }
}
