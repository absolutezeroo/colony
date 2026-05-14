package com.akikazu.colony.neoforge.client.render;

import com.akikazu.colony.api.item.ColonyToolMode;
import com.akikazu.colony.neoforge.item.ColonyItems;
import com.akikazu.colony.neoforge.item.ColonyToolItem;
import com.akikazu.colony.neoforge.network.StorageDesignationClientState;
import com.akikazu.colony.neoforge.network.StorageDesignationClientState.Active;

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

import org.jspecify.annotations.Nullable;

/**
 * Bottom-centre HUD prompt shown while the player has a chest-typing slot armed. Tells the player to right-click chests
 * in the building and reminds them that Esc cancels.
 *
 * <p>
 * Registered as a {@link LayeredDraw.Layer} via {@link RegisterGuiLayersEvent}; the layer is a no-op when no slot is
 * armed so it costs nothing in the common case.
 */
@OnlyIn(Dist.CLIENT)
public final class StorageDesignationHudOverlay
{
    public static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath("colony",
            "storage_designation_hud");

    private static final int BACKGROUND_COLOR = 0x88000000;

    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final int PADDING = 6;

    private static final int Y_FROM_BOTTOM = 60;

    private StorageDesignationHudOverlay()
    {
    }

    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, StorageDesignationHudOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker)
    {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.options.hideGui)
        {
            return;
        }

        @Nullable
        Active active = StorageDesignationClientState.current();

        if (active == null)
        {
            return;
        }

        if (!holdingColonyToolInStorageMode(player))
        {
            StorageDesignationClientState.clear();

            return;
        }

        Font font = minecraft.font;
        Component prompt = Component.translatable("colony.gui.hud.storage_designation");
        int textWidth = font.width(prompt);
        int boxWidth = textWidth + (PADDING * 2);
        int boxHeight = font.lineHeight + (PADDING * 2);
        int x = (graphics.guiWidth() - boxWidth) / 2;
        int y = graphics.guiHeight() - Y_FROM_BOTTOM - boxHeight;

        graphics.fill(x, y, x + boxWidth, y + boxHeight, BACKGROUND_COLOR);
        graphics.drawString(font, prompt, x + PADDING, y + PADDING, TEXT_COLOR, true);
    }

    private static boolean holdingColonyToolInStorageMode(LocalPlayer player)
    {
        ItemStack stack = colonyToolStack(player.getMainHandItem(), player.getOffhandItem());

        if (stack.isEmpty())
        {
            return false;
        }

        return ColonyToolItem.getMode(stack) == ColonyToolMode.STORAGE;
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
}
