package com.akikazu.colony.neoforge.client;

import com.akikazu.colony.api.item.ColonyToolMode;
import com.akikazu.colony.neoforge.item.ColonyItems;
import com.akikazu.colony.neoforge.item.ColonyToolItem;
import com.akikazu.colony.neoforge.network.CycleColonyToolModePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;

/**
 * Client-only listener that converts shift+scroll while holding the Colony Tool into a
 * {@link CycleColonyToolModePayload}. The mode change is applied client-side immediately so the HUD reflects it without
 * waiting for the server round-trip; the server's authoritative update on the {@link ItemStack} component will arrive
 * shortly after and either confirm or correct the local state via the synced data component.
 *
 * <p>
 * The scroll event is cancelled so the hotbar slot does not shift while cycling.
 */
@OnlyIn(Dist.CLIENT)
public final class ColonyToolKeyBindings
{
    private ColonyToolKeyBindings()
    {
    }

    public static void register()
    {
        NeoForge.EVENT_BUS.register(ColonyToolKeyBindings.class);
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event)
    {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null || !player.isShiftKeyDown())
        {
            return;
        }

        InteractionHand hand = handHoldingColonyTool(player);

        if (hand == null)
        {
            return;
        }

        double dy = event.getScrollDeltaY();

        if (dy == 0.0D)
        {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        ColonyToolMode current = ColonyToolItem.getMode(stack);
        ColonyToolMode next = dy > 0.0D ? current.next() : current.previous();

        PacketDistributor.sendToServer(new CycleColonyToolModePayload(hand, next));
        event.setCanceled(true);
    }

    private static @Nullable InteractionHand handHoldingColonyTool(LocalPlayer player)
    {
        if (player.getMainHandItem().is(ColonyItems.COLONY_TOOL.get()))
        {
            return InteractionHand.MAIN_HAND;
        }

        if (player.getOffhandItem().is(ColonyItems.COLONY_TOOL.get()))
        {
            return InteractionHand.OFF_HAND;
        }

        return null;
    }
}
