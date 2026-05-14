package com.akikazu.colony.neoforge.client;

import com.akikazu.colony.neoforge.network.CancelPendingPlacementPayload;
import com.akikazu.colony.neoforge.network.ConfirmPendingPlacementPayload;
import com.akikazu.colony.neoforge.network.PendingPlacementClientState;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/**
 * Translates Esc / Enter key presses into {@link CancelPendingPlacementPayload} /
 * {@link ConfirmPendingPlacementPayload} while a pending hut placement is active on the client.
 *
 * <p>
 * Esc would normally open the pause menu. To preempt that, we listen on {@link ScreenEvent.Opening} for the pause
 * screen and cancel the event if a pending placement is active — instead sending the cancel payload. Enter has no
 * default binding while no screen is open, so we listen on {@link InputEvent.Key} directly.
 */
@OnlyIn(Dist.CLIENT)
public final class PendingPlacementKeyHandler
{
    private PendingPlacementKeyHandler()
    {
    }

    public static void register()
    {
        NeoForge.EVENT_BUS.register(PendingPlacementKeyHandler.class);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event)
    {
        if (event.getAction() != InputConstants.PRESS)
        {
            return;
        }

        if (PendingPlacementClientState.current() == null)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || minecraft.screen != null)
        {
            return;
        }

        int key = event.getKey();

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)
        {
            PacketDistributor.sendToServer(ConfirmPendingPlacementPayload.INSTANCE);

            return;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE)
        {
            PendingPlacementClientState.clear();
            PacketDistributor.sendToServer(CancelPendingPlacementPayload.INSTANCE);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event)
    {
        if (PendingPlacementClientState.current() == null)
        {
            return;
        }

        if (!(event.getNewScreen() instanceof PauseScreen))
        {
            return;
        }

        if (Minecraft.getInstance().screen != null)
        {
            return;
        }

        event.setCanceled(true);
        PendingPlacementClientState.clear();
        PacketDistributor.sendToServer(CancelPendingPlacementPayload.INSTANCE);
    }
}
