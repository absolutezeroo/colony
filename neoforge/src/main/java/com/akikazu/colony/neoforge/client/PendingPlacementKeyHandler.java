package com.akikazu.colony.neoforge.client;

import com.akikazu.colony.neoforge.network.CancelPendingPlacementPayload;
import com.akikazu.colony.neoforge.network.ConfirmZonePaintingPayload;
import com.akikazu.colony.neoforge.network.PendingPlacementClientState;
import com.akikazu.colony.neoforge.network.ZonePaintingClientState;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Routes player input into the pending-placement workflow on the client.
 *
 * <p>
 * While a pending placement is active:
 * <ul>
 * <li>Right-click on a block alternates between setting corner A and corner B (see
 * {@link ZonePaintingClientState#recordRightClick}). The AABB between the two latest positions is the painted zone; the
 * player keeps right-clicking to refine either corner. The click is swallowed regardless of which item is held —
 * vanilla use-on does not fire, so the player can keep holding the Hut item and still paint.</li>
 * <li>Left-click is swallowed so vanilla never breaks blocks during painting.</li>
 * <li>Enter sends {@link ConfirmZonePaintingPayload} once both corners are set.</li>
 * <li>Esc sends {@link CancelPendingPlacementPayload}, including pre-empting the pause screen via
 * {@link ScreenEvent.Opening}.</li>
 * </ul>
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
            if (!ZonePaintingClientState.hasBothCorners())
            {
                return;
            }

            BlockPos cornerA = ZonePaintingClientState.cornerA().orElseThrow();
            BlockPos cornerB = ZonePaintingClientState.cornerB().orElseThrow();

            PacketDistributor.sendToServer(new ConfirmZonePaintingPayload(cornerA, cornerB));

            return;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE)
        {
            PendingPlacementClientState.clear();
            ZonePaintingClientState.clear();
            PacketDistributor.sendToServer(CancelPendingPlacementPayload.INSTANCE);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseButton(InputEvent.InteractionKeyMappingTriggered event)
    {
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

        if (event.isAttack())
        {
            event.setCanceled(true);
            event.setSwingHand(false);

            return;
        }

        if (!event.isUseItem())
        {
            return;
        }

        BlockPos clicked = lookedAtBlock(minecraft);

        if (clicked == null)
        {
            event.setCanceled(true);
            event.setSwingHand(false);

            return;
        }

        ZonePaintingClientState.recordRightClick(clicked);
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    private static @Nullable BlockPos lookedAtBlock(Minecraft minecraft)
    {
        HitResult hit = minecraft.hitResult;

        if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() == HitResult.Type.MISS)
        {
            return null;
        }

        return blockHit.getBlockPos();
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
        ZonePaintingClientState.clear();
        PacketDistributor.sendToServer(CancelPendingPlacementPayload.INSTANCE);
    }
}
