package com.akikazu.colony.neoforge.client;

import com.akikazu.colony.neoforge.client.entity.EntityCitizenRenderer;
import com.akikazu.colony.neoforge.client.render.PendingPlacementHudOverlay;
import com.akikazu.colony.neoforge.client.render.PendingPlacementOverlay;
import com.akikazu.colony.neoforge.client.render.ZonePaintingOverlay;
import com.akikazu.colony.neoforge.entity.ColonyEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-only mod event listeners. Registered from {@link com.akikazu.colony.neoforge.ColonyMod} only when the loader
 * dist is {@link Dist#CLIENT}, so this class — and the renderer classes it references — never load on a dedicated
 * server.
 */
@OnlyIn(Dist.CLIENT)
public final class ColonyClientEvents
{
    private ColonyClientEvents()
    {
    }

    public static void register(IEventBus modEventBus)
    {
        modEventBus.addListener(ColonyClientEvents::onRegisterRenderers);
        modEventBus.addListener(ColonyToolHud::onRegisterGuiLayers);
        modEventBus.addListener(PendingPlacementHudOverlay::onRegisterGuiLayers);
        ColonyToolKeyBindings.register();
        PendingPlacementOverlay.register();
        ZonePaintingOverlay.register();
        PendingPlacementKeyHandler.register();
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(ColonyEntities.CITIZEN.get(), EntityCitizenRenderer::new);
    }
}
