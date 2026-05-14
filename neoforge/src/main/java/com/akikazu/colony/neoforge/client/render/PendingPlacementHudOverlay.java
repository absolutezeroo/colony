package com.akikazu.colony.neoforge.client.render;

import com.akikazu.colony.api.building.AxisAlignedOuterZone;
import com.akikazu.colony.common.bootstrap.ColonyBootstrap;
import com.akikazu.colony.common.building.validation.ZoneValidator;
import com.akikazu.colony.core.registry.Identifier;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.network.PendingPlacementClientState;
import com.akikazu.colony.neoforge.network.ZonePaintingClientState;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Optional;

/**
 * Screen-space companion to {@link PendingPlacementOverlay}: shows a top-centered "Place: {hutName}" label whenever a
 * pending hut placement is active. Reads from {@link PendingPlacementClientState}.
 *
 * <p>
 * The hut's display name is resolved through {@link ColonyBootstrap#hutTypesView()} so we render the same translated
 * label the server uses in chat acks — no duplicate map of identifier → display name on the client.
 */
@OnlyIn(Dist.CLIENT)
public final class PendingPlacementHudOverlay
{
    public static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(
            ColonyMod.MOD_ID, "pending_placement_hud");

    private static final int LABEL_COLOR = 0xFFFFFFFF;

    private static final int HINT_COLOR = 0xFFD0D0D0;

    private static final int LABEL_TOP_PADDING = 24;

    private static final int LINE_SPACING = 12;

    private PendingPlacementHudOverlay()
    {
    }

    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, PendingPlacementHudOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.options.hideGui)
        {
            return;
        }

        PendingPlacementClientState.Snapshot snapshot = PendingPlacementClientState.current();

        if (snapshot == null)
        {
            return;
        }

        Identifier hutId = snapshot.hutTypeId();
        Component name = ColonyBootstrap.hutTypesView()
                .get(hutId)
                .map(type -> type.displayName())
                .orElse(Component.literal(hutId.toString()));
        Component label = Component.translatable("colony.hud.pending_placement.label", name);

        Font font = minecraft.font;
        int width = font.width(label);
        int x = (graphics.guiWidth() - width) / 2;
        int y = LABEL_TOP_PADDING;

        graphics.drawString(font, label, x, y, LABEL_COLOR, true);

        Component subtitle = volumeOrCornerHint();
        int subtitleWidth = font.width(subtitle);
        int subtitleX = (graphics.guiWidth() - subtitleWidth) / 2;

        graphics.drawString(font, subtitle, subtitleX, y + LINE_SPACING, HINT_COLOR, true);
    }

    private static Component volumeOrCornerHint()
    {
        Optional<BlockPos> a = ZonePaintingClientState.cornerA();
        Optional<BlockPos> b = ZonePaintingClientState.cornerB();

        if (a.isPresent() && b.isPresent())
        {
            AxisAlignedOuterZone zone = AxisAlignedOuterZone.fromCorners(a.get(), b.get());

            return Component.translatable(
                    "colony.hud.pending_placement.volume",
                    zone.volume(),
                    ZoneValidator.MAX_VOLUME);
        }

        return Component.translatable("colony.hud.pending_placement.corners_missing");
    }
}
