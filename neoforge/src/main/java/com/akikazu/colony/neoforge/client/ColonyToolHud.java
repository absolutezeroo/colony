package com.akikazu.colony.neoforge.client;

import com.akikazu.colony.api.item.ColonyToolMode;
import com.akikazu.colony.neoforge.ColonyMod;
import com.akikazu.colony.neoforge.item.ColonyItems;
import com.akikazu.colony.neoforge.item.ColonyToolItem;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.List;

/**
 * Top-right HUD shown while the player holds a Colony Tool. Displays the current {@link ColonyToolMode}, a small
 * solid-color icon (placeholder textures are not shipped in this prompt), and mode-specific hint text.
 *
 * <p>
 * Registered as a {@link LayeredDraw.Layer} via {@link RegisterGuiLayersEvent} so it composites with the rest of the
 * vanilla HUD. The layer is a no-op when the player is not holding a Colony Tool, so it costs nothing in the common
 * case.
 */
@OnlyIn(Dist.CLIENT)
public final class ColonyToolHud
{
    public static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(ColonyMod.MOD_ID,
            "colony_tool_hud");

    private static final int MARGIN = 8;

    private static final int ICON_SIZE = 16;

    private static final int LINE_HEIGHT = 10;

    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final int HINT_COLOR = 0xFFB0B0B0;

    private static final int ZONE_COLOR = 0xFF3070D0;

    private static final int STORAGE_COLOR = 0xFF40A040;

    private static final int LINK_COLOR = 0xFFD0C040;

    private static final int INSPECT_COLOR = 0xFFE0E0E0;

    private ColonyToolHud()
    {
    }

    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, ColonyToolHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker)
    {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.options.hideGui)
        {
            return;
        }

        ItemStack stack = colonyToolStack(player.getMainHandItem(), player.getOffhandItem());

        if (stack.isEmpty())
        {
            return;
        }

        ColonyToolMode mode = ColonyToolItem.getMode(stack);
        Font font = minecraft.font;

        Component modeLabel = Component.translatable(
                "item.colony.colony_tool.mode." + mode.getName());
        Component header = Component.translatable("colony.gui.tooltip.colony_tool", modeLabel);
        List<Component> hints = hintsFor(mode);

        int screenWidth = graphics.guiWidth();
        int headerWidth = font.width(header);
        int maxWidth = headerWidth + ICON_SIZE + 4;

        for (Component hint : hints)
        {
            maxWidth = Math.max(maxWidth, font.width(hint) + ICON_SIZE + 4);
        }

        int x = screenWidth - MARGIN - maxWidth;
        int y = MARGIN;

        graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, colorFor(mode));
        graphics.drawString(font, header, x + ICON_SIZE + 4, y + 4, TEXT_COLOR, true);

        int hintY = y + ICON_SIZE + 4;

        for (Component hint : hints)
        {
            graphics.drawString(font, hint, x + ICON_SIZE + 4, hintY, HINT_COLOR, true);
            hintY += LINE_HEIGHT;
        }
    }

    private static ItemStack colonyToolStack(ItemStack mainHand, ItemStack offHand)
    {
        if (mainHand.is(ColonyItems.COLONY_TOOL.get()))
        {
            return mainHand;
        }

        if (offHand.is(ColonyItems.COLONY_TOOL.get()))
        {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static int colorFor(ColonyToolMode mode)
    {
        if (mode == ColonyToolMode.ZONE)
        {
            return ZONE_COLOR;
        }

        if (mode == ColonyToolMode.STORAGE)
        {
            return STORAGE_COLOR;
        }

        if (mode == ColonyToolMode.LINK)
        {
            return LINK_COLOR;
        }

        return INSPECT_COLOR;
    }

    private static List<Component> hintsFor(ColonyToolMode mode)
    {
        Component scroll = Component.translatable("item.colony.colony_tool.hint.scroll");
        Component action = Component.translatable("item.colony.colony_tool.hint." + mode.getName());

        return List.of(scroll, action);
    }
}
